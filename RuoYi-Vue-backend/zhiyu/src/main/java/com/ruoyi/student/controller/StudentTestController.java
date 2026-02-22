package com.ruoyi.student.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 测试控制器
 * 用于验证Spring Boot是否能够正确扫描和注册zhiyu模块中的控制器
 */
@RestController
public class StudentTestController {
    
    /**
     * 简单的测试接口
     */
    @Anonymous
    @GetMapping("/test/hello")
    public AjaxResult hello() {
        return AjaxResult.success("Hello from zhiyu module!");
    }
}