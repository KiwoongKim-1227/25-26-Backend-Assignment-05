package com.example.oauthgoogleloginexample.dto.issue;

import com.example.oauthgoogleloginexample.domain.IssueEnums.Priority;
import com.example.oauthgoogleloginexample.domain.IssueEnums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IssueUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.") String title,
        String description,
        @NotNull Status status,
        @NotNull Priority priority
) {
}
