package com.lol.highlight.domain.match.controller;

import com.lol.highlight.domain.match.dto.MatchDetailResponse;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.service.MatchService;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.common.annotation.ApiErrorExamples;
import com.lol.highlight.global.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Match", description = "매치 관리 API")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "소환사 전적 조회", description = "롤 소환사의 전적을 조회합니다. DB에 없으면 Riot API에서 가져옵니다. Rate Limit이 적용됩니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.RATE_LIMIT_EXCEEDED,
            ErrorCode.EXTERNAL_API_ERROR,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/summoner/{gameName}/{tagLine}")
    public ApiResponse<Page<MatchResponse>> getMatches(
            @Parameter(description = "소환사 이름", required = true) @PathVariable String gameName,
            @Parameter(description = "태그 라인", required = true) @PathVariable String tagLine,
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {

        Long requestUserId = Long.parseLong(userDetails.getUsername());

        Page<MatchResponse> matches = matchService.getMatchesBySummonerName(
                requestUserId,
                gameName,
                tagLine,
                pageable
        );

        return ApiResponse.success(matches);
    }

    @Operation(summary = "전적 강제 갱신", description = "명시적으로 Riot API에서 최신 전적을 가져옵니다. Rate Limit이 적용됩니다.")
    @ApiErrorExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.RATE_LIMIT_EXCEEDED,
            ErrorCode.EXTERNAL_API_ERROR,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping("/summoner/{gameName}/{tagLine}/refresh")
    public ApiResponse<Void> refreshMatches(
            @Parameter(description = "소환사 이름", required = true) @PathVariable String gameName,
            @Parameter(description = "태그 라인", required = true) @PathVariable String tagLine,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long requestUserId = Long.parseLong(userDetails.getUsername());

        matchService.forceRefreshMatches(requestUserId, gameName, tagLine);

        return ApiResponse.success("전적이 성공적으로 갱신되었습니다");
    }

    @Operation(summary = "매치 상세 정보 조회", description = "Cloud Storage에서 매치 상세 데이터를 가져와 반환합니다.")
    @ApiErrorExamples({
            ErrorCode.MATCH_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/{matchId}/detail")
    public ApiResponse<MatchDetailResponse> getMatchDetail(
            @Parameter(description = "매치 ID (예: KR_7951942780)", required = true) @PathVariable String matchId) {
        MatchDetailResponse detail = matchService.getMatchDetail(matchId);
        return ApiResponse.success(detail);
    }

}
