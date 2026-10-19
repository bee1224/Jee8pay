# 隔離測試：上帝主選單「API 測試」與「登入裝置」。
# 在測試伺服器的 jee8pay-smoke 上執行（先跑 smoke-ml-dsa.py 建好 M_SMOKE_MLDSA 與商戶金鑰）：
#   ssh nnviopp-sandbox python3 - < smoke-api-test.py
# payment 需以 compose.blackhole.yml 把上游網域指到 127.0.0.1，渠道請求一定連線失敗，不會打到第三方。不可對 jee8pay-v2-dev 執行。
import base64, json, os, subprocess, sys, time, urllib.request, urllib.error, urllib.parse
MGR = 'http://127.0.0.1:29217'
PAY = 'http://127.0.0.1:29216'
S = os.path.expanduser('~/smoke-mldsa')
MCH, APP = 'M_SMOKE_MLDSA', 'APP_SMOKE_MLDSA'
ok = fail = 0
def b64(s): return base64.b64encode(s.encode()).decode()
def call(method, url, token=None, body=None, params=None, headers=None, raw=False):
    url = url + ('?' + urllib.parse.urlencode(params) if params else '')
    req = urllib.request.Request(url, data=json.dumps(body).encode() if body is not None else None, method=method, headers={'Content-Type': 'application/json', **(headers or {})})
    if token: req.add_header('iToken', token)
    try:
        r = urllib.request.urlopen(req, timeout=90)
    except urllib.error.HTTPError as e:
        r = e
    data = r.read().decode()
    return (r.headers, data) if raw else json.loads(data)
def sh(cmd): return subprocess.run(cmd, shell=True, capture_output=True, text=True)
def login(user, pwd):
    v = call('GET', MGR + '/api/anon/auth/vercode')['data']
    code = sh('sudo -n docker exec jee8pay-smoke-redis-1 redis-cli -n 1 get img_code_%s' % v['vercodeToken']).stdout.strip()
    r = call('POST', MGR + '/api/anon/auth/validate', body={'ia': b64(user), 'ip': b64(pwd), 'vc': b64(code), 'vt': b64(v['vercodeToken'])})
    if r.get('code') != 0: print('LOGIN FAIL', user, r); sys.exit(1)
    return r['data']['iToken']
def check(name, cond, detail=''):
    global ok, fail
    if cond: ok += 1; print('OK  ', name)
    else: fail += 1; print('FAIL', name, str(detail)[:600])

god = login('jeepay', 'jeepay123')
# 登入裝置回報（模擬手機），之後總覽要看得到
call('POST', MGR + '/api/current/loginDevice', god, body={'deviceId': 'smoke-device-0001', 'deviceModel': 'Pixel 8', 'deviceOs': 'Android 15', 'geoState': 'GRANTED', 'latitude': 25.0478, 'longitude': 121.5170, 'accuracy': 30})

# 1. 選項：商戶、應用、已開通支付方式
r = call('GET', MGR + '/api/apiTest/options', god)
m = next((x for x in r.get('data') or [] if x['mchNo'] == MCH), None)
check('options 列出冒煙商戶、應用與 RYO_IBON', m and m['apps'] and m['apps'][0]['ways'] and m['apps'][0]['ways'][0]['wayCode'] == 'RYO_IBON', r)

# 2. 下單測試：完整請求、原始回應、渠道往來（上游被導到本機 → 連線失敗）
r = call('POST', MGR + '/api/apiTest/unifiedOrder', god, body={'mchNo': MCH, 'appId': APP, 'wayCode': 'RYO_IBON', 'amount': '100',
         'payerName': '測試人員', 'payerMobile': '0912345678', 'payerEmail': 'test@example.com', 'payerPostcode': '100', 'payerAddress': '台北市中正區測試路 1 號'})
d = r.get('data') or {}
req = json.loads(d.get('requestBody') or '{}')
check('下單：回傳完整請求（含 sign、PLATFORM-ML-DSA-65、reqTime）', req.get('signType') == 'PLATFORM-ML-DSA-65' and len(req.get('sign', '')) > 1000 and req.get('reqTime') and req.get('amount') == 10000, d)
check('下單：回傳待簽字串，且不含 sign', d.get('canonical') and 'sign=' not in d['canonical'].replace('signType=', ''), d.get('canonical'))
check('下單：回傳支付服務的原始回應與 HTTP 狀態', d.get('httpStatus') == 200 and d.get('responseBody', '').startswith('{'), d)
print('INFO 平台回應：', (d.get('responseBody') or '')[:300])
print('INFO 回應驗簽：', d.get('responseSignCheck'))
trace = d.get('channelTrace') or []
check('下單：帶回上游往來（Token 請求）', len(trace) >= 1 and trace[0]['provider'] == 'RYO' and trace[0]['url'].endswith('/Token'), trace)
if trace:
    t = trace[0]
    print('INFO 上游：', t['method'], t['url'], t.get('status'), t.get('error'))
    check('下單：Token 請求的密碼已遮蔽', 'password=****' in (t.get('body') or '') and 'apiPassword' not in json.dumps(trace), t.get('body'))
    check('下單：連線失敗時記錄錯誤原因', t.get('status') is None and t.get('error'), t)

# 3. 外部（非平台內部簽名）帶追蹤標頭：拿不到任何追蹤內容
h, body = call('POST', PAY + '/api/pay/unifiedOrder', body={'version': '1.0', 'signType': 'ML-DSA-65', 'reqTime': str(int(time.time() * 1000)), 'mchNo': MCH, 'appId': APP,
                'mchOrderNo': 'EXT1', 'wayCode': 'RYO_IBON', 'amount': 100, 'currency': 'TWD', 'subject': 'x', 'body': 'x', 'sign': 'AAAA'}, headers={'X-Jeepay-Trace': '1'}, raw=True)
check('外部請求帶 X-Jeepay-Trace 不會回傳追蹤標頭', h.get('X-Jeepay-Channel-Trace') is None and '簽章驗證失敗' in body, body[:200])

# 4. 查單測試：以下單得到的支付單號查詢（只查本地，不打上游）
pay_order_id = None
try: pay_order_id = json.loads(d['responseBody'])['data']['payOrderId']
except Exception: pass
q = {'mchNo': MCH, 'appId': APP}
q.update({'payOrderId': pay_order_id} if pay_order_id else {'mchOrderNo': req.get('mchOrderNo')})
r = call('POST', MGR + '/api/apiTest/query', god, body=q)
qd = r.get('data') or {}
check('查單：回傳完整請求與原始回應，且沒有上游往來', json.loads(qd.get('requestBody') or '{}').get('mchNo') == MCH and qd.get('responseBody') and qd.get('channelTrace') == [], qd)
print('INFO 查單回應：', (qd.get('responseBody') or '')[:300])
r = call('POST', MGR + '/api/apiTest/query', god, body={'mchNo': MCH, 'appId': APP})
check('查單：沒填單號被擋', r.get('code') != 0, r)

# 5. 簽名驗證：錯誤簽名 → sign 不通過，並列出待簽字串
bad = {'mchNo': MCH, 'appId': APP, 'signType': 'ML-DSA-65', 'reqTime': str(int(time.time() * 1000)), 'mchOrderNo': 'X1', 'sign': base64.b64encode(b'x' * 3309).decode()}
r = call('POST', MGR + '/api/apiTest/verifySign', god, body={'requestJson': json.dumps(bad)})
v = r.get('data') or {}
checks = {c['item']: c['ok'] for c in v.get('checks', [])}
check('驗簽：signType、reqTime、商戶應用通過，sign 不通過', checks.get('signType') and checks.get('reqTime') and checks.get('mchNo / appId') and checks.get('sign') is False, v)
check('驗簽：待簽字串依鍵排序', (v.get('canonical') or '').startswith('appId=' + APP + '&mchNo='), v.get('canonical'))
r = call('POST', MGR + '/api/apiTest/verifySign', god, body={'requestJson': '{bad json'})
check('驗簽：不是 JSON 時提示', r.get('code') != 0 and 'JSON' in r.get('msg', ''), r)
# 用商戶私鑰簽一筆正確的下單請求（MerchantApiExample print-create，容器內 JDK 25），應全部通過
signed = sh("sudo -n docker run --rm -v %(s)s/tools:/w:ro -v %(s)s/mkeys:/k:ro -v %(s)s/secrets/sign-platform-public-key.pem:/p.pem:ro "
            "-e UAT_BASE_URL=http://payment:9216 -e UAT_MERCHANT_ID=%(m)s -e UAT_APP_ID=%(a)s -e UAT_PRIVATE_KEY_FILE=/k/smoke-merchant-private-key.pem "
            "-e UAT_PLATFORM_PUBLIC_KEY_FILE=/p.pem -e UAT_NOTIFY_URL=https://example.test/cb eclipse-temurin:25-jdk-noble java /w/MerchantApiExample.java print-create RYO_IBON"
            % {'s': S, 'm': MCH, 'a': APP}).stdout.strip()
r = call('POST', MGR + '/api/apiTest/verifySign', god, body={'requestJson': signed})
check('驗簽：商戶私鑰簽的請求全部通過', signed.startswith('{') and all(c['ok'] for c in (r.get('data') or {}).get('checks', [{'ok': False}])), (signed[:200], r))

# 6. 登入裝置總覽
r = call('GET', MGR + '/api/loginDevices/overview', god)
o = r.get('data') or {}
mine = o.get('mine') or []
check('登入裝置：看得到自己的 IP、裝置識別碼、型號、GPS', mine and mine[0].get('userIp') and mine[0].get('deviceId') == 'smoke-device-0001' and mine[0].get('latitude') is not None, mine[:1])
check('登入裝置：列出代理清單（含 lastLogin 欄位）', isinstance(o.get('agents'), list), o)
print('INFO 代理數：', len(o.get('agents') or []), '有登入紀錄：', sum(1 for a in o.get('agents') or [] if a.get('lastLogin')))
r = call('GET', MGR + '/api/loginDevices/agent', god, params={'agentNo': 'A_HOUSE'})
check('登入裝置：單一代理歷史', r.get('code') == 0 and isinstance(r.get('data'), list), r)

print('\nRESULT ok=%d fail=%d' % (ok, fail))
