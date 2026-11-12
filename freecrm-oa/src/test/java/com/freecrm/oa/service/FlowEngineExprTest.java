/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.oa.service;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** 条件表达式求值单测（纯静态方法，不依赖 Spring 容器） */
class FlowEngineExprTest {

    @Test
       void testSimpleGte() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 50000.0);
        assertTrue(FlowEngine.evalExpr("amount >= 50000", vars));
        assertFalse(FlowEngine.evalExpr("amount > 50000", vars));
    }

    @Test
    void testCombinedAnd() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 30000.0);
        vars.put("days", 5);
        assertTrue(FlowEngine.evalExpr("amount >= 30000 && days < 10", vars));
        assertFalse(FlowEngine.evalExpr("amount > 30000 && days < 10", vars));
    }

    @Test
    void testMissingVarIsFalse() {
        Map<String, Object> vars = new HashMap<>();
        assertFalse(FlowEngine.evalExpr("amount >= 1", vars));
        vars.put("amount", "abc");
        assertFalse(FlowEngine.evalExpr("amount >= 1", vars));
    }
}
