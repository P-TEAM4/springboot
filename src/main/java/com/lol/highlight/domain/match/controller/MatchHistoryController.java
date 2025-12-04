package com.lol.highlight.domain.match.controller;

import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.service.MatchHistoryService;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.config.SwaggerConfig.ApiErrorExamples;
import com.lol.highlight.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Match History", description = "전적 조회 및 갱신 API")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchHistoryController {

    private final MatchHistoryService matchHistoryService;

    @Operation(summary = "사용자 전적 목록 조회", description = "사용자의 전적 목록을 페이징하여 조회합니다. 마지막 활동 시간을 업데이트합니다.")
    @GetMapping("/user/{userId}")
    public ApiResponse<Page<MatchResponse>> getUserMatches(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            Pageable pageable) {
        Page<MatchResponse> matches = matchHistoryService.getUserMatches(userId, pageable);
        return ApiResponse.success(matches);
    }

    @Operation(summary = "매치 상세 정보 조회", description = "S3에서 매치 상세 데이터를 가져와 반환합니다.")
    @ApiErrorExamples({
            ErrorCode.MATCH_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/{matchId}/detail")
    public ApiResponse<MatchDetailResponse> getMatchDetail(
            @Parameter(description = "매치 ID") @PathVariable Long matchId) {
        MatchDetailResponse detail = matchHistoryService.getMatchDetail(matchId);
        return ApiResponse.success(detail);
    }

    @Operation(summary = "전적 갱신", description = "Riot API에서 최신 전적을 가져와 갱신합니다. 3분에 한번만 가능합니다. 소환사 정보도 함께 갱신됩니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.EXTERNAL_API_ERROR,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping("/user/{userId}/refresh")
    public ApiResponse<List<MatchResponse>> refreshMatches(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        List<MatchResponse> matches = matchHistoryService.refreshMatches(userId);
        return ApiResponse.success(matches, "전적이 성공적으로 갱신되었습니다");
    }
}
