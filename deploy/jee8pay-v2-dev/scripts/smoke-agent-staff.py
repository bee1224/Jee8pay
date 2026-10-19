# 代理後台三個流程（統計看板、我的子帳號、渠道測試）隔離測試：在測試伺服器的 jee8pay-smoke（全新資料庫、manager 發布於 127.0.0.1:29217）上實際呼叫 API。
# 用法：ssh nnviopp-sandbox python3 - < smoke-agent-staff.py；會讀取 ~/stage-20261013/ 的搬遷腳本（含 20261014）。不可對 jee8pay-v2-dev 執行。
# 2026-10-14 追加：團長手機號選填、中文登入帳號、代理不能改自己的個人資料、品牌設定停用、刪商戶清綁定、刪團長連帶刪登入帳號。
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


def try_login(user, pwd):
    v = call('GET', '/api/anon/auth/vercode')['data']
    code = sh('sudo -n docker exec jee8pay-smoke-redis-1 redis-cli -n 1 get img_code_%s' % v['vercodeToken'])
    r = call('POST', '/api/anon/auth/validate', body={'ia': b64(user), 'ip': b64(pwd), 'vc': b64(code), 'vt': b64(v['vercodeToken'])})
    return r['data']['iToken'] if r.get('code') == 0 else None
P = '/api/agentPortal'
god = login('jeepay', 'jeepay123')

r = call('POST', '/api/agentInfo', god, body={'agentName': '甲團長', 'agentLevel': 1, 'contactName': '甲', 'contactTel': '0911000001', 'loginUsername': 'leaderA'})
check('新增甲團長', r.get('code') == 0, r); A = r['data']['agentNo']; a_tok = login('leaderA', r['data']['initPassword']); a_uid = r['data']['sysUserId']
r = call('POST', '/api/agentInfo', god, body={'agentName': '乙團長', 'agentLevel': 1, 'contactName': '乙', 'contactTel': '0911000002', 'loginUsername': 'leaderB'})
check('新增乙團長', r.get('code') == 0, r); b_tok = login('leaderB', r['data']['initPassword'])

menu = json.dumps(call('GET', '/api/current/user', a_tok), ensure_ascii=False)
check('團長選單有統計看板、我的子帳號、渠道測試', all(k in menu for k in ('ENT_AGENT_PORTAL_DASH', 'ENT_AGENT_PORTAL_STAFF', 'ENT_AGENT_PORTAL_CHANNEL_TEST')), menu[:300])

# ---- 統計看板 ----
d = call('GET', P + '/branch/dashboard', a_tok)
check('看板回傳各項欄位且為 0', d.get('code') == 0 and all(d['data'].get(k) == 0 for k in ('todayAmount', 'yesterdayAmount', 'monthAmount', 'lastMonthAmount', 'todayPaid', 'todayFailed', 'mchCount')), d)
check('上帝不能用代理看板', call('GET', P + '/branch/dashboard', god).get('code') != 0)

# ---- 商戶與看板數字 ----
r = call('POST', P + '/merchants', a_tok, body={'mchName': '甲商戶', 'mchShortName': '甲商', 'contactName': '陳', 'contactTel': '0922000001', 'loginUsername': 'mcha01'})
check('甲團長新增商戶', r.get('code') == 0, r); M = r['data'].get('mchNo') or sql("select mch_no from t_mch_info where mch_name='甲商戶'")
sql("""insert into t_pay_order (pay_order_id, mch_no, app_id, mch_name, mch_type, mch_order_no, if_code, way_code, amount, mch_fee_rate, mch_fee_amount, currency, state, client_ip, subject, body, created_at, updated_at)
 values ('P_T1','%(m)s','app1','甲商戶',1,'o1','ryo','RYO_IBON',10000,0.01,100,'TWD',2,'1.1.1.1','s','b',now(),now()),
        ('P_T2','%(m)s','app1','甲商戶',1,'o2','ryo','RYO_IBON',5000,0.01,50,'TWD',6,'1.1.1.1','s','b',now(),now()),
        ('P_Y1','%(m)s','app1','甲商戶',1,'o3','ryo','RYO_IBON',7000,0.01,70,'TWD',2,'1.1.1.1','s','b',date_sub(curdate(), interval 12 hour),now());""" % {'m': M})
cnt = sql("select count(*) from t_pay_order where mch_no='%s'" % M)
d = call('GET', P + '/branch/dashboard', a_tok)['data']
check('測試訂單已寫入', cnt == '3', cnt)
check('看板：今日成交 100 元、成功 1 失敗 1、昨日 70 元、今日手續費 1 元', d.get('todayAmount') == 10000 and d.get('todayPaid') == 1 and d.get('todayFailed') == 1 and d.get('yesterdayAmount') == 7000 and d.get('todayFee') == 100 and d.get('mchCount') == 1, d)
d = call('GET', P + '/branch/dashboard', b_tok)['data']
check('乙團長看板看不到甲的訂單', d.get('todayAmount') == 0 and d.get('mchCount') == 0, d)

# ---- 我的子帳號 ----
r = call('POST', P + '/staff', a_tok, body={'loginUsername': 'staffa1', 'realname': '小明', 'telphone': '0933000001'})
check('建立子帳號回傳一次性密碼', r.get('code') == 0 and len(r['data'].get('initPassword', '')) == 8, r); S = r['data']['sysUserId']; s_pwd = r['data']['initPassword']
check('密碼不落在資料庫明文', sql("select count(*) from t_sys_user_auth where credential='%s'" % s_pwd) == '0')
check('帳號重複被拒', call('POST', P + '/staff', a_tok, body={'loginUsername': 'staffa1', 'realname': 'x', 'telphone': '0933000009'}).get('code') != 0)
check('手機格式錯被拒', call('POST', P + '/staff', a_tok, body={'loginUsername': 'staffa2', 'realname': 'x', 'telphone': '123'}).get('code') != 0)
lst = call('GET', P + '/staff', a_tok)['data']
check('列表一筆、尚未設定權限、不含主帳號', len(lst) == 1 and lst[0]['loginUsername'] == 'staffa1' and lst[0]['entCount'] == 0, lst)
check('乙團長列表是空的', call('GET', P + '/staff', b_tok)['data'] == [])
# 手機號非必填：可連續建立多個不填手機的子帳號，列表顯示空白、不留手機號登入方式
n1 = call('POST', P + '/staff', a_tok, body={'loginUsername': 'staffn1', 'realname': '沒手機一'})
n2 = call('POST', P + '/staff', a_tok, body={'loginUsername': 'staffn2', 'realname': '沒手機二', 'telphone': ''})
check('不填手機號可建立（連續兩個）', n1.get('code') == 0 and n2.get('code') == 0, [n1, n2])
nl = [x for x in call('GET', P + '/staff', a_tok)['data'] if x['loginUsername'] in ('staffn1', 'staffn2')]
check('不填手機號的子帳號列表顯示空白', len(nl) == 2 and all(x['telphone'] == '' for x in nl), nl)
check('不填手機號就沒有手機號登入方式', sql("select count(*) from t_sys_user_auth where user_id in (%s,%s) and identity_type=2" % (n1['data']['sysUserId'], n2['data']['sysUserId'])) == '0')
call('PUT', P + '/staff/%s/ents' % n1['data']['sysUserId'], a_tok, body={'entIds': ['ENT_AGENT_PORTAL_MCH']})
check('不填手機號的子帳號可用帳號密碼登入', try_login('staffn1', n1['data']['initPassword']) is not None)

tok0 = try_login('staffa1', s_pwd)
if tok0:
    check('未設權限的子帳號讀不到代理資料', call('GET', P + '/me', tok0).get('code') != 0)
else:
    check('未設權限的子帳號無法登入', True)

g = call('GET', P + '/staff/grantable', a_tok)['data']; gids = [e['entId'] for e in g]
check('可下放清單不含子帳號管理、含商戶列表', 'ENT_AGENT_PORTAL_STAFF' not in gids and 'ENT_AGENT_PORTAL_MCH' in gids, gids)
check('不可授予子帳號管理', call('PUT', P + '/staff/%s/ents' % S, a_tok, body={'entIds': ['ENT_AGENT_PORTAL_STAFF']}).get('code') != 0)
check('不可授予平台權限', call('PUT', P + '/staff/%s/ents' % S, a_tok, body={'entIds': ['ENT_MCH_INFO']}).get('code') != 0)
r = call('PUT', P + '/staff/%s/ents' % S, a_tok, body={'entIds': ['ENT_AGENT_PORTAL_MCH', 'ENT_AGENT_PORTAL_DASH']})
check('設定權限並自動補基礎權限', r.get('code') == 0 and set(r['data']) == {'ENT_AGENT_PORTAL_MCH', 'ENT_AGENT_PORTAL_DASH', 'ENT_COMMONS', 'ENT_C_USERINFO', 'ENT_AGENT_PORTAL', 'ENT_AGENT_PORTAL_VIEW'}, r)
check('乙團長不能讀甲的子帳號權限', call('GET', P + '/staff/%s/ents' % S, b_tok).get('code') != 0)
check('乙團長不能改甲的子帳號權限', call('PUT', P + '/staff/%s/ents' % S, b_tok, body={'entIds': []}).get('code') != 0)
check('乙團長不能停用甲的子帳號', call('PUT', P + '/staff/%s/state' % S, b_tok, body={'state': 0}).get('code') != 0)
check('乙團長不能重設甲的子帳號密碼', call('POST', P + '/staff/%s/resetPassword' % S, b_tok, body={}).get('code') != 0)
check('主帳號不能被當成子帳號改權限', call('PUT', P + '/staff/%s/ents' % a_uid, a_tok, body={'entIds': []}).get('code') != 0)
check('主帳號不能被當成子帳號重設密碼', call('POST', P + '/staff/%s/resetPassword' % a_uid, a_tok, body={}).get('code') != 0)

s_tok = login('staffa1', s_pwd)
smenu = json.dumps(call('GET', '/api/current/user', s_tok), ensure_ascii=False)
check('子帳號選單只有被授予的項目', 'ENT_AGENT_PORTAL_MCH' in smenu and 'ENT_AGENT_PORTAL_DASH' in smenu and 'ENT_AGENT_PORTAL_STAFF' not in smenu and 'ENT_AGENT_PORTAL_FEE' not in smenu and 'ENT_MCH_INFO' not in smenu, smenu[:400])
check('子帳號讀得到代理資料與看板', call('GET', P + '/me', s_tok).get('code') == 0 and call('GET', P + '/branch/dashboard', s_tok).get('code') == 0)
check('子帳號看得到轄區商戶', len(call('GET', P + '/merchants', s_tok).get('data') or []) == 1)
check('子帳號不能管理子帳號', call('GET', P + '/staff', s_tok).get('code') != 0 and call('POST', P + '/staff', s_tok, body={'loginUsername': 'staffx', 'realname': 'x', 'telphone': '0933000008'}).get('code') != 0)
check('子帳號不能替自己加權限', call('PUT', P + '/staff/%s/ents' % S, s_tok, body={'entIds': ['ENT_AGENT_PORTAL_FEE']}).get('code') != 0)
check('子帳號沒有錢包選單時不能申請提現', call('POST', P + '/wallet/withdraws', s_tok, body={'amount': '1', 'reqNo': 'R1'}).get('code') != 0)
check('子帳號沒有錢包選單時不能改收款帳戶', call('PUT', P + '/wallet/payoutAccount', s_tok, body={'payoutBankName': 'x'}).get('code') != 0)
check('子帳號沒有新增商戶權限', call('POST', P + '/merchants', s_tok, body={'mchName': 'z', 'mchShortName': 'z', 'contactName': 'z', 'contactTel': '0922000009', 'loginUsername': 'mchz01'}).get('code') != 0)
check('子帳號沒有費率權限', call('POST', P + '/branch/feeRules', s_tok, body={}).get('code') != 0)
check('子帳號不能呼叫平台介面', call('GET', '/api/mchInfo', s_tok).get('code') != 0)
check('主帳號仍可申請提現以外的錢包讀取', call('GET', P + '/wallet', a_tok).get('code') == 0)

# 權限即時生效：加上渠道測試後不必重新登入
call('PUT', P + '/staff/%s/ents' % S, a_tok, body={'entIds': ['ENT_AGENT_PORTAL_MCH', 'ENT_AGENT_PORTAL_CHANNEL_TEST']})
check('權限改完立即生效（新增的可用）', call('GET', P + '/branch/merchants/%s/testWays' % M, s_tok).get('code') == 0)
check('權限改完立即生效（移除的不可用）', call('GET', P + '/branch/dashboard', s_tok).get('code') != 0)

check('停用子帳號', call('PUT', P + '/staff/%s/state' % S, a_tok, body={'state': 0}).get('code') == 0)
check('停用後原憑證失效', call('GET', P + '/me', s_tok).get('code') != 0)
check('停用後無法登入', try_login('staffa1', s_pwd) is None)
call('PUT', P + '/staff/%s/state' % S, a_tok, body={'state': 1})
check('啟用後可登入', try_login('staffa1', s_pwd) is not None)
r = call('POST', P + '/staff/%s/resetPassword' % S, a_tok, body={})
check('重設密碼回傳新密碼', r.get('code') == 0 and r['data']['initPassword'] != s_pwd, r)
check('舊密碼失效、新密碼可用', try_login('staffa1', s_pwd) is None and try_login('staffa1', r['data']['initPassword']) is not None)

# ---- 隊長 ----
r = call('POST', P + '/subAgents', a_tok, body={'agentName': '甲隊長', 'contactName': '丙', 'contactTel': '0911000003', 'loginUsername': 'captainA'})
check('新增隊長', r.get('code') == 0, r); c_tok = login('captainA', r['data']['initPassword'])
check('隊長有看板', call('GET', P + '/branch/dashboard', c_tok).get('code') == 0)
check('隊長不能管理子帳號', call('GET', P + '/staff', c_tok).get('code') != 0)

# ---- 渠道測試 ----
check('甲團長查商戶支付方式（尚未開通為空）', call('GET', P + '/branch/merchants/%s/testWays' % M, a_tok).get('data') == [])
check('乙團長不能測甲的商戶', call('GET', P + '/branch/merchants/%s/testWays' % M, b_tok).get('code') != 0)
body = {'appId': 'nope', 'wayCode': 'RYO_IBON', 'amount': '100', 'payerName': '測', 'payerMobile': '0912345678', 'payerEmail': 'a@example.com', 'payerPostcode': '100', 'payerAddress': '台北'}
check('乙團長不能替甲的商戶下測試單', call('POST', P + '/branch/merchants/%s/channelTest' % M, b_tok, body=body).get('code') != 0)
r = call('POST', P + '/branch/merchants/%s/channelTest' % M, a_tok, body=body)
check('應用不存在被拒', r.get('code') != 0 and '應用' in str(r.get('msg')), r)
app = sql("select app_id from t_mch_app where mch_no='%s' limit 1" % M)
if not app:
    sql("insert into t_mch_app (app_id, app_name, mch_no, state, created_at, updated_at) values ('app_smoke_1','預設應用','%s',1,now(),now())" % M); app = 'app_smoke_1'
body['appId'] = app
r = call('POST', P + '/branch/merchants/%s/channelTest' % M, a_tok, body=body)
check('未開通支付方式被拒', r.get('code') != 0 and '開通' in str(r.get('msg')), r)
sql("insert into t_mch_pay_passage (mch_no, app_id, if_code, way_code, rate, state, created_at, updated_at) values ('%s','%s','ryo','RYO_IBON',0.01,1,now(),now())" % (M, app))
w = call('GET', P + '/branch/merchants/%s/testWays' % M, a_tok).get('data')
check('開通後列出支付方式', w and w[0]['wayCode'] == 'RYO_IBON' and w[0]['appId'] == app, w)
b2 = dict(body); b2['amount'] = '999999'
check('金額超過上限被拒', '不超過' in str(call('POST', P + '/branch/merchants/%s/channelTest' % M, a_tok, body=b2).get('msg')))
b3 = dict(body); b3.pop('payerEmail')
check('繳款人資料不完整被拒', '繳款人' in str(call('POST', P + '/branch/merchants/%s/channelTest' % M, a_tok, body=b3).get('msg')))
before = sql("select count(*) from t_pay_order where mch_no='%s'" % M)
r = call('POST', P + '/branch/merchants/%s/channelTest' % M, a_tok, body=body)
check('驗證通過後確實送往付款服務（隔離環境沒有付款服務，預期連線失敗）', r.get('code') != 0, r)
print('INFO 送單回應：', str(r)[:200])
check('送單失敗不留下訂單', sql("select count(*) from t_pay_order where mch_no='%s'" % M) == before)

# ---- 搬遷腳本可重複執行 ----
mig = open('/home/incident/stage-20261013/20261013-agent-dashboard-staff-channel-test.sql').read()
n1 = sql("select count(*) from t_sys_role_ent_rela where role_id like 'ROLE_AGENT_PORTAL%' and ent_id in ('ENT_AGENT_PORTAL_DASH','ENT_AGENT_PORTAL_STAFF','ENT_AGENT_PORTAL_CHANNEL_TEST')")
sql(mig); sql(mig)
n2 = sql("select count(*) from t_sys_role_ent_rela where role_id like 'ROLE_AGENT_PORTAL%' and ent_id in ('ENT_AGENT_PORTAL_DASH','ENT_AGENT_PORTAL_STAFF','ENT_AGENT_PORTAL_CHANNEL_TEST')")
check('搬遷腳本與種子檔結果一致且可重複執行', n1 == '5' and n2 == '5', (n1, n2))

# ---- 2026-10-14：團長手機號選填、中文登入帳號 ----
r = call('POST', '/api/agentInfo', god, body={'agentName': '丁團長', 'agentLevel': 1, 'loginUsername': '丁團長'})
check('不填手機號、用中文帳號新增團長', r.get('code') == 0, r); D = r['data']['agentNo']; d_pwd = r['data']['initPassword']
r2 = call('POST', '/api/agentInfo', god, body={'agentName': '戊團長', 'agentLevel': 1, 'loginUsername': 'leaderE', 'contactTel': ''})
check('再新增一個不填手機號的團長（佔位值不重複）', r2.get('code') == 0, r2); E = r2['data']['agentNo']
check('中文帳號可以登入', try_login('丁團長', d_pwd) is not None)
d_tok = login('丁團長', d_pwd)
acc = call('GET', '/api/agentInfo/%s/accounts' % D, god).get('data') or []
check('登入帳號列表的手機號顯示空白', len(acc) == 1 and acc[0]['telphone'] == '', acc)
check('不填手機號的團長沒有手機號登入方式', sql("select count(*) from t_sys_user_auth where user_id=%s and identity_type=2" % acc[0]['sysUserId']) == '0')
check('手機號格式錯仍被拒', call('POST', '/api/agentInfo', god, body={'agentName': 'x', 'agentLevel': 1, 'loginUsername': 'leaderX', 'contactTel': '123'}).get('code') != 0)
check('帳號含空白或符號被拒', call('POST', '/api/agentInfo', god, body={'agentName': 'x', 'agentLevel': 1, 'loginUsername': '丁 團'}).get('code') != 0)
r = call('POST', P + '/subAgents', d_tok, body={'agentName': '丁隊長', 'loginUsername': '丁隊長'})
check('團長新增隊長可不填手機號、可用中文帳號', r.get('code') == 0, r); DC = r['data']['agentNo']

# ---- 品牌設定停用 ----
dmenu = json.dumps(call('GET', '/api/current/user', d_tok), ensure_ascii=False)
check('團長選單沒有品牌設定', 'ENT_AGENT_PORTAL_BRAND' not in dmenu and 'ENT_AGENT_PORTAL_DASH' in dmenu, dmenu[:200])
check('團長不能設定品牌', call('PUT', P + '/branch/brand', d_tok, body={'brandEnabled': 1, 'brandTitle': 'x'}).get('code') != 0)

# ---- 代理不能改自己的個人資料 ----
check('團長不能改自己的名稱', call('PUT', '/api/current/user', d_tok, body={'realname': '改名'}).get('code') != 0)
check('名稱沒有被改', sql("select realname from t_sys_user where sys_user_id=%s" % acc[0]['sysUserId']) != '改名')
check('上帝仍可改自己的個人資料', call('PUT', '/api/current/user', god, body={'realname': '上帝'}).get('code') == 0)

# ---- 商戶平台登入網址 ----
r = call('POST', P + '/merchants', d_tok, body={'mchName': '丁商戶', 'mchShortName': '丁商', 'contactName': '林', 'contactTel': '0922000099', 'loginUsername': 'mchd01'})
check('新增商戶回傳登入網址欄位（商戶平台網址仍是 127.0.0.1 時為空）', r.get('code') == 0 and r['data'].get('loginUrl') == '', r); DM = r['data']['mchNo']

# ---- 刪商戶清綁定、刪團長連帶刪登入帳號 ----
check('上帝刪除商戶', call('DELETE', '/api/mchInfo/%s' % DM, god).get('code') == 0)
check('刪商戶後團長與商戶的綁定一併刪除', sql("select count(*) from t_agent_mch_rela where mch_no='%s'" % DM) == '0')
check('團長下仍有隊長時不可刪除', '隊長' in str(call('DELETE', '/api/agentInfo/%s' % D, god).get('msg')))
check('刪除隊長（連帶刪除隊長的登入帳號）', call('DELETE', '/api/agentInfo/%s' % DC, god).get('code') == 0 and sql("select count(*) from t_sys_user where belong_info_id='%s'" % DC) == '0')
sql("insert into t_agent_mch_rela (mch_no, agent_no, created_at, updated_at) values ('M_GHOST','%s',now(),now())" % D)
r = call('POST', '/api/agentInfo/%s/accounts' % D, god, body={'loginUsername': 'dstaff2', 'realname': '丁二號'})
check('上帝替團長再開一個登入帳號（不填手機號）', r.get('code') == 0, r)
r = call('POST', P + '/staff', d_tok, body={'loginUsername': '丁的員工', 'realname': '員工'})
check('團長建立子帳號（中文帳號）', r.get('code') == 0, r); dsid = r['data']['sysUserId']
r = call('DELETE', '/api/agentInfo/%s' % D, god)
check('團長名下只剩已刪除商戶的綁定時可以刪除', r.get('code') == 0, r)
check('團長的三個登入帳號都已刪除', sql("select count(*) from t_sys_user where belong_info_id='%s'" % D) == '0')
check('登入方式與角色關聯一併刪除', sql("select count(*) from t_sys_user_auth where user_id=%s" % dsid) == '0' and sql("select count(*) from t_sys_user_role_rela where user_id=%s" % dsid) == '0')
check('子帳號專屬角色一併刪除', sql("select count(*) from t_sys_role where role_id='ROLE_AGSUB_%s'" % dsid) == '0')
check('被刪團長的舊登入狀態立即失效', call('GET', '/api/current/user', d_tok).get('code') != 0)
check('被刪團長無法再登入', try_login('丁團長', d_pwd) is None)
r = call('DELETE', '/api/agentInfo/%s' % A, god)
check('有隊長與真實商戶的團長仍不可刪除，帳號保留', r.get('code') != 0 and sql("select count(*) from t_sys_user where belong_info_id='%s'" % A) != '0', r)

# ---- 20261014 搬遷腳本可重複執行 ----
sql("update t_sys_entitlement set state=1 where ent_id='ENT_AGENT_PORTAL_BRAND'; insert ignore into t_sys_role_ent_rela values ('ROLE_AGENT_PORTAL','ENT_AGENT_PORTAL_BRAND'); insert into t_agent_mch_rela (mch_no, agent_no, created_at, updated_at) values ('M_GHOST2','%s',now(),now());" % E)
mig2 = open('/home/incident/stage-20261013/20261014-agent-brand-off.sql').read()
sql(mig2); sql(mig2)
check('搬遷後品牌設定停用且不授予任何角色', sql("select state from t_sys_entitlement where ent_id='ENT_AGENT_PORTAL_BRAND'") == '0' and sql("select count(*) from t_sys_role_ent_rela where ent_id='ENT_AGENT_PORTAL_BRAND'") == '0')
check('搬遷後指向已刪除商戶的綁定清除', sql("select count(*) from t_agent_mch_rela r left join t_mch_info m on m.mch_no=r.mch_no where m.mch_no is null") == '0')

print('\nRESULT ok=%d fail=%d' % (ok, fail))
sys.exit(1 if fail else 0)
