# ADR-0012 第一階段隔離測試：在測試伺服器的 jee8pay-smoke（全新資料庫、manager 發布於 127.0.0.1:29217）上實際呼叫 API。
# 用法：ssh nnviopp-sandbox python3 - < smoke-channel-accounts.py；會讀取 ~/stage-rename/20261009-channel-accounts.sql 驗證搬遷腳本。不可對 jee8pay-v2-dev 執行。
import base64, json, subprocess, sys, urllib.request, urllib.error, urllib.parse
BASE = 'http://127.0.0.1:29217'
ok = fail = 0
def b64(s): return base64.b64encode(s.encode()).decode()
def call(method, path, token=None, body=None, params=None):
    url = BASE + path + ('?' + urllib.parse.urlencode(params) if params else '')
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers={'Content-Type': 'application/json'})
    if token: req.add_header('iToken', token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r: return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try: return json.loads(raw)
        except Exception: return {'code': e.code, 'msg': raw[:200]}
def sh(cmd): return subprocess.run(cmd, shell=True, capture_output=True, text=True).stdout.strip()
def sql(q):
    return subprocess.run("sudo -n docker exec -i jee8pay-smoke-db-1 sh -c 'mariadb -uroot -p\"$(cat /run/secrets/db-root-password)\" --default-character-set=utf8mb4 jee8pay_v2_dev -N -B'", shell=True, input=q, capture_output=True, text=True).stdout.strip()
def login(user, pwd):
    v = call('GET', '/api/anon/auth/vercode')['data']
    code = sh('sudo -n docker exec jee8pay-smoke-redis-1 redis-cli -n 1 get img_code_%s' % v['vercodeToken'])
    r = call('POST', '/api/anon/auth/validate', body={'ia': b64(user), 'ip': b64(pwd), 'vc': b64(code), 'vt': b64(v['vercodeToken'])})
    if r.get('code') != 0: print('LOGIN FAIL', user, r); sys.exit(1)
    return r['data']['iToken']
def check(name, cond, detail=''):
    global ok, fail
    if cond: ok += 1; print('PASS', name)
    else: fail += 1; print('FAIL', name, '|', str(detail)[:400])

god = login('jeepay', 'jeepay123')
me = call('GET', '/api/current/user', god)
tree = json.dumps(me, ensure_ascii=False)
check('上帝選單有團長詳情路由', 'ENT_AGENT_DETAIL' in tree, tree[:200])
check('上帝選單沒有服務商', '"ENT_ISV"' not in tree)
check('上帝有渠道帳號權限', 'ENT_CHANNEL_ACCOUNT_EDIT' in tree)

agents = call('GET', '/api/agentInfo', god, params={'pageSize': -1})
house = [a for a in agents['data']['records'] if a['agentNo'] == 'A_HOUSE']
check('平台直屬存在且 isHouse=1', house and house[0].get('isHouse') == 1, agents)

r = call('POST', '/api/agentInfo', god, body={'agentName': '測試團長', 'agentLevel': 1, 'contactName': '王', 'contactTel': '0912345678', 'loginUsername': 'leader01'})
check('新增團長', r.get('code') == 0, r)
T1 = r['data']['agentNo']; t1_pwd = r['data']['initPassword']

SECRET = 'S3cretPwd-XYZ'
r = call('POST', '/api/channelAccounts', god, body={'accountName': 'RYO 主帳號', 'ifCode': 'ryo', 'ownerSrAgentNo': T1, 'ifParams': json.dumps({'environment': 'TEST', 'custId': 'C1', 'apiPassword': SECRET})})
check('新增渠道帳號', r.get('code') == 0, r)
ACC = r['data']['accountId']
lst = call('GET', '/api/channelAccounts', god, params={'srAgentNo': T1})
check('列表只有一筆且不含金鑰', len(lst['data']) == 1 and SECRET not in json.dumps(lst), lst)
d = call('GET', '/api/channelAccounts/' + ACC, god)
masked = json.loads(d['data']['ifParams'])
check('詳情金鑰已遮罩', SECRET not in json.dumps(d) and masked.get('custId') == 'C1' and masked.get('apiPassword'), d)
check('金鑰存在 info_type=4', sql("select count(*) from t_pay_interface_config where info_type=4 and info_id='%s' and if_params like concat(char(37),'%s',char(37))" % (ACC, SECRET)) == '1')

r = call('PUT', '/api/channelAccounts/' + ACC, god, body={'accountName': 'RYO 改名', 'ifParams': json.dumps({'custId': 'C2'})})
check('修改名稱與部分金鑰', r.get('code') == 0, r)
row = sql("select if_params from t_pay_interface_config where info_type=4 and info_id='%s'" % ACC)
check('未填的遮罩欄位保留原值', SECRET in row and 'C2' in row, row)

r = call('POST', '/api/channelAccounts/%s/agents' % ACC, god, body={'srAgentNo': 'A_HOUSE'})
check('未開共用不可加派', r.get('code') != 0, r)
call('PUT', '/api/channelAccounts/' + ACC, god, body={'shareable': 1})
r = call('POST', '/api/channelAccounts/%s/agents' % ACC, god, body={'srAgentNo': 'A_HOUSE'})
check('開共用後可加派', r.get('code') == 0, r)
lst = call('GET', '/api/channelAccounts', god, params={'srAgentNo': 'A_HOUSE'})
check('被加派的團長看得到', len(lst['data']) == 1, lst)
r = call('PUT', '/api/channelAccounts/' + ACC, god, body={'shareable': 0})
check('已加派時不可關閉共用', r.get('code') != 0, r)
r = call('DELETE', '/api/channelAccounts/%s/agents/%s' % (ACC, T1), god)
check('不可收回擁有者', r.get('code') != 0, r)
r = call('DELETE', '/api/channelAccounts/' + ACC, god)
check('已加派時不可刪除', r.get('code') != 0, r)
r = call('DELETE', '/api/channelAccounts/%s/agents/A_HOUSE' % ACC, god)
check('收回加派', r.get('code') == 0, r)

r = call('POST', '/api/channelAccounts', god, body={'accountName': 'x', 'ifCode': 'nope', 'ownerSrAgentNo': T1, 'ifParams': '{}'})
check('不存在的接口被拒', r.get('code') != 0, r)
r = call('POST', '/api/channelAccounts', god, body={'accountName': 'x', 'ifCode': 'ryo', 'ownerSrAgentNo': T1, 'ifParams': ''})
check('沒有金鑰被拒', r.get('code') != 0, r)

r = call('POST', '/api/agentInfo/%s/merchants' % T1, god, body={'mchName': '測試商戶', 'mchShortName': '測試', 'contactName': '李', 'contactTel': '0922333444', 'loginUsername': 'mch_test01', 'targetAgentNo': T1})
check('在團長底下新增商戶', r.get('code') == 0 and r['data'].get('initPassword'), r)
ms = call('GET', '/api/agentInfo/%s/merchants' % T1, god)
check('團長的商戶列表有一筆', len(ms['data']) == 1, ms)
MCH = ms['data'][0]['mchNo']

# 團長本人登入
t1 = login('leader01', t1_pwd)
tree1 = json.dumps(call('GET', '/api/current/user', t1), ensure_ascii=False)
check('團長選單有渠道列表', 'ENT_AGENT_PORTAL_CHANNEL' in tree1)
ch = call('GET', '/api/agentPortal/channels', t1)
check('團長看得到自己的渠道且不含金鑰', ch.get('code') == 0 and len(ch['data']) == 1 and SECRET not in json.dumps(ch) and 'ifParams' not in json.dumps(ch), ch)
for m, p, b in [('GET', '/api/channelAccounts', None), ('GET', '/api/channelAccounts/' + ACC, None), ('POST', '/api/channelAccounts', {'accountName': 'h', 'ifCode': 'ryo', 'ownerSrAgentNo': T1, 'ifParams': '{}'}), ('PUT', '/api/channelAccounts/' + ACC, {'shareable': 1}), ('DELETE', '/api/channelAccounts/' + ACC, None), ('POST', '/api/agentInfo/%s/merchants' % T1, {}), ('GET', '/api/agentInfo/%s/merchants' % T1, None)]:
    r = call(m, p, t1, body=b)
    check('團長不可呼叫上帝端點 %s %s' % (m, p), r.get('code') not in (0, None) and r.get('code') != 0, r)

r = call('POST', '/api/agentPortal/subAgents', t1, body={'agentName': '測試隊長', 'contactTel': '0933444555', 'loginUsername': 'captain01'})
check('團長新增隊長', r.get('code') == 0, r)
c1 = login('captain01', r['data']['initPassword'])
r = call('GET', '/api/agentPortal/channels', c1)
check('隊長看不到渠道列表', r.get('code') != 0, r)

# 使用範圍：渠道仍屬於團長，但可限定只給某幾位隊長的商戶
CAP = [a for a in call('GET', '/api/agentInfo', god, params={'parentAgentNo': T1, 'pageSize': -1})['data']['records']][0]['agentNo']
r = call('PUT', '/api/channelAccounts/%s/scope' % ACC, god, body={'srAgentNo': T1, 'agentNos': [CAP]})
check('限定給隊長', r.get('code') == 0, r)
lst = call('GET', '/api/channelAccounts', god, params={'srAgentNo': T1})
check('列表顯示使用範圍', [x['agentNo'] for x in lst['data'][0].get('scopes', [])] == [CAP], lst)
ch = call('GET', '/api/agentPortal/channels', t1)
check('團長看得到使用範圍', [x['agentNo'] for x in ch['data'][0].get('scopes', [])] == [CAP], ch)
r = call('PUT', '/api/channelAccounts/%s/scope' % ACC, god, body={'srAgentNo': T1, 'agentNos': [T1]})
check('範圍不可指定團長本人', r.get('code') != 0, r)
r = call('PUT', '/api/channelAccounts/%s/scope' % ACC, god, body={'srAgentNo': 'A_HOUSE', 'agentNos': []})
check('未派發的團長不可設範圍', r.get('code') != 0, r)
r = call('PUT', '/api/channelAccounts/%s/scope' % ACC, t1, body={'srAgentNo': T1, 'agentNos': []})
check('團長不可自行改範圍', r.get('code') != 0, r)
r = call('DELETE', '/api/agentInfo/' + CAP, god)
check('被指定的隊長不可刪除', r.get('code') != 0 and '使用範圍' in str(r.get('msg')), r)
r = call('PUT', '/api/channelAccounts/%s/scope' % ACC, god, body={'srAgentNo': T1, 'agentNos': []})
check('清空範圍 = 全部可用', r.get('code') == 0 and sql("select count(*) from t_channel_account_scope") == '0', r)

r = call('DELETE', '/api/agentInfo/A_HOUSE', god)
check('平台直屬不可刪', r.get('code') != 0, r)

# 搬遷腳本：先替商戶應用放一份金鑰，清空渠道帳號，再跑腳本
apps = call('GET', '/api/mchApps', god, params={'mchNo': MCH})
APP = apps['data']['records'][0]['appId']
r = call('POST', '/api/mch/payConfigs', god, body={'infoId': APP, 'ifCode': 'ryo', 'state': 1, 'ifParams': json.dumps({'environment': 'TEST', 'custId': 'M1', 'apiPassword': 'AppKey-1'})})
check('商戶應用金鑰（舊方式）仍可設定', r.get('code') == 0, r)
r = call('DELETE', '/api/channelAccounts/' + ACC, god)
check('刪除渠道帳號', r.get('code') == 0 and sql("select count(*) from t_pay_interface_config where info_type=4") == '0', r)
sql("delete from t_agent_mch_rela where mch_no='%s'" % MCH)
out = subprocess.run("sudo -n docker exec -i jee8pay-smoke-db-1 sh -c 'mariadb -uroot -p\"$(cat /run/secrets/db-root-password)\" --default-character-set=utf8mb4 jee8pay_v2_dev' < ~/stage-rename/20261009-channel-accounts.sql", shell=True, capture_output=True, text=True)
check('搬遷腳本執行無錯誤', out.returncode == 0, out.stderr)
check('搬遷後產生平台直屬渠道帳號', sql("select concat(account_id,'|',owner_sr_agent_no) from t_channel_account") == 'CAHOUSERYO01|A_HOUSE', sql("select * from t_channel_account"))
check('搬遷後金鑰複製到 info_type=4', sql("select count(*) from t_pay_interface_config where info_type=4 and info_id='CAHOUSERYO01' and if_params like concat(char(37),'AppKey-1',char(37))") == '1')
check('搬遷後未歸屬商戶歸到平台直屬', sql("select agent_no from t_agent_mch_rela where mch_no='%s'" % MCH) == 'A_HOUSE')
out2 = subprocess.run("sudo -n docker exec -i jee8pay-smoke-db-1 sh -c 'mariadb -uroot -p\"$(cat /run/secrets/db-root-password)\" --default-character-set=utf8mb4 jee8pay_v2_dev' < ~/stage-rename/20261009-channel-accounts.sql", shell=True, capture_output=True, text=True)
check('搬遷腳本可重複執行', out2.returncode == 0 and sql("select count(*) from t_channel_account") == '1', out2.stderr)
print('RESULT pass=%d fail=%d' % (ok, fail))
