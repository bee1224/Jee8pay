/*
 * Copyright (c) 2021-2031, 河北计全科技有限公司 (https://www.jeequan.com & jeequan@126.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeequan.jeepay.core.utils;

import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

/*
* jeepay工具类
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 16:50
*/
@Slf4j
public class JeepayKit {

    public static byte[] AES_KEY = "4ChT08phkz59hquD795X7w==".getBytes();

    /** 加密 **/
    public static String aesEncode(String str){
        return SecureUtil.aes(JeepayKit.AES_KEY).encryptHex(str);
    }

    public static String aesDecode(String str){
        return SecureUtil.aes(JeepayKit.AES_KEY).decryptStr(str);
    }



    // 商戶 API 的 MD5 簽名已移除，改用 MlDsaKit（ADR-0013）

    /** map 转换为  url参数 **/
    public static String genUrlParams(Map<String, Object> paraMap) {
        if(paraMap == null || paraMap.isEmpty()) {
            return "";
        }
        StringBuffer urlParam = new StringBuffer();
        Set<String> keySet = paraMap.keySet();
        int i = 0;
        for(String key:keySet) {
            urlParam.append(key).append("=").append( paraMap.get(key) == null ? "" : doEncode(paraMap.get(key).toString()) );
            if(++i == keySet.size()) {
                break;
            }
            urlParam.append("&");
        }
        return urlParam.toString();
    }

    static String doEncode(String str) {
        if(str.contains("+")) {
            return URLEncoder.encode(str);
        }
        return str;
    }

}
