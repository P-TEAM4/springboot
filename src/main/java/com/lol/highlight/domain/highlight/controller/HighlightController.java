package com.lol.highlight.domain.highlight.controller;

import com.lol.highlight.domain.highlight.dto.HighlightCreateRequest;
import com.lol.highlight.domain.highlight.dto.HighlightResponse;
import com.lol.highlight.domain.highlight.service.HighlightService;
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

@Tag(name = "Highlight", description = "하이라이트 관리 API")
@RestController
@RequestMapping("/api/highlights")
@RequiredArgsConstructor
public class HighlightController {

    private final HighlightService highlightService;

    @Operation(
            summary = "하이라이트 정보 조회",
            description = "하이라이트 ID로 특정 하이라이트의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = HighlightResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "하이라이트를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/{id}")
    public ApiResponse<HighlightResponse> getHighlight(
            @Parameter(description = "하이라이트 ID", required = true) @PathVariable Long id) {
        HighlightResponse highlight = highlightService.getHighlightById(id);
        return ApiResponse.success(highlight);
    }

    @Operation(
            summary = "매치의 하이라이트 목록 조회",
            description = "특정 매치에 속한 모든 하이라이트를 페이징하여 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/match/{matchId}")
    public ApiResponse<Page<HighlightResponse>> getMatchHighlights(
            @Parameter(description = "매치 ID", required = true) @PathVariable Long matchId,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
        Page<HighlightResponse> highlights = highlightService.getMatchHighlights(matchId, pageable);
        return ApiResponse.success(highlights);
    }

    @Operation(
            summary = "사용자의 하이라이트 목록 조회",
            description = "특정 사용자의 모든 하이라이트를 페이징하여 조회합니다."
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
    public ApiResponse<Page<HighlightResponse>> getUserHighlights(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "페이징 정보 (page, size, sort)") Pageable pageable) {
        Page<HighlightResponse> highlights = highlightService.getUserHighlights(userId, pageable);
        return ApiResponse.success(highlights);
    }

    @Operation(
            summary = "하이라이트 생성",
            description = "새로운 하이라이트를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = HighlightResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ApiResponse<HighlightResponse> createHighlight(
            @Valid @RequestBody HighlightCreateRequest request) {
        HighlightResponse highlight = highlightService.createHighlight(request);
        return ApiResponse.success(highlight);
    }

    @Operation(
            summary = "AI 자동 하이라이트 생성",
            description = "AI를 통해 매치의 주요 장면을 자동으로 분석하여 하이라이트를 생성합니다. (비동기 처리)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "생성 요청 접수"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "매치 데이터 불충분"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/match/{matchId}/auto-generate")
    public ApiResponse<Void> generateAutoHighlights(
            @Parameter(description = "매치 ID", required = true) @PathVariable Long matchId) {
        highlightService.generateAutoHighlights(matchId);
        return ApiResponse.accepted();
    }

    @Operation(
            summary = "하이라이트 조회수 증가",
            description = "하이라이트의 조회수를 1 증가시킵니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "증가 성공",
                    content = @Content(schema = @Schema(implementation = HighlightResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "하이라이트를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/{id}/view")
    public ApiResponse<HighlightResponse> incrementViewCount(
            @Parameter(description = "하이라이트 ID", required = true) @PathVariable Long id) {
        HighlightResponse highlight = highlightService.incrementViewCount(id);
        return ApiResponse.success(highlight);
    }

    @Operation(
            summary = "하이라이트 삭제",
            description = "특정 하이라이트를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "하이라이트를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteHighlight(
            @Parameter(description = "하이라이트 ID", required = true) @PathVariable Long id) {
        highlightService.deleteHighlight(id);
        return ApiResponse.success();
    }
}
