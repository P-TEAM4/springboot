package com.lol.highlight.domain.match.controller;

import com.lol.highlight.domain.match.dto.MatchImportRequest;
import com.lol.highlight.domain.match.dto.MatchListResponse;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.service.MatchService;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.config.SwaggerConfig.ApiErrorExamples;
import com.lol.highlight.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Match", description = "매치 관리 API")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(
            summary = "매치 정보 조회",
            description = "매치 ID로 특정 매치의 상세 정보를 조회합니다."
    )
    @ApiErrorExamples({ErrorCode.MATCH_NOT_FOUND, ErrorCode.UNAUTHORIZED})
    @GetMapping("/{id}")
    public ApiResponse<MatchResponse> getMatch(
            @Parameter(description = "매치 ID", required = true) @PathVariable Long id) {
        MatchResponse match = matchService.getMatchById(id);
        return ApiResponse.success("매치 정보 조회 성공", match);
    }

    @Operation(
            summary = "사용자의 매치 목록 조회 (페이징)",
            description = "특정 사용자의 전체 매치 목록을 페이징하여 조회합니다."
    )
    @ApiErrorExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.UNAUTHORIZED})
    @GetMapping("/user/{userId}")
    public ApiResponse<Page<MatchResponse>> getUserMatches(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
        Page<MatchResponse> matches = matchService.getUserMatches(userId, pageable);
        return ApiResponse.success(matches);
    }

    @Operation(
            summary = "사용자의 최근 매치 조회",
            description = "특정 사용자의 최근 매치를 지정된 개수만큼 조회합니다."
    )
    @ApiErrorExamples({ErrorCode.USER_NOT_FOUND, ErrorCode.RIOT_API_ERROR, ErrorCode.UNAUTHORIZED})
    @GetMapping("/user/{userId}/recent")
    public ApiResponse<List<MatchListResponse>> getRecentMatches(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "조회할 매치 개수 (기본값: 20)") @RequestParam(defaultValue = "20") int count) {
        List<MatchListResponse> matches = matchService.getRecentMatches(userId, count);
        return ApiResponse.success(matches);
    }

    // import/sync 기능 제거: 캐시를 사용하므로 불필요
    // 분석/하이라이트 생성 시 자동으로 DB에 저장됨

    @Operation(
            summary = "매치 삭제",
            description = "특정 매치를 삭제합니다."
    )
    @ApiErrorExamples({ErrorCode.MATCH_NOT_FOUND, ErrorCode.UNAUTHORIZED})
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMatch(
            @Parameter(description = "매치 ID", required = true) @PathVariable Long id) {
        matchService.deleteMatch(id);
        return ApiResponse.success();
    }
}
