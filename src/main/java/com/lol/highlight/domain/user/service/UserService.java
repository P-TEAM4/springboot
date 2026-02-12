package com.lol.highlight.domain.user.service;

import com.lol.highlight.domain.user.dto.RiotAccountLinkRequest;
import com.lol.highlight.domain.user.dto.UserResponse;
import com.lol.highlight.domain.user.dto.UserUpdateRequest;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.repository.UserRepository;
import com.lol.highlight.global.exception.BusinessException;
import com.lol.highlight.global.exception.ErrorCode;
import com.lol.highlight.global.external.riot.client.RiotApiClient;
import com.lol.highlight.global.external.riot.dto.RiotLeagueDto;
import com.lol.highlight.global.external.riot.dto.RiotSummonerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RiotApiClient riotApiClient;

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.getName(), request.getProfileImage());

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse linkRiotAccount(Long id, RiotAccountLinkRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String riotId = request.getSummonerName() + "#" + request.getTagLine();

        if (userRepository.existsByRiotId(riotId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 다른 사용자에게 연동된 Riot 계정입니다");
        }

        // Riot API로 계정 검증 및 정보 조회
        RiotSummonerDto accountDto = riotApiClient.getSummonerByRiotId(
                request.getSummonerName(), request.getTagLine());
        String puuid = accountDto.getPuuid();

        RiotSummonerDto summonerDto = riotApiClient.getSummonerByPuuid(puuid);

        user.linkRiotAccount(riotId, puuid, request.getSummonerName(), request.getTagLine());

        // 소환사 정보 업데이트 (프로필 아이콘, 레벨, 랭크 정보)
        String tier = null;
        String rank = null;
        Integer leaguePoints = null;
        Integer wins = null;
        Integer losses = null;

        try {
            List<RiotLeagueDto> leagueEntries = riotApiClient.getLeagueByPuuid(puuid);
            if (leagueEntries != null && !leagueEntries.isEmpty()) {
                // 솔로 랭크 우선, 없으면 첫 번째 엔트리 사용
                RiotLeagueDto soloRank = leagueEntries.stream()
                        .filter(e -> "RANKED_SOLO_5x5".equals(e.getQueueType()))
                        .findFirst()
                        .orElse(leagueEntries.get(0));

                tier = soloRank.getTier();
                rank = soloRank.getRank();
                leaguePoints = soloRank.getLeaguePoints();
                wins = soloRank.getWins();
                losses = soloRank.getLosses();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch league info for puuid: {}", puuid, e);
        }

        user.updateSummonerInfo(
                summonerDto.getProfileIconId(),
                summonerDto.getSummonerLevel(),
                tier, rank, leaguePoints, wins, losses
        );

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse unlinkRiotAccount(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRiotId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "연동된 Riot 계정이 없습니다");
        }

        user.unlinkRiotAccount();
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}
