package com.stalab.e_ink_billboard_backend.service.wx;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信公众号消息处理器
 */
@Component
public class WeChatMpHandler implements WxMpMessageHandler {

    private final VerificationService verificationService;

    public WeChatMpHandler(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context,
                                    WxMpService wxMpService,
                                    WxSessionManager sessionManager) throws WxErrorException {

        String content = wxMessage.getContent();

        // 如果用户发送"验证码"
        if ("验证码".equals(content) || "code".equalsIgnoreCase(content)) {
            String code = verificationService.generateCode();
            String replyContent = "您的验证码是：" + code + "\n有效期30分钟，请在小程序中输入使用。";

            return WxMpXmlOutMessage.TEXT().content(replyContent)
                    .fromUser(wxMessage.getToUser())
                    .toUser(wxMessage.getFromUser())
                    .build();
        }

        // 关注事件
        if ("event".equals(wxMessage.getMsgType()) && "subscribe".equals(wxMessage.getEvent())) {
             return WxMpXmlOutMessage.TEXT().content("欢迎关注！发送“验证码”获取墨水屏推送权限。")
                    .fromUser(wxMessage.getToUser())
                    .toUser(wxMessage.getFromUser())
                    .build();
        }

        // 默认回复
        return WxMpXmlOutMessage.TEXT().content("发送“验证码”获取使用权限")
                .fromUser(wxMessage.getToUser())
                .toUser(wxMessage.getFromUser())
                .build();
    }
}
