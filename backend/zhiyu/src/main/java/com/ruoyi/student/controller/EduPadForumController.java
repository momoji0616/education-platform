package com.ruoyi.student.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.domain.EduForumPost;
import com.ruoyi.student.domain.EduForumReply;

@RestController
@RequestMapping("/education/pad")
public class EduPadForumController extends EduPadSupport
{
    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @GetMapping("/forum/posts")
    public AjaxResult listForumPosts()
    {
        String role = resolveCurrentForumRole();
        String className = getCurrentClassName();
        List<Map<String, Object>> posts = ROLE_MANAGER.equals(role) ? eduPadMapper.selectForumPostsAll() : eduPadMapper.selectForumPostsByRole(role, className);
        if (posts == null || posts.isEmpty()) return emptyListSuccess();

        List<Long> postIds = posts.stream().map(item -> Long.valueOf(String.valueOf(item.get(POST_ID_KEY)))).collect(Collectors.toList());
        List<Map<String, Object>> replies = eduPadMapper.selectForumRepliesByPostIds(postIds);
        Map<Long, List<Map<String, Object>>> replyMap = new HashMap<>();
        if (replies != null)
        {
            for (Map<String, Object> reply : replies)
            {
                Long postId = Long.valueOf(String.valueOf(reply.get(POST_ID_KEY)));
                replyMap.computeIfAbsent(postId, key -> new ArrayList<>()).add(reply);
            }
        }
        for (Map<String, Object> post : posts)
        {
            Long postId = Long.valueOf(String.valueOf(post.get(POST_ID_KEY)));
            post.put("replies", replyMap.getOrDefault(postId, new ArrayList<>()));
        }
        return success(posts);
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @PostMapping("/forum/post")
    public AjaxResult createForumPost(@RequestBody EduForumPost post)
    {
        if (post == null || post.getTitle() == null || post.getTitle().trim().isEmpty()) return error("标题不能为空");
        if (post.getContent() == null || post.getContent().trim().isEmpty()) return error("内容不能为空");
        String role = resolveCurrentForumRole();
        String targetRole = normalizeTargetRole(post.getTargetRole(), role);
        if (targetRole == null) return error("目标角色无效");
        post.setAuthorId(currentUserId());
        post.setAuthorName(currentUserNickName());
        post.setAuthorRole(role);
        post.setTargetRole(targetRole);
        if (!ROLE_MANAGER.equals(role))
        {
            String className = getCurrentClassName();
            if (StringUtils.isEmpty(className)) return error("未绑定班级，无法发布帖子");
            post.setClassName(className);
        }
        return toAjax(eduPadMapper.insertForumPost(post));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @PostMapping("/forum/{postId}/reply")
    public AjaxResult replyForumPost(@PathVariable Long postId, @RequestBody EduForumReply reply)
    {
        if (reply == null || reply.getContent() == null || reply.getContent().trim().isEmpty()) return error("回复内容不能为空");
        reply.setPostId(postId);
        reply.setAuthorId(currentUserId());
        reply.setAuthorName(currentUserNickName());
        reply.setAuthorRole(resolveCurrentForumRole());
        return toAjax(eduPadMapper.insertForumReply(reply));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @GetMapping("/forum/notice")
    public AjaxResult getForumNotice()
    {
        Long userId = currentUserId();
        String role = resolveCurrentForumRole();
        Date readTime = eduPadMapper.selectForumLastReadTime(userId);
        if (readTime == null) readTime = new Date(0L);
        int unreadPosts;
        int unreadReplies;
        if (ROLE_MANAGER.equals(role))
        {
            unreadPosts = eduPadMapper.countUnreadForumPostsAll(readTime, userId);
            unreadReplies = eduPadMapper.countUnreadForumRepliesAll(readTime, userId);
        }
        else
        {
            String className = getCurrentClassName();
            unreadPosts = eduPadMapper.countUnreadForumPostsByRole(role, className, readTime, userId);
            unreadReplies = eduPadMapper.countUnreadForumRepliesByRole(role, className, readTime, userId);
        }
        AjaxResult result = AjaxResult.success();
        result.put("unreadPosts", unreadPosts);
        result.put("unreadReplies", unreadReplies);
        result.put("unreadTotal", unreadPosts + unreadReplies);
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student')")
    @PostMapping("/forum/notice/read")
    public AjaxResult markForumRead()
    {
        Long userId = currentUserId();
        Date now = new Date();
        Date existing = eduPadMapper.selectForumLastReadTime(userId);
        if (existing == null) eduPadMapper.insertForumReadState(userId, now);
        else eduPadMapper.updateForumReadState(userId, now);
        return success();
    }
}
