package com.ruoyi.student.controller;

import java.util.List;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.ServletUtils;

/**
 * 自定义控制器基类，不继承框架的BaseController
 * 提供控制器通用功能，如分页处理、响应结果封装等
 */
public class CustomBaseController {
    
    /**
     * 设置请求数据中的日期格式
     */
    @InitBinder
    public void initBinder() {
        // 这里可以添加日期格式转换逻辑
    }
    
    /**
     * 设置请求分页数据
     */
    protected void startPage() {
        // 获取分页参数
        Integer pageNum = ServletUtils.getParameterToInt("pageNum", 1);
        Integer pageSize = ServletUtils.getParameterToInt("pageSize", 10);
        String orderByColumn = ServletUtils.getParameter("orderByColumn");
        String isAsc = ServletUtils.getParameter("isAsc", "asc");
        
        // 构建排序字符串
        String orderBy = "";
        if (orderByColumn != null && !orderByColumn.isEmpty()) {
            orderBy = orderByColumn + " " + isAsc;
        }
        
        // 设置分页
        PageHelper.startPage(pageNum, pageSize, orderBy);
    }
    
    /**
     * 清理分页的线程变量
     */
    protected void clearPage() {
        PageHelper.clearPage();
    }
    
    /**
     * 响应请求分页数据
     */
    @SuppressWarnings("rawtypes")
    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rspData = new TableDataInfo();
        PageInfo<?> pageInfo = new PageInfo<>(list);
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(pageInfo.getTotal());
        return rspData;
    }
    
    /**
     * 返回成功消息
     */
    public AjaxResult success() {
        return AjaxResult.success();
    }
    
    /**
     * 返回成功消息
     */
    public AjaxResult success(Object data) {
        return AjaxResult.success(data);
    }
    
    /**
     * 返回失败消息
     */
    public AjaxResult error() {
        return AjaxResult.error();
    }
    
    /**
     * 返回失败消息
     */
    public AjaxResult error(String message) {
        return AjaxResult.error(message);
    }
    
    /**
     * 操作结果返回
     */
    protected AjaxResult toAjax(int rows) {
        return rows > 0 ? success() : error();
    }
    
    /**
     * 操作结果返回
     */
    protected AjaxResult toAjax(boolean flag) {
        return flag ? success() : error();
    }
}