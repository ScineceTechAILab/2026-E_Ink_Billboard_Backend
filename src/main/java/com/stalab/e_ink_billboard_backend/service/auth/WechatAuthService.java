package com.stalab.e_ink_billboard_backend.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.LoginSource;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.model.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 微信小程序登录实现（游客）
 */
@Service("wechatAuthService")
@Slf4j
public class WechatAuthService implements AuthService {

    @Value("${wechat.appid}")
    private String appid;
    @Value("${wechat.secret}")
    private String secret;

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public WechatAuthService(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public LoginVO login(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid={}&secret={}&js_code={}&grant_type=authorization_code";
        String response = HttpUtil.get(StrUtil.format(url, appid, secret, code));

        JSONObject json = JSONUtil.parseObj(response);
        String openid = json.getStr("openid");

        if (StrUtil.isBlank(openid)) {
            log.error("微信登录失败：{}", response);
            log.error("code={}", code);
            throw new BusinessException(400, "微信登录失败");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getWx_openid, openid));

        if (user == null) {
            user = new User();
            user.setWx_openid(openid);
            user.setRole(UserRole.VISITOR.getCode());
            user.setLoginSource(LoginSource.WECHAT.getCode());
            userMapper.insert(user);
        }
        log.info("用户登录成功，ID：{}，角色：{}", user.getId(), user.getRole());
        String token = jwtUtils.createToken(user.getId(), user.getRole());

        return LoginVO.builder()
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .build();
    }
}
