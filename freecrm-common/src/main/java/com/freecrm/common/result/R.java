/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务码：0 成功，非 0 失败 */
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return build(0, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return build(0, "ok", data);
    }

    public static <T> R<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    public static <T> R<T> build(int code, String msg, T data) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        return r;
    }
}
