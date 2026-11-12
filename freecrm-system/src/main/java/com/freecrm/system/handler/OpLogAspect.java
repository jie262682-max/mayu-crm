/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.freecrm.common.log.OpLog;
import com.freecrm.system.entity.SysOperLog;
import com.freecrm.system.mapper.SysOperLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/** 操作日志切面：@OpLog 注解的方法自动记录（含失败与耗时） */
@Aspect
@Component
public class OpLogAspect {

    /** 敏感字段脱敏：密码/密钥不得写入操作日志（" = 双引号） */
    private static final java.util.regex.Pattern SENSITIVE =
            java.util.regex.Pattern.compile("(\\u0022)(password|oldPassword|accessSecret|token|secret)(\\u0022\\\\s*:\\u0022)[^\\u0022]*(\\u0022)");

    private final SysOperLogMapper logMapper;

    public OpLogAspect(SysOperLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog log = new SysOperLog();
        log.setModule(opLog.module());
        log.setAction(opLog.action());
        log.setMethod(pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName());
        try {
            String json = JSONUtil.toJsonStr(pjp.getArgs());
            json = SENSITIVE.matcher(json).replaceAll("$1$2$3***$4");
            log.setParams(json.substring(0, Math.min(2000, json.length())));
        } catch (Exception ignore) { }
        try {
            fillRequestInfo(log);
        } catch (Exception ignore) { }
        try {
            Object result = pjp.proceed();
            log.setSuccess(1);
            return result;
        } catch (Throwable e) {
            log.setSuccess(0);
            log.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName()
                    : e.getMessage().substring(0, Math.min(480, e.getMessage().length())));
            throw e;
        } finally {
            log.setCostMs(System.currentTimeMillis() - start);
            try {
                logMapper.insert(log);
            } catch (Exception ignore) { /* 日志失败不影响业务 */ }
        }
    }

    private void fillRequestInfo(SysOperLog log) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            log.setIp(req.getHeader("X-Real-IP") != null ? req.getHeader("X-Real-IP") : req.getRemoteAddr());
        }
        if (StpUtil.isLogin()) {
            log.setUserId(StpUtil.getLoginIdAsLong());
            log.setUsername(String.valueOf(StpUtil.getSession().get("username")));
        }
    }
}
