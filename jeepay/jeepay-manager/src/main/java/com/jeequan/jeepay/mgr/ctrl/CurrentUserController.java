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
package com.jeequan.jeepay.mgr.ctrl;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.cache.ITokenService;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.SysEntitlement;
import com.jeequan.jeepay.core.entity.SysLoginDevice;
import com.jeequan.jeepay.core.entity.SysUser;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.model.security.JeeUserDetails;
import com.jeequan.jeepay.core.utils.TreeDataBuilder;
import com.jeequan.jeepay.service.impl.SysEntitlementService;
import com.jeequan.jeepay.service.impl.SysLoginDeviceService;
import com.jeequan.jeepay.service.impl.SysUserAuthService;
import com.jeequan.jeepay.service.impl.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
* 当前登录者的信息相关接口
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:10
*/
@Tag(name = "登录者信息")
@RestController
@RequestMapping("api/current")
public class CurrentUserController extends CommonCtrl{

	@Autowired private SysEntitlementService sysEntitlementService;
	@Autowired private SysUserService sysUserService;
	@Autowired private SysUserAuthService sysUserAuthService;
	@Autowired private SysLoginDeviceService sysLoginDeviceService;

	@Operation(summary = "查询当前登录者的用户信息")
	@Parameters({
			@Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER)
	})
	@RequestMapping(value="/user", method = RequestMethod.GET)
	public ApiRes<SysUser> currentUserInfo() {

		///当前用户信息
		JeeUserDetails jeeUserDetails = getCurrentUser();
		SysUser user = jeeUserDetails.getSysUser();

		//1. 当前用户所有权限ID集合
		List<String> entIdList = new ArrayList<>();
		jeeUserDetails.getAuthorities().stream().forEach(r->entIdList.add(r.getAuthority()));

		List<SysEntitlement> allMenuList = new ArrayList<>();    //所有菜单集合

		//2. 查询出用户所有菜单集合 (包含左侧显示菜单 和 其他类型菜单 )
		if(!entIdList.isEmpty()){
			allMenuList = sysEntitlementService.list(SysEntitlement.gw()
					.in(SysEntitlement::getEntId, entIdList)
					.in(SysEntitlement::getEntType, Arrays.asList(CS.ENT_TYPE.MENU_LEFT, CS.ENT_TYPE.MENU_OTHER))
					.eq(SysEntitlement::getSysType, CS.SYS_TYPE.MGR)
					.eq(SysEntitlement::getState, CS.PUB_USABLE));
		}

		//4. 转换为json树状结构
		JSONArray jsonArray = (JSONArray) JSON.toJSON(allMenuList);
		List<JSONObject> allMenuRouteTree = new TreeDataBuilder(jsonArray,
				"entId", "pid", "children", "entSort", true)
				.buildTreeObject();

		//1. 所有权限ID集合
		user.addExt("entIdList", entIdList);
		user.addExt("allMenuRouteTree", allMenuRouteTree);
		return ApiRes.ok(getCurrentUser().getSysUser());
	}


	/** 修改个人信息 */
	@Operation(summary = "修改个人信息--基本信息")
	@Parameters({
			@Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "avatarUrl", description = "头像地址"),
			@Parameter(name = "realname", description = "真实姓名"),
			@Parameter(name = "sex", description = "性别 0-未知, 1-男, 2-女")
	})
	@RequestMapping(value="/user", method = RequestMethod.PUT)
	@MethodLog(remark = "修改信息")
	public ApiRes modifyCurrentUserInfo() {

		// 代理帳號（團長、隊長、子帳號）的個人資料只能由上帝修改
		String belong = getCurrentUser().getSysUser().getBelongInfoId();
		if (StringUtils.isNotEmpty(belong) && !"0".equals(belong)) {
			throw new BizException("代理帳號的個人資料只能由平台修改");
		}

		//修改头像
		String avatarUrl = getValString("avatarUrl");
		String realname = getValString("realname");
		Byte sex = getValByte("sex");
		SysUser updateRecord = new SysUser();
		updateRecord.setSysUserId(getCurrentUser().getSysUser().getSysUserId());
		if (StringUtils.isNotEmpty(avatarUrl)) {
            updateRecord.setAvatarUrl(avatarUrl);
        }
		if (StringUtils.isNotEmpty(realname)) {
            updateRecord.setRealname(realname);
        }
		if (sex != null) {
            updateRecord.setSex(sex);
        }
		sysUserService.updateById(updateRecord);


		//保存redis最新数据
		JeeUserDetails currentUser = getCurrentUser();
		currentUser.setSysUser(sysUserService.getById(getCurrentUser().getSysUser().getSysUserId()));
		ITokenService.refData(currentUser);

		return ApiRes.ok();
	}


	/** 修改密码 */
	@Operation(summary = "修改个人信息--安全信息")
	@Parameters({
			@Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "confirmPwd", description = "新密码"),
			@Parameter(name = "originalPwd", description = "原密码")
	})
	@RequestMapping(value="modifyPwd", method = RequestMethod.PUT)
	@MethodLog(remark = "修改密碼")
	public ApiRes modifyPwd() throws BizException{

		//更改密码，验证当前用户信息
		String currentUserPwd = Base64.decodeStr(getValStringRequired("originalPwd")); //当前用户登录密码
		//验证当前密码是否正确
		if(!sysUserAuthService.validateCurrentUserPwd(currentUserPwd)){
			throw new BizException("原密碼驗證失敗！");
		}

		String opUserPwd = Base64.decodeStr(getValStringRequired("confirmPwd"));

		// 验证原密码与新密码是否相同
		if (opUserPwd.equals(currentUserPwd)) {
			throw new BizException("新密碼與原密碼不能相同！");
		}

		sysUserAuthService.resetAuthInfo(getCurrentUser().getSysUser().getSysUserId(), null, null, opUserPwd, CS.SYS_TYPE.MGR);
		//调用登出接口
		return logout();
	}

//	/** 登出 */
//	@RequestMapping(value="logout", method = RequestMethod.POST)
//	@MethodLog(remark = "登出")
	public ApiRes logout() throws BizException{

		ITokenService.removeIToken(getCurrentUser().getCacheKey(), getCurrentUser().getSysUser().getSysUserId());
		return ApiRes.ok();
	}




	// ---------------- 登入裝置紀錄 ----------------

	@Operation(summary = "回報本次登入的裝置資訊（登入成功後由前端呼叫一次）")
	@RequestMapping(value="/loginDevice", method = RequestMethod.POST)
	public ApiRes reportLoginDevice() {
		SysUser user = getCurrentUser().getSysUser();
		JSONObject p = getReqParamJSON();
		SysLoginDevice d = new SysLoginDevice()
				.setSysUserId(user.getSysUserId()).setSysType(CS.SYS_TYPE.MGR).setBelongInfoId(user.getBelongInfoId())
				.setUserName(user.getRealname()).setUserIp(getClientIp()).setUserAgent(request.getHeader("User-Agent"))
				.setDeviceId(p.getString("deviceId")).setDeviceModel(p.getString("deviceModel")).setDeviceOs(p.getString("deviceOs"))
				.setGeoState(p.getString("geoState")).setLatitude(p.getBigDecimal("latitude")).setLongitude(p.getBigDecimal("longitude"))
				.setAccuracy(p.getInteger("accuracy"));
		sysLoginDeviceService.record(d);
		return ApiRes.ok();
	}

	@Operation(summary = "自己最近的登入：代理帳號只回時間與完整 IP；裝置只給平台帳號（上帝）看")
	@RequestMapping(value="/loginDevices", method = RequestMethod.GET)
	public ApiRes loginDevices() {
		SysUser user = getCurrentUser().getSysUser();
		boolean platform = StringUtils.isBlank(user.getBelongInfoId()) || "0".equals(user.getBelongInfoId());
		List<JSONObject> list = new ArrayList<>();
		sysLoginDeviceService.recentByUser(user.getSysUserId(), 5).forEach(d -> {
			JSONObject o = new JSONObject();
			o.put("createdAt", d.getCreatedAt());
			o.put("userIp", d.getUserIp());
			if (platform) {
				o.put("deviceModel", d.getDeviceModel());
				o.put("deviceOs", d.getDeviceOs());
			}
			list.add(o);
		});
		return ApiRes.ok(list);
	}
}
