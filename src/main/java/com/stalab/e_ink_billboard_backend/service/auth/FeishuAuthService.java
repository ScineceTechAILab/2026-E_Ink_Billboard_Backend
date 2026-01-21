package com.stalab.e_ink_billboard_backend.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.LoginSource;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.config.FeishuConfig;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.model.vo.FeishuUserInfoVO;
import com.stalab.e_ink_billboard_backend.model.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 飞书登录服务（管理员专用）
 */
@Service("feishuAuthService")
@Slf4j
public class FeishuAuthService implements AuthService {

    private final FeishuConfig feishuConfig;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public FeishuAuthService(FeishuConfig feishuConfig, UserMapper userMapper, JwtUtils jwtUtils) {
        this.feishuConfig = feishuConfig;
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(String code) {
        String accessToken = getAccessToken(code);
        FeishuUserInfoVO userInfo = getUserInfo(accessToken);
        User user = getOrCreateFeishuUser(userInfo);
        String token = jwtUtils.createToken(user.getId(), user.getRole());
        log.info("飞书管理员登录成功，用户ID：{}，昵称：{}", user.getId(), user.getNickname());
        return LoginVO.builder()
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .build();
    }

    private String getAccessToken(String code) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("grant_type", "authorization_code");
            requestBody.set("client_id", feishuConfig.getAppId());
            requestBody.set("client_secret", feishuConfig.getAppSecret());
            requestBody.set("code", code);
            requestBody.set("redirect_uri", feishuConfig.getRedirectUri());
            HttpResponse response = HttpRequest.post(feishuConfig.getOauthTokenUrl())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(requestBody.toString())
                    .timeout(5000)
                    .execute();

            String responseBody = response.body();
            log.info("飞书获取access_token响应：{}", responseBody);
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            if (jsonResponse.getInt("code") != 0) {
                log.error("飞书获取access_token失败：{}", responseBody);
                throw new BusinessException(400, "飞书登录失败：" + jsonResponse.getStr("msg"));
            }
            String accessToken = jsonResponse.get("access_token", String.class);
            if (StrUtil.isBlank(accessToken)) {
                log.error("飞书返回的access_token为空");
                throw new BusinessException(400, "获取飞书access_token失败");
            }
            return accessToken;
        } catch (Exception e) {
            log.error("飞书获取access_token异常", e);
            throw new BusinessException(500, "飞书登录异常：" + e.getMessage());
        }
    }

    private FeishuUserInfoVO getUserInfo(String accessToken) {
        try {
            HttpResponse response = HttpRequest.get(feishuConfig.getUserInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .timeout(5000)
                    .execute();

            String responseBody = response.body();
            log.info("飞书获取用户信息响应：{}", responseBody);
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            if (jsonResponse.getInt("code") != 0) {
                log.error("飞书获取用户信息失败：{}", responseBody);
                throw new BusinessException(400, "获取飞书用户信息失败：" + jsonResponse.getStr("msg"));
            }
            JSONObject data = jsonResponse.getJSONObject("data");
            FeishuUserInfoVO userInfo = new FeishuUserInfoVO();
            userInfo.setOpenId(data.getStr("open_id"));
            userInfo.setUnionId(data.getStr("union_id"));
            userInfo.setName(data.getStr("name"));
            userInfo.setEnName(data.getStr("en_name"));
            userInfo.setAvatar(data.getStr("avatar_url"));
            userInfo.setMobile(data.getStr("mobile"));
            userInfo.setEmail(data.getStr("email"));
            userInfo.setEmployeeNo(data.getStr("employee_no"));
            if (StrUtil.isBlank(userInfo.getOpenId())) {
                log.error("飞书返回的用户open_id为空");
                throw new BusinessException(400, "获取飞书用户标识失败");
            }
            return userInfo;
        } catch (Exception e) {
            log.error("飞书获取用户信息异常", e);
            throw new BusinessException(500, "获取飞书用户信息异常：" + e.getMessage());
        }
    }

    private User getOrCreateFeishuUser(FeishuUserInfoVO userInfo) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getFeishuOpenId, userInfo.getOpenId()));
        if (user == null) {
            user = new User();
            user.setFeishuOpenId(userInfo.getOpenId());
            user.setFeishuUnionId(userInfo.getUnionId());
            user.setNickname(StrUtil.isNotBlank(userInfo.getName()) ? userInfo.getName() : "飞书用户");
            user.setAvatar(userInfo.getAvatar());
            user.setFeishuMobile(userInfo.getMobile());
            user.setFeishuEmployeeNo(userInfo.getEmployeeNo());
            user.setRole(UserRole.ADMIN.getCode());
            user.setLoginSource(LoginSource.FEISHU.getCode());
            userMapper.insert(user);
            log.info("创建飞书管理员用户成功，ID：{}，昵称：{}", user.getId(), user.getNickname());
        } else {
            boolean needUpdate = false;
            if (!StrUtil.equals(user.getRole(), UserRole.ADMIN.getCode())) {
                user.setRole(UserRole.ADMIN.getCode());
                needUpdate = true;
                log.info("用户ID：{} 角色更新为ADMIN", user.getId());
            }
            if (!StrUtil.equals(user.getLoginSource(), LoginSource.FEISHU.getCode())) {
                user.setLoginSource(LoginSource.FEISHU.getCode());
                needUpdate = true;
            }
            if (StrUtil.isNotBlank(userInfo.getName()) && !StrUtil.equals(user.getNickname(), userInfo.getName())) {
                user.setNickname(userInfo.getName());
                needUpdate = true;
            }
            if (StrUtil.isNotBlank(userInfo.getAvatar()) && !StrUtil.equals(user.getAvatar(), userInfo.getAvatar())) {
                user.setAvatar(userInfo.getAvatar());
                needUpdate = true;
            }
            if (needUpdate) {
                userMapper.updateById(user);
                log.info("更新飞书用户信息成功，ID：{}", user.getId());
            }
        }
        return user;
    }
}
