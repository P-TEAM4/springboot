package com.lol.highlight.domain.match.service;

import com.lol.highlight.domain.match.dto.MatchImportRequest;
import com.lol.highlight.domain.match.dto.MatchResponse;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.entity.MatchStatus;
import com.lol.highlight.domain.match.repository.MatchRepository;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        return MatchResponse.from(match);
    }

    public Page<MatchResponse> getUserMatches(Long userId, Pageable pageable) {
        return matchRepository.findByUserId(userId, pageable)
                .map(MatchResponse::from);
    }

    public List<MatchResponse> getRecentMatches(Long userId, int count) {
        List<Match> matches = matchRepository.findTop20ByUserIdOrderByGameCreationDesc(userId);
        return matches.stream()
                .limit(count)
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MatchResponse importMatch(Long userId, MatchImportRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (matchRepository.existsByMatchId(request.getMatchId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Match already imported");
        }

        // TODO: Riot API를 통해 실제 매치 데이터 가져오기
        Match match = Match.builder()
                .user(user)
                .matchId(request.getMatchId())
                .championName("Unknown")
                .status(MatchStatus.PENDING)
                .build();

        match = matchRepository.save(match);

        // TODO: 비동기로 Riot API 호출 및 데이터 처리
        log.info("Match import initiated for matchId: {}", request.getMatchId());

        return MatchResponse.from(match);
    }

    @Transactional
    public void deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        matchRepository.delete(match);
    }

    @Transactional
    public void syncUserMatches(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRiotId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "User must link Riot account first");
        }

        // TODO: Riot API를 통해 최근 매치 목록 가져와서 저장
        log.info("Match sync initiated for user: {}", userId);
    }
}
