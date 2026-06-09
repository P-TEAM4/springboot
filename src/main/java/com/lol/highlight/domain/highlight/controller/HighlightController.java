package com.lol.highlight.domain.highlight.controller;

import com.lol.highlight.domain.highlight.dto.HighlightCreateRequest;
import com.lol.highlight.domain.highlight.dto.HighlightResponse;
import com.lol.highlight.domain.highlight.service.HighlightService;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.global.auth.annotation.AuthUser;
import com.lol.highlight.global.common.ApiResponse;
import com.lol.highlight.global.common.annotation.ApiErrorExamples;
import com.lol.highlight.global.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Highlight", description = "하이라이트 관리 API")
@RestController
@RequestMapping("/api/highlights")
@RequiredArgsConstructor
public class HighlightController {

    private final HighlightService highlightService;

    @Operation(summary = "하이라이트 정보 조회", description = "하이라이트 ID로 특정 하이라이트의 상세 정보를 조회합니다.")
    @ApiErrorExamples({
            ErrorCode.HIGHLIGHT_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/{id}")
    public ApiResponse<HighlightResponse> getHighlight(
            @Parameter(description = "하이라이트 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(highlightService.getHighlightById(id));
    }

    @Operation(summary = "매치의 하이라이트 목록 조회", description = "특정 매치에 속한 모든 하이라이트를 페이징하여 조회합니다.")
    @ApiErrorExamples({
            ErrorCode.MATCH_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/match/{matchId}")
    public ApiResponse<Page<HighlightResponse>> getMatchHighlights(
            @Parameter(description = "Riot 매치 ID (예: KR_8031304127)", required = true) @PathVariable String matchId,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
        return ApiResponse.success(highlightService.getMatchHighlights(matchId, pageable));
    }

    @Operation(summary = "플레이어의 하이라이트 목록 조회", description = "특정 플레이어(puuid)의 모든 하이라이트를 페이징하여 조회합니다.")
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/player/{puuid}")
    public ApiResponse<Page<HighlightResponse>> getPlayerHighlights(
            @Parameter(description = "플레이어 PUUID", required = true) @PathVariable String puuid,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
        return ApiResponse.success(highlightService.getHighlightsByPuuid(puuid, pageable));
    }

    @Operation(summary = "하이라이트 생성",
            description = "게임 영상을 업로드하면 AI가 자동으로 하이라이트/실수 클립을 추출합니다. (비동기 처리)")
    @ApiErrorExamples({
            ErrorCode.MATCH_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<HighlightResponse> createHighlight(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestPart("video") MultipartFile video,
            @RequestPart("request") @Valid HighlightCreateRequest request) {
        return ApiResponse.success(highlightService.createHighlight(request, video, user));
    }

    @Operation(summary = "AI 자동 하이라이트 생성", description = "매치 ID를 기반으로 AI가 자동으로 하이라이트를 추출합니다. (비동기 처리)")
    @ApiErrorExamples({
            ErrorCode.MATCH_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping("/match/{matchId}/auto-generate")
    public ApiResponse<Void> autoGenerateHighlights(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "Riot 매치 ID", required = true) @PathVariable String matchId) {
        highlightService.autoGenerateHighlights(matchId, user);
        return ApiResponse.success();
    }

    @Operation(summary = "하이라이트 조회수 증가", description = "하이라이트의 조회수를 1 증가시킵니다.")
    @ApiErrorExamples({
            ErrorCode.HIGHLIGHT_NOT_FOUND
    })
    @PostMapping("/{id}/view")
    public ApiResponse<HighlightResponse> incrementViewCount(
            @Parameter(description = "하이라이트 ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(highlightService.incrementViewCount(id));
    }

    @Operation(summary = "하이라이트 삭제", description = "특정 하이라이트를 삭제합니다.")
    @ApiErrorExamples({
            ErrorCode.HIGHLIGHT_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteHighlight(
            @Parameter(description = "하이라이트 ID", required = true) @PathVariable Long id) {
        highlightService.deleteHighlight(id);
        return ApiResponse.success();
    }
}
