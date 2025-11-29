package com.lol.highlight.domain.match.service;

import com.lol.highlight.domain.match.dto.MatchListResponse;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.service.RiotApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final RiotApiService riotApiService;

    /**
     * 매치 ID로 조회 (DB에 저장된 매치만)
     */
    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        return MatchResponse.from(match);
    }

    /**
     * 사용자의 DB에 저장된 매치 목록 조회 (페이징)
     * - 분석/하이라이트가 생성된 매치만 조회됨
     */
    public Page<MatchResponse> getUserMatches(Long userId, Pageable pageable) {
        return matchRepository.findByUserId(userId, pageable)
                .map(MatchResponse::from);
    }

    /**
     * 최근 매치 리스트 조회 (캐시 사용, DB 저장 안 함)
     * - Riot API에서 직접 가져와 상세 통계 포함
     * - Rate Limit 회피를 위해 로컬 캐시 사용
     *
     * @param userId 사용자 ID
     * @param count 조회할 매치 개수 (기본 20)
     * @return 매치 리스트 (MatchListResponse)
     */
    public List<MatchListResponse> getRecentMatches(Long userId, int count) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRiotPuuid() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "User must link Riot account first");
        }

        // RiotApiService의 캐시된 메서드 호출
        return riotApiService.getRecentMatchList(user, count);
    }

    /**
     * 매치 삭제 (DB에서만 삭제, 캐시는 TTL로 자동 만료)
     */
    @Transactional
    public void deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        matchRepository.delete(match);
    }
}
