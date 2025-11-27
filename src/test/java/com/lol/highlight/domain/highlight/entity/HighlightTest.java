package com.lol.highlight.domain.highlight.entity;

import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.domain.match.entity.MatchStatus;
import com.lol.highlight.domain.user.entity.AuthProvider;
import com.lol.highlight.domain.user.entity.User;
import com.lol.highlight.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HighlightTest {

    private Match match;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .role(UserRole.USER)
                .build();

        match = Match.builder()
                .user(user)
                .matchId("KR_123456789")
                .championName("Ahri")
                .status(MatchStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("Highlight 엔티티 생성 성공")
    void createHighlightSuccess() {
        // given & when
        Highlight highlight = Highlight.builder()
                .match(match)
                .title("First Blood")
                .description("Amazing first blood kill")
                .videoUrl("https://example.com/video.mp4")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .startTime(120)
                .endTime(135)
                .duration(15)
                .type(HighlightType.KILL)
                .status(HighlightStatus.COMPLETED)
                .eventData("{}")
                .build();

        // then
        assertThat(highlight.getTitle()).isEqualTo("First Blood");
        assertThat(highlight.getDescription()).isEqualTo("Amazing first blood kill");
        assertThat(highlight.getVideoUrl()).isEqualTo("https://example.com/video.mp4");
        assertThat(highlight.getType()).isEqualTo(HighlightType.KILL);
        assertThat(highlight.getDuration()).isEqualTo(15);
    }

    @Test
    @DisplayName("비디오 정보 업데이트 성공")
    void updateVideoInfoSuccess() {
        // given
        Highlight highlight = Highlight.builder()
                .match(match)
                .title("First Blood")
                .status(HighlightStatus.PENDING)
                .build();

        // when
        highlight.updateVideoInfo("https://new-video.com/video.mp4", "https://new-video.com/thumbnail.jpg");

        // then
        assertThat(highlight.getVideoUrl()).isEqualTo("https://new-video.com/video.mp4");
        assertThat(highlight.getThumbnailUrl()).isEqualTo("https://new-video.com/thumbnail.jpg");
        assertThat(highlight.getStatus()).isEqualTo(HighlightStatus.COMPLETED);
    }

    @Test
    @DisplayName("하이라이트 상태 업데이트 성공")
    void updateStatusSuccess() {
        // given
        Highlight highlight = Highlight.builder()
                .match(match)
                .title("First Blood")
                .status(HighlightStatus.PENDING)
                .build();

        // when
        highlight.updateStatus(HighlightStatus.PROCESSING);

        // then
        assertThat(highlight.getStatus()).isEqualTo(HighlightStatus.PROCESSING);
    }

    @Test
    @DisplayName("조회수 증가 성공")
    void incrementViewCountSuccess() {
        // given
        Highlight highlight = Highlight.builder()
                .match(match)
                .title("First Blood")
                .build();

        // when
        highlight.incrementViewCount();
        highlight.incrementViewCount();

        // then
        assertThat(highlight.getViewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("기본 HighlightStatus는 PENDING")
    void defaultHighlightStatus() {
        // given & when
        Highlight highlight = Highlight.builder()
                .match(match)
                .title("First Blood")
                .build();

        // then
        assertThat(highlight.getStatus()).isEqualTo(HighlightStatus.PENDING);
    }

    @Test
    @DisplayName("기본 조회수는 0")
    void defaultViewCount() {
        // given & when
        Highlight highlight = Highlight.builder()
                .match(match)
                .title("First Blood")
                .build();

        // then
        assertThat(highlight.getViewCount()).isEqualTo(0);
    }
}
