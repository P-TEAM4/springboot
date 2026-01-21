package com.lol.highlight.domain.analysis.controller;

import com.lol.highlight.domain.analysis.dto.AnalysisCreateRequest;
import com.lol.highlight.domain.analysis.dto.AnalysisResponse;
import com.lol.highlight.domain.analysis.service.AnalysisService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Analysis", description = "경기 분석 API")
@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "분석 정보 조회", description = "분석 ID로 특정 분석의 상세 정보를 조회합니다.")
    @ApiErrorExamples({
            ErrorCode.ANALYSIS_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/{id}")
    public ApiResponse<AnalysisResponse> getAnalysis(
            @Parameter(description = "분석 ID", required = true) @PathVariable Long id) {
        AnalysisResponse analysis = analysisService.getAnalysisById(id);
        return ApiResponse.success(analysis);
    }

    @Operation(summary = "매치의 분석 조회", description = "특정 매치에 대한 분석 정보를 조회합니다. 분석이 없으면 자동으로 생성하고 AI 분석을 요청합니다.")
    @ApiErrorExamples({
            ErrorCode.MATCH_NOT_FOUND,
            ErrorCode.ANALYSIS_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/match/{matchId}")
    public ApiResponse<AnalysisResponse> getAnalysisByMatch(
            @AuthUser User user,
            @Parameter(description = "매치 ID", required = true) @PathVariable Long matchId) {
        AnalysisResponse analysis = analysisService.getAnalysisByMatchId(matchId, user.getTier());
        return ApiResponse.success(analysis);
    }

    @Operation(summary = "플레이어의 분석 목록 조회", description = "특정 플레이어(puuid)의 모든 경기 분석을 페이징하여 조회합니다.")
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @GetMapping("/player/{puuid}")
    public ApiResponse<Page<AnalysisResponse>> getPlayerAnalyses(
            @Parameter(description = "플레이어 PUUID", required = true) @PathVariable String puuid,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
        Page<AnalysisResponse> analyses = analysisService.getAnalysesByPuuid(puuid, pageable);
        return ApiResponse.success(analyses);
    }

    @Operation(summary = "경기 분석 생성", description = "새로운 경기 분석을 생성합니다.")
    @ApiErrorExamples({
            ErrorCode.MATCH_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping
    public ApiResponse<AnalysisResponse> createAnalysis(
            @AuthUser User user,
            @Valid @RequestBody AnalysisCreateRequest request) {
        AnalysisResponse analysis = analysisService.createAnalysis(request, user.getTier());
        return ApiResponse.success(analysis);
    }

    @Operation(summary = "분석 삭제", description = "특정 경기 분석을 삭제합니다.")
    @ApiErrorExamples({
            ErrorCode.ANALYSIS_NOT_FOUND,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAnalysis(
            @Parameter(description = "분석 ID", required = true) @PathVariable Long id) {
        analysisService.deleteAnalysis(id);
        return ApiResponse.success();
    }
}
