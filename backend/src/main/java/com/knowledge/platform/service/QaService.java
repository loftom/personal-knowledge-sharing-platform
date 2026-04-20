package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.QaAnswer;
import com.knowledge.platform.domain.entity.QaQuestion;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.QaAnswerMapper;
import com.knowledge.platform.domain.mapper.QaQuestionMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.realtime.RealtimePushService;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class QaService {

    private final QaQuestionMapper qaQuestionMapper;
    private final QaAnswerMapper qaAnswerMapper;
    private final ContentMapper contentMapper;
    private final FollowRelationMapper followRelationMapper;
    private final UserMapper userMapper;
    private final BehaviorService behaviorService;
    private final NotificationService notificationService;
    private final PointService pointService;
    private final RealtimePushService realtimePushService;

    public QaService(QaQuestionMapper qaQuestionMapper,
                     QaAnswerMapper qaAnswerMapper,
                     ContentMapper contentMapper,
                     FollowRelationMapper followRelationMapper,
                     UserMapper userMapper,
                     BehaviorService behaviorService,
                     NotificationService notificationService,
                     PointService pointService,
                     RealtimePushService realtimePushService) {
        this.qaQuestionMapper = qaQuestionMapper;
        this.qaAnswerMapper = qaAnswerMapper;
        this.contentMapper = contentMapper;
        this.followRelationMapper = followRelationMapper;
        this.userMapper = userMapper;
        this.behaviorService = behaviorService;
        this.notificationService = notificationService;
        this.pointService = pointService;
        this.realtimePushService = realtimePushService;
    }

    public void initQuestionIfNeeded(Content content) {
        if (content == null || !"QUESTION".equalsIgnoreCase(content.getType())) {
            return;
        }
        QaQuestion exists = qaQuestionMapper.selectOne(new LambdaQueryWrapper<QaQuestion>()
                .eq(QaQuestion::getQuestionId, content.getId()));
        if (exists != null) {
            return;
        }
        QaQuestion question = new QaQuestion();
        question.setQuestionId(content.getId());
        question.setStatus("PENDING");
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        qaQuestionMapper.insert(question);
    }

    @Transactional
    public Long addAnswer(Long questionId, Phase2Dtos.CreateAnswerRequest request) {
        Content question = assertQuestion(questionId, UserContext.getUserId(), true);
        QaAnswer answer = new QaAnswer();
        answer.setQuestionId(questionId);
        answer.setUserId(UserContext.getUserId());
        answer.setBody(request.getBody());
        answer.setLikeCount(0L);
        answer.setIsBest(0);
        answer.setStatus(1);
        answer.setCreatedAt(LocalDateTime.now());
        answer.setUpdatedAt(LocalDateTime.now());
        qaAnswerMapper.insert(answer);
        behaviorService.record(UserContext.getUserId(), "ANSWER", "CONTENT", questionId, 4);

        QaQuestion meta = getOrInitMeta(questionId);
        if (!"PENDING".equals(meta.getStatus())) {
            meta.setStatus("PENDING");
            meta.setBestAnswerId(null);
            meta.setResolvedAt(null);
            meta.setUpdatedAt(LocalDateTime.now());
            qaQuestionMapper.updateById(meta);
        }

        realtimePushService.pushAfterCommit(buildUserTargets(question.getAuthorId(), answer.getUserId()), "qa-answer", Map.of(
                "questionId", questionId,
                "answerId", answer.getId()
        ));
        return answer.getId();
    }

    @Transactional
    public void pickBestAnswer(Long questionId, Long answerId) {
        Content question = assertQuestion(questionId, UserContext.getUserId(), true);
        if (!question.getAuthorId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new AppException("No permission to mark best answer");
        }
        QaAnswer answer = qaAnswerMapper.selectById(answerId);
        if (answer == null || !questionId.equals(answer.getQuestionId()) || answer.getStatus() == null || answer.getStatus() != 1) {
            throw new AppException("Answer not found");
        }

        qaAnswerMapper.update(new LambdaUpdateWrapper<QaAnswer>()
                .set(QaAnswer::getIsBest, 0)
                .eq(QaAnswer::getQuestionId, questionId));

        answer.setIsBest(1);
        answer.setUpdatedAt(LocalDateTime.now());
        qaAnswerMapper.updateById(answer);

        QaQuestion meta = getOrInitMeta(questionId);
        meta.setStatus("RESOLVED");
        meta.setBestAnswerId(answerId);
        meta.setResolvedAt(LocalDateTime.now());
        meta.setUpdatedAt(LocalDateTime.now());
        qaQuestionMapper.updateById(meta);

        notificationService.notifyBestAnswer(answer, question);
        pointService.rewardBestAnswer(answer.getUserId(), answer.getId());
        realtimePushService.pushAfterCommit(buildUserTargets(question.getAuthorId(), answer.getUserId()), "qa-state-changed", Map.of(
            "questionId", questionId,
            "status", "RESOLVED",
            "bestAnswerId", answerId
        ));
    }

    public void reopen(Long questionId) {
        Content question = assertQuestion(questionId, UserContext.getUserId(), true);
        if (!question.getAuthorId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new AppException("No permission to reopen question");
        }
        QaQuestion meta = getOrInitMeta(questionId);
        meta.setStatus("PENDING");
        meta.setBestAnswerId(null);
        meta.setResolvedAt(null);
        meta.setUpdatedAt(LocalDateTime.now());
        qaQuestionMapper.updateById(meta);
        qaAnswerMapper.update(new LambdaUpdateWrapper<QaAnswer>()
                .set(QaAnswer::getIsBest, 0)
                .eq(QaAnswer::getQuestionId, questionId));
        realtimePushService.pushAfterCommit(buildUserTargets(question.getAuthorId(), UserContext.getUserId()), "qa-state-changed", Map.of(
            "questionId", questionId,
            "status", "PENDING",
            "bestAnswerId", 0
        ));
    }

        private List<Long> buildUserTargets(Long... userIds) {
        return java.util.Arrays.stream(userIds)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        }

    public Phase2Dtos.QuestionStateResponse state(Long questionId) {
        assertQuestion(questionId, UserContext.getUserId(), false);
        QaQuestion meta = getOrInitMeta(questionId);
        Phase2Dtos.QuestionStateResponse response = new Phase2Dtos.QuestionStateResponse();
        response.setQuestionId(questionId);
        response.setStatus(meta.getStatus());
        response.setBestAnswerId(meta.getBestAnswerId());
        response.setResolvedAt(meta.getResolvedAt());
        return response;
    }

    public List<Phase2Dtos.AnswerItem> answers(Long questionId) {
        assertQuestion(questionId, UserContext.getUserId(), false);
        List<QaAnswer> answers = qaAnswerMapper.selectList(new LambdaQueryWrapper<QaAnswer>()
                        .eq(QaAnswer::getQuestionId, questionId)
                        .eq(QaAnswer::getStatus, 1)
                        .orderByDesc(QaAnswer::getIsBest)
                        .orderByAsc(QaAnswer::getCreatedAt));
        Map<Long, User> userMap = loadUserMap(answers.stream().map(QaAnswer::getUserId).toList());
        return answers.stream()
                .map(answer -> toAnswerItem(answer, userMap.get(answer.getUserId())))
                .collect(Collectors.toList());
    }

    public List<Phase2Dtos.QuestionListItem> questions(String keyword) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<Content>()
                .eq(Content::getType, "QUESTION")
                .eq(Content::getStatus, "PUBLISHED")
                .orderByDesc(Content::getPublishedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Content::getTitle, keyword).or().like(Content::getBody, keyword));
        }
        List<Content> questions = contentMapper.selectList(wrapper);
        Long currentUserId = UserContext.getUserId();
        questions = questions.stream()
                .filter(content -> canAccessQuestion(content, currentUserId))
                .toList();
        Map<Long, User> userMap = loadUserMap(questions.stream().map(Content::getAuthorId).toList());
        return questions.stream()
                .map(content -> toQuestionListItem(content, userMap.get(content.getAuthorId())))
                .collect(Collectors.toList());
    }

    private Phase2Dtos.AnswerItem toAnswerItem(QaAnswer answer, User user) {
        Phase2Dtos.AnswerItem item = new Phase2Dtos.AnswerItem();
        item.setId(answer.getId());
        item.setQuestionId(answer.getQuestionId());
        item.setUserId(answer.getUserId());
        if (user != null) {
            item.setUsername(user.getUsername());
            item.setNickname(user.getNickname());
            item.setDisplayName(resolveDisplayName(user.getNickname(), user.getUsername(), user.getId()));
        }
        item.setBody(answer.getBody());
        item.setLikeCount(answer.getLikeCount());
        item.setIsBest(answer.getIsBest());
        item.setCreatedAt(answer.getCreatedAt());
        return item;
    }

    private Phase2Dtos.QuestionListItem toQuestionListItem(Content content, User author) {
        QaQuestion meta = getOrInitMeta(content.getId());
        long answerCount = qaAnswerMapper.selectCount(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, content.getId())
                .eq(QaAnswer::getStatus, 1));
        Phase2Dtos.QuestionListItem item = new Phase2Dtos.QuestionListItem();
        item.setId(content.getId());
        item.setTitle(content.getTitle());
        item.setSummary(content.getSummary());
        item.setAuthorId(content.getAuthorId());
        item.setAuthorName(author == null ? "用户 " + content.getAuthorId() : resolveDisplayName(author.getNickname(), author.getUsername(), author.getId()));
        item.setAnswerCount(answerCount);
        item.setStatus(meta.getStatus());
        item.setBestAnswerId(meta.getBestAnswerId());
        item.setViewCount(content.getViewCount());
        item.setCreatedAt(content.getCreatedAt());
        return item;
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        List<Long> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return new HashMap<>();
        }
        return userMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private String resolveDisplayName(String nickname, String username, Long userId) {
        if (nickname != null) {
            String trimmed = nickname.trim();
            if (!trimmed.isEmpty() && !trimmed.matches("[?？]+")) {
                return trimmed;
            }
        }
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "用户 " + userId;
    }

    private QaQuestion getOrInitMeta(Long questionId) {
        QaQuestion meta = qaQuestionMapper.selectOne(new LambdaQueryWrapper<QaQuestion>()
                .eq(QaQuestion::getQuestionId, questionId));
        if (meta != null) {
            return meta;
        }
        QaQuestion question = new QaQuestion();
        question.setQuestionId(questionId);
        question.setStatus("PENDING");
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        qaQuestionMapper.insert(question);
        return question;
    }

    private Content assertQuestion(Long questionId, Long viewerUserId, boolean allowOwnerOnUnpublished) {
        Content question = contentMapper.selectById(questionId);
        if (question == null || !"QUESTION".equalsIgnoreCase(question.getType())) {
            throw new AppException("Question not found");
        }
        if (!canAccessQuestion(question, viewerUserId, allowOwnerOnUnpublished)) {
            throw new AppException("Question is not visible");
        }
        return question;
    }

    private boolean canAccessQuestion(Content question, Long viewerUserId) {
        return canAccessQuestion(question, viewerUserId, false);
    }

    private boolean canAccessQuestion(Content question, Long viewerUserId, boolean allowOwnerOnUnpublished) {
        if (question == null) {
            return false;
        }
        if (!"PUBLISHED".equals(question.getStatus())) {
            return allowOwnerOnUnpublished && viewerUserId != null && viewerUserId.equals(question.getAuthorId());
        }
        String visibility = question.getVisibility();
        if (visibility == null || visibility.isBlank() || "PUBLIC".equalsIgnoreCase(visibility)) {
            return true;
        }
        if (viewerUserId == null) {
            return false;
        }
        if (viewerUserId.equals(question.getAuthorId()) || UserContext.isAdmin()) {
            return true;
        }
        if ("PRIVATE".equalsIgnoreCase(visibility)) {
            return false;
        }
        if ("FOLLOWERS".equalsIgnoreCase(visibility)) {
            return followRelationMapper.selectCount(new LambdaQueryWrapper<FollowRelation>()
                    .eq(FollowRelation::getFollowerUserId, viewerUserId)
                    .eq(FollowRelation::getTargetUserId, question.getAuthorId())
                    .eq(FollowRelation::getStatus, 1)) > 0;
        }
        return false;
    }
}
