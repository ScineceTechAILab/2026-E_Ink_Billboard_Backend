package com.stalab.e_ink_billboard_backend.controller;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 微信公众号交互控制器
 * 用于接收微信服务器的验证请求和消息推送
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat/mp/portal")
public class WeChatMpController {

    private final WxMpService wxMpService;
    private final WxMpMessageRouter messageRouter;

    public WeChatMpController(WxMpService wxMpService, WxMpMessageRouter messageRouter) {
        this.wxMpService = wxMpService;
        this.messageRouter = messageRouter;
    }

    /**
     * 接收微信服务器的认证请求（GET）
     * 微信后台配置服务器URL时会发送此请求
     */
    @GetMapping
    public String auth(@RequestParam(name = "signature", required = false) String signature,
                       @RequestParam(name = "timestamp", required = false) String timestamp,
                       @RequestParam(name = "nonce", required = false) String nonce,
                       @RequestParam(name = "echostr", required = false) String echostr) {

        log.info("接收到微信认证请求：[signature=[{}], timestamp=[{}], nonce=[{}], echostr=[{}]]",
                signature, timestamp, nonce, echostr);

        if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
            throw new IllegalArgumentException("请求参数非法，请核实!");
        }

        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }

        return "非法请求";
    }

    /**
     * 接收微信服务器的消息推送（POST）
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {

        log.info("接收微信请求：[openid=[{}], [signature=[{}], encType=[{}], msgSignature=[{}], timestamp=[{}], nonce=[{}], requestBody=[\n{}\n] ",
                openid, signature, encType, msgSignature, timestamp, nonce, requestBody);

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            throw new IllegalArgumentException("非法请求，可能属于伪造的请求！");
        }

        String out = null;
        if (encType == null) {
            // 明文传输的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
            WxMpXmlOutMessage outMessage = this.messageRouter.route(inMessage);
            if (outMessage == null) {
                return "";
            }

            out = outMessage.toXml();
        } else if ("aes".equalsIgnoreCase(encType)) {
            // AES加密的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody, wxMpService.getWxMpConfigStorage(),
                    timestamp, nonce, msgSignature);
            log.debug("解密后的消息：{} ", inMessage.toString());
            WxMpXmlOutMessage outMessage = this.messageRouter.route(inMessage);
            if (outMessage == null) {
                return "";
            }

            out = outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage());
        }

        log.debug("组装回复信息：{}", out);
        return out;
    }
}
