# ADR-0013 隔離測試：商戶 API 改用 ML-DSA-65、移除 MD5；後端 JDK 25。
# 在測試伺服器的 jee8pay-smoke（全新資料庫；manager 127.0.0.1:29217、payment 127.0.0.1:29216）上實際呼叫 API。
# 用法：ssh nnviopp-sandbox python3 - < smoke-ml-dsa.py。金鑰與工具在 ~/smoke-mldsa（由 smoke-mldsa-up.sh 產生）。不可對 jee8pay-v2-dev 執行。
import base64, json, os, subprocess, sys, time, urllib.request, urllib.error, urllib.parse
MGR = 'http://127.0.0.1:29217'
PAY = 'http://127.0.0.1:29216'
S = os.path.expanduser('~/smoke-mldsa')
ok = fail = 0
def b64(s): return base64.b64encode(s.encode()).decode()
def call(method, url, token=None, body=None, params=None):
    url = url + ('?' + urllib.parse.urlencode(params) if params else '')
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers={'Content-Type': 'application/json'})
    if token: req.add_header('iToken', token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r: return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try: return json.loads(raw)
        except Exception: return {'code': e.code, 'msg': raw[:200]}
def sh(cmd): return subprocess.run(cmd, shell=True, capture_output=True, text=True)
def sql(q):
    return subprocess.run("sudo -n docker exec -i jee8pay-smoke-db-1 sh -c 'mariadb -uroot -p\"$(cat /run/secrets/db-root-password)\" --default-character-set=utf8mb4 jee8pay_v2_dev -N -B'", shell=True, input=q, capture_output=True, text=True).stdout.strip()
def login(user, pwd):
    v = call('GET', MGR + '/api/anon/auth/vercode')['data']
    code = sh('sudo -n docker exec jee8pay-smoke-redis-1 redis-cli -n 1 get img_code_%s' % v['vercodeToken']).stdout.strip()
    r = call('POST', MGR + '/api/anon/auth/validate', body={'ia': b64(user), 'ip': b64(pwd), 'vc': b64(code), 'vt': b64(v['vercodeToken'])})
    if r.get('code') != 0: print('LOGIN FAIL', user, r); sys.exit(1)
    return r['data']['iToken']
def check(name, cond, detail=''):
    global ok, fail
    if cond: ok += 1; print('PASS', name)
    else: fail += 1; print('FAIL', name, '|', str(detail)[:400])
def pem_body(p): return ''.join(l for l in p.strip().splitlines() if not l.startswith('-----'))

MCH, APP = 'M_SMOKE_MLDSA', 'APP_SMOKE_MLDSA'
# 測試商戶：沒有公鑰；RYO_IBON 通道與假的上游設定（只做會在呼叫上游前被拒絕的請求）
sql("""insert into t_mch_info (mch_no, mch_name, mch_short_name, state, created_at, updated_at) values ('%(m)s','ML-DSA 冒煙商戶','冒煙',1,now(),now());
insert into t_mch_app (app_id, app_name, mch_no, state, created_at, updated_at) values ('%(a)s','冒煙應用','%(m)s',1,now(),now());
insert into t_mch_pay_passage (mch_no, app_id, if_code, way_code, rate, state, created_at, updated_at) values ('%(m)s','%(a)s','ryo','RYO_IBON',0.025,1,now(),now());
insert into t_pay_interface_config (info_type, info_id, if_code, if_params, if_rate, state, created_at, updated_at) values (3,'%(a)s','ryo','{"environment":"TEST","custId":"SMOKE_DUMMY","apiPassword":"SMOKE_DUMMY"}',0.02,1,now(),now());""" % {'m': MCH, 'a': APP})
check('結構：t_mch_app 沒有 app_secret、有 mch_public_key', sql("select group_concat(column_name) from information_schema.columns where table_schema='jee8pay_v2_dev' and table_name='t_mch_app' and column_name in ('app_secret','mch_public_key')") == 'mch_public_key')

# 1. 尚未上傳公鑰：一律拒絕
r = call('POST', PAY + '/api/pay/query', body={'version': '1.0', 'signType': 'ML-DSA-65', 'reqTime': str(int(time.time() * 1000)), 'mchNo': MCH, 'appId': APP, 'mchOrderNo': 'X', 'sign': 'AAAA'})
check('沒有公鑰的應用不能呼叫 API', r.get('msg') == '商戶應用尚未設定公鑰', r)

# 2. 後台：平台公鑰、上傳商戶公鑰（只接受 ML-DSA-65）
god = login('jeepay', 'jeepay123')
r = call('GET', MGR + '/api/mchApps/platformPublicKey', god)
check('後台取得平台公鑰', r.get('code') == 0 and pem_body(r['data']['platformPublicKey']) == pem_body(open(S + '/secrets/sign-platform-public-key.pem').read()), r)
rsa = sh("openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null | openssl pkey -pubout").stdout
r = call('PUT', MGR + '/api/mchApps/' + APP, god, body={'appName': '冒煙應用', 'mchNo': MCH, 'mchPublicKey': rsa})
check('RSA 公鑰被拒', r.get('code') != 0 and '公鑰格式錯誤' in str(r.get('msg')), r)
r = call('PUT', MGR + '/api/mchApps/' + APP, god, body={'appName': '冒煙應用', 'mchNo': MCH, 'mchPublicKey': 'not a key'})
check('亂碼公鑰被拒', r.get('code') != 0, r)
check('被拒後資料庫仍沒有公鑰', sql("select ifnull(mch_public_key,'NULL') from t_mch_app where app_id='%s'" % APP) == 'NULL')
mpub = open(S + '/mkeys/smoke-merchant-public-key.pem').read()
r = call('PUT', MGR + '/api/mchApps/' + APP, god, body={'appName': '冒煙應用', 'mchNo': MCH, 'mchPublicKey': pem_body(mpub)})
check('上傳 ML-DSA-65 公鑰（純 Base64 也可）', r.get('code') == 0, r)
d = call('GET', MGR + '/api/mchApps/' + APP, god)
check('詳情回傳標準 PEM', d.get('code') == 0 and d['data']['mchPublicKey'].startswith('-----BEGIN PUBLIC KEY-----') and pem_body(d['data']['mchPublicKey']) == pem_body(mpub), d)
r = call('PUT', MGR + '/api/mchApps/' + APP, god, body={'appName': '冒煙應用改名', 'mchNo': MCH})
check('修改時不帶公鑰不會清掉', r.get('code') == 0 and sql("select mch_public_key is not null from t_mch_app where app_id='%s'" % APP) == '1', r)
time.sleep(3)  # 等 payment 收到應用設定變更的 MQ，清掉快取

# 3. 黑箱：簽名、時效、MD5 被拒、參數錯誤在呼叫上游前被拒
bb = sh("sudo -n docker run --rm --network jee8pay-smoke-network -v %s/tools:/w:ro -v %s/mkeys:/k:ro -v %s/secrets/sign-platform-public-key.pem:/p.pem:ro "
        "-e UAT_INTERNAL_BASE_URL=http://payment:9216 -e UAT_MERCHANT_ID=%s -e UAT_APP_ID=%s -e UAT_PRIVATE_KEY_FILE=/k/smoke-merchant-private-key.pem "
        "-e UAT_PLATFORM_PUBLIC_KEY_FILE=/p.pem eclipse-temurin:25-jdk-noble java /w/D01Blackbox.java" % (S, S, S, MCH, APP))
print(bb.stdout.strip())
check('D01 黑箱（ML-DSA 版）全數通過', 'D01_BLACKBOX_SUITE=PASS' in bb.stdout, bb.stdout[-300:] + bb.stderr[-300:])

# 4. 通知：平台私鑰簽 → 存網址 → MQ → POST 給 merchant 服務的測試接收端 → 以平台公鑰驗簽並回 SUCCESS
PO = 'P_SMOKE_MLDSA_0001'
sql("""insert into t_pay_order (pay_order_id, mch_no, app_id, mch_name, mch_type, mch_order_no, if_code, way_code, amount, mch_fee_rate, mch_fee_amount, currency, state, subject, body, notify_url, created_at, updated_at, success_time)
values ('%s','%s','%s','ML-DSA 冒煙商戶',1,'SMOKE-NOTIFY-0001','ryo','RYO_IBON',4000,0.025,100,'TWD',2,'特殊字元 A&B=C+D 測試','body with space','http://merchant:9218/api/anon/paytestNotify/payOrder',now(),now(),now());""" % (PO, MCH, APP))
r = call('POST', MGR + '/api/payOrder/manualNotify', god, body={'payOrderId': PO})
check('人工補發通知已送出', r.get('code') == 0, r)
st = ''
for _ in range(20):
    st = sql("select concat(state,'|',ifnull(res_result,'')) from t_mch_notify_record where order_id='%s'" % PO)
    if st.startswith('2|'): break
    time.sleep(2)
check('通知被 merchant 服務以平台公鑰驗簽成功（回 SUCCESS）', st == '2|SUCCESS', st)
url = sql("select notify_url from t_mch_notify_record where order_id='%s'" % PO)
check('通知網址帶 signType=ML-DSA-65 與 Base64 簽名', 'signType=ML-DSA-65' in url and 'sign=' in url and 'MD5' not in url, url[:200])

# 5. 後台代商戶呼叫支付服務（平台內部金鑰）：以退款為例，簽名須通過、由後續業務規則拒絕
r = call('POST', MGR + '/api/payOrder/refunds/' + PO, god, body={'refundAmount': 1, 'refundReason': 'ML-DSA 冒煙'})
msg = str(r.get('msg'))
check('內部金鑰簽名被支付服務接受（不是簽名錯誤）', '簽' not in msg and '連線' not in msg and r.get('code') is not None, r)
print('INFO 退款回應：', r.get('code'), msg)
logs = sh("sudo -n docker logs jee8pay-smoke-payment-1 2>&1 | grep -c '簽章驗證失敗' ").stdout.strip()
print('INFO payment 簽章驗證失敗次數（黑箱刻意送的）：', logs)

print('\nRESULT ok=%d fail=%d' % (ok, fail))
sys.exit(1 if fail else 0)
