package com.ruoyi.student.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.domain.EduForumPost;

@RestController
@RequestMapping("/education/pad")
public class EduPadChatController extends EduPadSupport
{
    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @GetMapping("/chat/contacts")
    public AjaxResult listChatContacts()
    {
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return emptyListSuccess();
        String currentRole = resolveCurrentForumRole();
        String peerRole = ROLE_TEACHER.equals(currentRole) ? ROLE_KEY_STUDENT : ROLE_KEY_TEACHER;
        return success(eduPadMapper.selectClassChatContacts(className, currentUserId(), peerRole));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @GetMapping("/chat/messages")
    public AjaxResult listChatMessages(Long peerUserId)
    {
        if (peerUserId == null || peerUserId <= 0) return error("聊天对象不能为空");
        if (!canChatWith(peerUserId)) return error("只能与本班允许联系的对象聊天");
        String className = getCurrentClassName();
        Long currentUserId = currentUserId();
        return success(eduPadMapper.selectChatMessages(className, currentUserId, peerUserId, String.valueOf(currentUserId), String.valueOf(peerUserId)));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @GetMapping("/chat/groups")
    public AjaxResult listChatGroups()
    {
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return emptyListSuccess();
        List<Map<String, Object>> groups = new ArrayList<>();
        Map<String, Object> classGroup = new HashMap<>();
        classGroup.put("groupId", DEFAULT_CLASS_GROUP_ID);
        classGroup.put("groupName", className + " 群聊");
        classGroup.put("className", className);
        groups.add(classGroup);
        return success(groups);
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @GetMapping("/chat/group/messages")
    public AjaxResult listGroupChatMessages(String groupId)
    {
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return emptyListSuccess();
        if (StringUtils.isEmpty(groupId) || !DEFAULT_CLASS_GROUP_ID.equals(groupId)) return error("群聊不存在或无权访问");
        return success(eduPadMapper.selectGroupChatMessages(className, groupId));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @PostMapping("/chat/send")
    public AjaxResult sendChatMessage(@RequestBody Map<String, Object> body)
    {
        Long peerUserId = toLong(body == null ? null : body.get("peerUserId"));
        String content = body == null ? "" : String.valueOf(body.getOrDefault("content", "")).trim();
        if (peerUserId == null || peerUserId <= 0) return error("聊天对象不能为空");
        if (StringUtils.isEmpty(content)) return error("消息内容不能为空");
        if (!canChatWith(peerUserId)) return error("只能与本班允许联系的对象聊天");
        EduForumPost msg = new EduForumPost();
        msg.setTitle(CHAT_DM_TITLE);
        msg.setContent(content);
        msg.setAuthorId(currentUserId());
        msg.setAuthorName(currentUserNickName());
        msg.setAuthorRole(resolveCurrentForumRole());
        msg.setTargetRole(String.valueOf(peerUserId));
        msg.setClassName(getCurrentClassName());
        return toAjax(eduPadMapper.insertForumPost(msg));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @PostMapping("/chat/group/send")
    public AjaxResult sendGroupChatMessage(@RequestBody Map<String, Object> body)
    {
        String groupId = body == null ? "" : String.valueOf(body.getOrDefault("groupId", "")).trim();
        String content = body == null ? "" : String.valueOf(body.getOrDefault("content", "")).trim();
        if (StringUtils.isEmpty(groupId) || !DEFAULT_CLASS_GROUP_ID.equals(groupId)) return error("群聊不存在或无权发送");
        if (StringUtils.isEmpty(content)) return error("消息内容不能为空");
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return error("未绑定班级，无法发送群消息");
        EduForumPost msg = new EduForumPost();
        msg.setTitle(CHAT_GROUP_TITLE);
        msg.setContent(content);
        msg.setAuthorId(currentUserId());
        msg.setAuthorName(currentUserNickName());
        msg.setAuthorRole(resolveCurrentForumRole());
        msg.setTargetRole(groupId);
        msg.setClassName(className);
        return toAjax(eduPadMapper.insertForumPost(msg));
    }
}
