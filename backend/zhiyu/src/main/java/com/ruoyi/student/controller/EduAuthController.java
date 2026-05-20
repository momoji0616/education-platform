package com.ruoyi.student.controller;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.security.context.AuthenticationContextHolder;
import com.ruoyi.student.domain.EduLoginBody;
import com.ruoyi.student.domain.EduRegisterBody;

/**
 * Pad 端登录注册接口
 */
@RestController
public class EduAuthController extends EduPadSupport
{
    @PostMapping("/student/performance/anonymous/education/login")
    public AjaxResult login(@RequestBody EduLoginBody loginBody)
    {
        String username = StringUtils.trim(loginBody.getUsername());
        String password = loginBody.getPassword();

        loginService.loginPreCheck(username, password);

        Authentication authentication;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                return error("用户名或密码错误");
            }
            return error(StringUtils.isEmpty(e.getMessage()) ? "登录失败" : e.getMessage());
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        boolean managerSide = hasManagerRole(loginUser);
        if (!managerSide)
        {
            MajorProfile majorProfile = resolveMajorProfile(loginBody.getMajorKey(), loginBody.getGradeNo(), loginBody.getClassNo());
            if (majorProfile == null)
            {
                return error("请选择专业，目前仅支持数据科学与大数据、网络工程");
            }
            if (!validateMajorOnLogin(loginUser.getUserId(), majorProfile))
            {
                return error("所选专业与账号不匹配，请确认后重试");
            }
        }

        loginService.recordLoginInfo(loginUser.getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, tokenService.createToken(loginUser));
        return ajax;
    }

    @PostMapping("/student/performance/anonymous/education/register")
    public AjaxResult register(@RequestBody EduRegisterBody registerBody)
    {
        String msg = registerPadUser(registerBody);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
