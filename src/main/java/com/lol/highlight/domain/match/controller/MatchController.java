package com.lol.highlight.domain.match.controller;

import com.lol.highlight.domain.match.dto.MatchImportRequest;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.service.MatchService;
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
import com.lol.highlight.global.common.ApiResponse;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/user/{userId}/recent")
    public ApiResponse<List<MatchResponse>> getRecentMatches(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "조회할 매치 개수 (기본값: 20)") @RequestParam(defaultValue = "20") int count) {
        List<MatchResponse> matches = matchService.getRecentMatches(userId, count);
        return ApiResponse.success(matches);
    }

    @Operation(
            summary = "매치 가져오기",
            description = "Riot API를 통해 특정 매치 데이터를 가져와 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "가져오기 성공",
                    content = @Content(schema = @Schema(implementation = MatchResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (중복된 매치 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/user/{userId}/import")
    public ApiResponse<MatchResponse> importMatch(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Valid @RequestBody MatchImportRequest request) {
        MatchResponse match = matchService.importMatch(userId, request);
        return ApiResponse.success(match);
    }

    @Operation(
            summary = "사용자 매치 동기화",
            description = "Riot API를 통해 사용자의 최근 매치 목록을 동기화합니다. (비동기 처리)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "동기화 요청 접수"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Riot 계정 미연동"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/user/{userId}/sync")
    public ApiResponse<Void> syncMatches(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId) {
        matchService.syncUserMatches(userId);
        return ApiResponse.accepted();
    }

    @Operation(
            summary = "매치 삭제",
            description = "특정 매치를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMatch(
            @Parameter(description = "매치 ID", required = true) @PathVariable Long id) {
        matchService.deleteMatch(id);
        return ApiResponse.success();
    }
}
