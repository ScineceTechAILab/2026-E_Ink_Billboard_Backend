package com.stalab.e_ink_billboard_backend.service;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.model.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 登录服务
 * @Version: 1.0
 */
@Service
@Slf4j
public class AuthService {

    @Value("${wechat.appid}")
    private String appid;
    @Value("${wechat.secret}")
    private String secret;


    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public AuthService(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    public LoginVO wechatLogin(String code) {
        // 1. 找微信服务器换 OpenID
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid={}&secret={}&js_code={}&grant_type=authorization_code";
        String response = HttpUtil.get(StrUtil.format(url, appid, secret, code));

        JSONObject json = JSONUtil.parseObj(response);
        String openid = json.getStr("openid");

        if (StrUtil.isBlank(openid)) {
            log.error("微信登录失败：{}", response);
            log.error("code={}", code);
            throw new BusinessException(400, "微信登录失败");
        }

        // 2. 查数据库：这个人以前来过吗？
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));

        if (user == null) {
            // 3. 没来过 -> 自动注册为【游客】
            user = new User();
            user.setOpenid(openid);
            user.setRole("VISITOR"); // 默认都是游客
            userMapper.insert(user);
        }
        log.info("用户登录成功，ID：{}，角色：{}", user.getId(), user.getRole());
        // 4. 生成 Token (把 Role 塞进去)
        String token = jwtUtils.createToken(user.getId(), user.getRole());

        // 5. 返回给前端
        return LoginVO.builder()
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .build();
    }
}
