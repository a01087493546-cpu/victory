package com.victory.controller;

import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
import com.victory.service.FeedbackAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackAiController {

    private final FeedbackAiService feedbackAiService;

    @PostMapping("/ai-review")
    public AiFeedbackResponse getAiReview(@RequestBody AiFeedbackRequest request) {
        return feedbackAiService.getFeedback(request);
    }
}
