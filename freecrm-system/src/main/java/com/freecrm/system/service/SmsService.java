/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.system.entity.SysSmsLog;
import com.freecrm.system.mapper.SysSmsLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿里云短信：配置存 sys_config（后台可改，无需重启）
 *   sms.enabled / sms.accessKeyId / sms.accessSecret / sms.signName / sms.templateCode
 * 未启用或未配置时静默跳过（返回 false），业务不中断
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final ConfigService configService;
    private final SysSmsLogMapper smsLogMapper;
    private volatile Client client;

    public SmsService(ConfigService configService, SysSmsLogMapper smsLogMapper) {
        this.configService = configService;
        this.smsLogMapper = smsLogMapper;
    }

    public boolean enabled() {
        return "1".equals(configService.get("sms.enabled", "0")) && configured();
    }

    public boolean configured() {
        return notBlank("sms.accessKeyId") && notBlank("sms.accessSecret")
                && notBlank("sms.signName") && notBlank("sms.templateCode");
    }

    /**
     * 发送模板短信
     * @param templateParam 模板变量，如 {"code":"1234"} / {"title":"合同审批"}
     */
    public boolean send(String mobile, Map<String, String> templateParam) {
        SysSmsLog l = new SysSmsLog();
        l.setMobile(mobile);
        l.setChannel("ALIYUN");
        l.setTemplate(configService.get("sms.templateCode", ""));
        try {
            if (!enabled()) {
                l.setSuccess(0);
                l.setErrorMsg("短信未启用或配置不完整");
                smsLogMapper.insert(l);
                return false;
            }
            Client c = obtainClient();
            SendSmsRequest req = new SendSmsRequest()
                    .setPhoneNumbers(mobile)
                    .setSignName(configService.get("sms.signName", ""))
                    .setTemplateCode(configService.get("sms.templateCode", ""))
                    .setTemplateParam(cn.hutool.json.JSONUtil.toJsonStr(templateParam == null ? new HashMap<>() : templateParam));
            SendSmsResponse resp = c.sendSms(req);
            boolean ok = resp.getBody() != null && "OK".equals(resp.getBody().getCode());
            l.setSuccess(ok ? 1 : 0);
            l.setErrorMsg(ok ? null : (resp.getBody() != null ? resp.getBody().getMessage() : "no response"));
            if (!ok) {
                log.warn("短信发送失败 mobile={} : {}", mobile, l.getErrorMsg());
            }
            return ok;
        } catch (Exception e) {
            l.setSuccess(0);
            l.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            log.warn("短信发送异常 mobile={} : {}", mobile, l.getErrorMsg());
            return false;
        } finally {
            try {
                smsLogMapper.insert(l);
            } catch (Exception ignore) { }
        }
    }

    /** 配置修改后强制重建客户端 */
    public void resetClient() {
        client = null;
    }

    public Page<SysSmsLog> page(long pageNo, long pageSize, String mobile) {
        LambdaQueryWrapper<SysSmsLog> qw = new LambdaQueryWrapper<>();
        if (mobile != null && !mobile.isEmpty()) {
            qw.eq(SysSmsLog::getMobile, mobile);
        }
        return smsLogMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(SysSmsLog::getId));
    }

    /** 当前配置（secret 脱敏） */
    public Map<String, String> configView() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("sms.enabled", configService.get("sms.enabled", "0"));
        m.put("sms.accessKeyId", configService.get("sms.accessKeyId", ""));
        String secret = configService.get("sms.accessSecret", "");
        m.put("sms.accessSecret", secret.isEmpty() ? "" : "******");
        m.put("sms.signName", configService.get("sms.signName", ""));
        m.put("sms.templateCode", configService.get("sms.templateCode", ""));
        m.put("sms.notify.todo", configService.get("sms.notify.todo", "1"));
        return m;
    }

    private Client obtainClient() throws Exception {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    Config c = new Config()
                            .setAccessKeyId(configService.get("sms.accessKeyId", ""))
                            .setAccessKeySecret(configService.get("sms.accessSecret", ""));
                    c.endpoint = "dysmsapi.aliyuncs.com";
                    client = new Client(c);
                }
            }
        }
        return client;
    }

    /** 通知模板的变量名（默认 title，可在后台 sys_config sms.var.name 调整以匹配模板） */
    public String varName() {
        return configService.get("sms.var.name", "title");
    }

    private boolean notBlank(String key) {
        String v = configService.get(key, "");
        return v != null && !v.isEmpty();
    }
}
