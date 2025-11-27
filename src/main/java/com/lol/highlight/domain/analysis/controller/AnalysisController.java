package com.lol.highlight.domain.analysis.controller;

import com.lol.highlight.domain.analysis.dto.AnalysisCreateRequest;
import com.lol.highlight.domain.analysis.dto.AnalysisResponse;
import com.lol.highlight.domain.analysis.service.AnalysisService;
import com.lol.highlight.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Analysis", description = "경기 분석 API")
@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "분석 정보 조회",
            description = "분석 ID로 특정 분석의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/{id}")
    public ApiResponse<AnalysisResponse> getAnalysis(
            @Parameter(description = "분석 ID", required = true) @PathVariable Long id) {
        AnalysisResponse analysis = analysisService.getAnalysisById(id);
        return ApiResponse.success(analysis);
    }

    @Operation(
            summary = "매치의 분석 조회",
            description = "특정 매치에 대한 분석 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치 또는 분석을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/match/{matchId}")
    public ApiResponse<AnalysisResponse> getAnalysisByMatch(
            @Parameter(description = "매치 ID", required = true) @PathVariable Long matchId) {
        AnalysisResponse analysis = analysisService.getAnalysisByMatchId(matchId);
        return ApiResponse.success(analysis);
    }

    @Operation(
            summary = "사용자의 분석 목록 조회",
            description = "특정 사용자의 모든 경기 분석을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/user/{userId}")
    public ApiResponse<Page<AnalysisResponse>> getUserAnalyses(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
        Page<AnalysisResponse> analyses = analysisService.getUserAnalyses(userId, pageable);
        return ApiResponse.success(analyses);
    }

    @Operation(
            summary = "경기 분석 생성",
            description = "새로운 경기 분석을 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 존재하는 분석"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ApiResponse<AnalysisResponse> createAnalysis(
            @Valid @RequestBody AnalysisCreateRequest request) {
        AnalysisResponse analysis = analysisService.createAnalysis(request);
        return ApiResponse.success(analysis);
    }

    @Operation(
            summary = "AI 분석 재생성",
            description = "기존 분석을 AI를 통해 다시 생성합니다. (비동기 처리)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "재생성 요청 접수",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "매치 데이터 불충분"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/{id}/regenerate")
    public ApiResponse<AnalysisResponse> regenerateAnalysis(
            @Parameter(description = "분석 ID", required = true) @PathVariable Long id) {
        AnalysisResponse analysis = analysisService.regenerateAnalysis(id);
        return ApiResponse.accepted("분석 재생성 요청이 접수되었습니다");
    }

    @Operation(
            summary = "분석 삭제",
            description = "특정 경기 분석을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAnalysis(
            @Parameter(description = "분석 ID", required = true) @PathVariable Long id) {
        analysisService.deleteAnalysis(id);
        return ApiResponse.success();
    }
}
