package com.zhihu.platform.controller;

import com.zhihu.platform.common.ApiResponse;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.service.QaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qa")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/{questionId}/answer")
    public ApiResponse<Long> answer(@PathVariable Long questionId,
                                    @Valid @RequestBody Phase2Dtos.CreateAnswerRequest request) {
        return ApiResponse.ok(qaService.addAnswer(questionId, request));
    }

    @GetMapping("/{questionId}/answers")
    public ApiResponse<List<Phase2Dtos.AnswerItem>> answers(@PathVariable Long questionId) {
        return ApiResponse.ok(qaService.answers(questionId));
    }

    @PostMapping("/{questionId}/best")
    public ApiResponse<Void> pickBest(@PathVariable Long questionId,
                                      @Valid @RequestBody Phase2Dtos.PickBestAnswerRequest request) {
        qaService.pickBestAnswer(questionId, request.getAnswerId());
        return ApiResponse.ok();
    }

    @PostMapping("/{questionId}/reopen")
    public ApiResponse<Void> reopen(@PathVariable Long questionId) {
        qaService.reopen(questionId);
        return ApiResponse.ok();
    }

    @GetMapping("/{questionId}/state")
    public ApiResponse<Phase2Dtos.QuestionStateResponse> state(@PathVariable Long questionId) {
        return ApiResponse.ok(qaService.state(questionId));
    }

    @GetMapping("/questions")
    public ApiResponse<List<Phase2Dtos.QuestionListItem>> questions(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(qaService.questions(keyword));
    }
}
