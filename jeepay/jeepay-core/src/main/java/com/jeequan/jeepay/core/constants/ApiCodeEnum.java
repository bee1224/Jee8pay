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
package com.jeequan.jeepay.core.constants;

/*
* 接口返回码
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/5/24 17:07
*/
public enum ApiCodeEnum{

    SUCCESS(0, "SUCCESS"), //请求成功

    CUSTOM_FAIL(9999, "自定義業務異常"),  //自定义业务异常

    SYSTEM_ERROR(10, "系統異常[%s]"),
    PARAMS_ERROR(11, "參數有誤[%s]"),
    DB_ERROR(12, "資料庫服務異常"),

    SYS_OPERATION_FAIL_CREATE(5000, "新增失敗"),
    SYS_OPERATION_FAIL_DELETE(5001, "刪除失敗"),
    SYS_OPERATION_FAIL_UPDATE(5002, "修改失敗"),
    SYS_OPERATION_FAIL_SELETE(5003, "記錄不存在"),
    SYS_PERMISSION_ERROR(5004, "權限錯誤，當前用戶不支持此操作");


    private int code;

    private String msg;

    ApiCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode(){
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }
}
