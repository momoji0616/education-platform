package com.ruoyi.student.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.security.context.AuthenticationContextHolder;
import com.ruoyi.student.domain.EduLoginBody;
import com.ruoyi.student.domain.EduRegisterBody;
import com.ruoyi.student.domain.EduTeacherTask;

@RestController
@RequestMapping("/education/pad")
public class EduPadController extends EduPadSupport
{
    @PostMapping("/auth/login")
    public AjaxResult padAuthLogin(@RequestBody EduLoginBody loginBody)
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
        if (!hasManagerRole(loginUser))
        {
            MajorProfile majorProfile = resolveMajorProfile(loginBody.getMajorKey(), loginBody.getGradeNo(), loginBody.getClassNo());
            if (majorProfile == null)
            {
                return error("请选择专业");
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

    @PostMapping("/auth/register")
    public AjaxResult padAuthRegister(@RequestBody EduRegisterBody registerBody)
    {
        String msg = registerPadUser(registerBody);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    @PreAuthorize("@ss.hasRole('admin') or @ss.hasRole('manager')")
    @GetMapping("/manager/scores")
    public AjaxResult listManagerAllScores()
    {
        AjaxResult result = AjaxResult.success();
        result.put("examScores", eduPadMapper.selectExamScoreAll());
        result.put("performanceScores", eduPadMapper.selectStudentPerformanceAll());
        return result;
    }

    @PreAuthorize("@ss.hasRole('admin') or @ss.hasRole('manager')")
    @GetMapping("/manager/overview")
    public AjaxResult managerOverview()
    {
        List<Map<String, Object>> majors = eduPadMapper.selectAdminMajorOverview();
        for (Map<String, Object> major : majors)
        {
            String className = String.valueOf(major.getOrDefault("className", ""));
            major.put("teachers", eduPadMapper.selectAdminMajorUsers(className, ROLE_KEY_TEACHER));
            major.put("students", eduPadMapper.selectAdminMajorUsers(className, ROLE_KEY_STUDENT));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("majors", majors);
        data.put("majorCount", majors.size());
        return success(data);
    }

    @PreAuthorize("@ss.hasRole('admin') or @ss.hasRole('manager')")
    @PostMapping("/manager/teacher-task")
    public AjaxResult createTeacherTask(@RequestBody EduTeacherTask task)
    {
        if (task.getStatus() == null)
        {
            task.setStatus(STATUS_TODO);
        }
        return toAjax(eduPadMapper.insertTeacherTask(task));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('admin') or @ss.hasRole('manager')")
    @PostMapping("/review/ai-suggest")
    public AjaxResult aiSuggestReview(@RequestBody Map<String, Object> body)
    {
        String targetAnswer = body == null ? "" : String.valueOf(body.getOrDefault("targetAnswer", "")).trim();
        if (StringUtils.isEmpty(targetAnswer))
        {
            return error("待批改答案不能为空");
        }
        String exampleAnswer = body == null ? "" : String.valueOf(body.getOrDefault("exampleAnswer", "")).trim();
        String exampleFeedback = body == null ? "" : String.valueOf(body.getOrDefault("exampleFeedback", "")).trim();
        int exampleScore = toInt(body == null ? null : body.get("exampleScore"), DEFAULT_EXAMPLE_SCORE);
        int maxScore = toInt(body == null ? null : body.get("maxScore"), DEFAULT_EXAM_MAX_SCORE);
        if (maxScore <= 0)
        {
            maxScore = DEFAULT_EXAM_MAX_SCORE;
        }
        if (exampleScore < 0)
        {
            exampleScore = 0;
        }
        if (exampleScore > maxScore)
        {
            exampleScore = maxScore;
        }

        double similarity = computeTextSimilarity(exampleAnswer, targetAnswer);
        double ratio = 0.55 + 0.45 * similarity;
        int suggestedScore = StringUtils.isEmpty(exampleAnswer)
                ? Math.min(maxScore, Math.max(MIN_AI_FALLBACK_SCORE, targetAnswer.length() / 8))
                : (int) Math.round(exampleScore * ratio);
        suggestedScore = Math.max(0, Math.min(maxScore, suggestedScore));
        String level = suggestedScore >= (int) (maxScore * 0.9) ? "优秀"
                : suggestedScore >= (int) (maxScore * 0.75) ? "良好"
                : suggestedScore >= (int) (maxScore * 0.6) ? "合格" : "需改进";
        String feedback = StringUtils.isEmpty(exampleFeedback)
                ? "AI建议：" + level + "。建议完善答题步骤、关键依据与最终结论。"
                : "AI参考批语（可人工修改）：" + exampleFeedback + "；综合判定：" + level + "。";
        AjaxResult result = AjaxResult.success();
        result.put("suggestedScore", suggestedScore);
        result.put("suggestedFeedback", feedback);
        result.put("similarity", Math.round(similarity * 100.0) / 100.0);
        return result;
    }
}
