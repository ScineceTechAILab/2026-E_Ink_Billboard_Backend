package com.stalab.e_ink_billboard_backend.service.wx;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信内容安全服务
 * 负责调用微信小程序内容安全接口进行图片/视频审核
 */
@Slf4j
@Service
public class WeChatContentSecurityService {

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.secret}")
    private String secret;

    private String accessToken;
    private long tokenExpirationTime = 0;

    /**
     * 获取 AccessToken
     * 自动处理缓存和过期
     */
    private synchronized String getAccessToken() {
        if (System.currentTimeMillis() < tokenExpirationTime && StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + secret;
        String response = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("获取微信 AccessToken 失败: {}", response);
            throw new BusinessException("系统内部错误: 获取鉴权凭证失败");
        }

        accessToken = json.getStr("access_token");
        int expiresIn = json.getInt("expires_in");
        // 提前 5 分钟过期，防止临界点问题
        tokenExpirationTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

        log.info("刷新微信 AccessToken 成功");
        return accessToken;
    }

    /**
     * 校验图片内容是否合规
     * @param file 图片文件
     * @return true=合规, false=违规
     */
    public boolean checkImage(MultipartFile file) {
        try {
            // 转换为临时文件 (使用 copy 而不是 transferTo，避免源文件被删除或流关闭)
            File tempFile = File.createTempFile("wechat_check_", ".jpg");
            FileUtil.writeFromStream(file.getInputStream(), tempFile);
            boolean result = checkImage(tempFile);
            FileUtil.del(tempFile); // 删除临时文件
            return result;
        } catch (IOException e) {
            log.error("图片转换临时文件失败", e);
            throw new BusinessException("图片审核前置处理失败");
        }
    }

    /**
     * 校验图片内容是否合规 (BufferedImage)
     * @param image 图片对象
     * @return true=合规, false=违规
     */
    public boolean checkImage(BufferedImage image) {
        try {
            // 转换为临时文件
            File tempFile = File.createTempFile("wechat_check_", ".jpg");
            ImgUtil.write(image, tempFile);
            boolean result = checkImage(tempFile);
            FileUtil.del(tempFile);
            return result;
        } catch (IOException e) {
            log.error("图片对象转换文件失败", e);
            throw new BusinessException("图片审核前置处理失败");
        }
    }

    /**
     * 核心校验逻辑
     * 调用 security.imgSecCheck 接口
     */
    private boolean checkImage(File file) {
        String token = getAccessToken();
        String url = "https://api.weixin.qq.com/wxa/img_sec_check?access_token=" + token;

        // 构建请求
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("media", file);

        try {
            String response = HttpUtil.post(url, paramMap);
            JSONObject json = JSONUtil.parseObj(response);

            Integer errCode = json.getInt("errcode");
            if (errCode == 0) {
                return true;
            } else if (errCode == 87014) {
                log.warn("图片内容违规: {}", json.getStr("errmsg"));
                return false;
            } else {
                log.error("微信内容安全接口调用异常: {}", response);
                // 其他错误视为审核不通过，或者可以抛出异常
                throw new BusinessException("内容审核服务异常: " + json.getStr("errmsg"));
            }
        } catch (Exception e) {
            log.error("调用微信审核接口失败", e);
            throw new BusinessException("内容审核网络异常");
        }
    }
}
