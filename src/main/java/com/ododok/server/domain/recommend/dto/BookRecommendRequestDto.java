package com.ododok.server.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Getter
@NoArgsConstructor
public class BookRecommendRequestDto {

    // 🌟 프론트(Expo)에서는 "duration" 또는 "one_way_minutes" 둘 다 받음
    // 🌟 파이썬으로 보낼 때는 "one_way_minutes"로 변환하여 전송
    @JsonProperty("one_way_minutes")
    @JsonAlias("duration")
    private int duration;

    @JsonProperty("user_cpm")
    @JsonAlias("userCpm")
    private Integer userCpm;

    @JsonProperty("preferred_genres")
    @JsonAlias("preferredGenres")
    private List<String> preferredGenres;

    public int getUserCpmOrDefault() {
        return (userCpm != null && userCpm > 0) ? userCpm : 950;
    }

    // Getter & Setter
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public Integer getUserCpm() { return userCpm; }
    public void setUserCpm(Integer userCpm) { this.userCpm = userCpm; }

    public List<String> getPreferredGenres() { return preferredGenres; }
    public void setPreferredGenres(List<String> preferredGenres) { this.preferredGenres = preferredGenres; }
}