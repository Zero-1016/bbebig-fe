package com.bbebig.userserver.member.dto.request;

import com.bbebig.userserver.member.entity.CustomPresenceStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberPresenceUpdateRequestDto {

    @Schema(description = "멤버의 접속 정보", example = "ONLINE/OFFLINE/NOT_DISTURB/INVISIBLE", required = true)
    private final CustomPresenceStatus customPresenceStatus;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MemberPresenceUpdateRequestDto(@JsonProperty("customPresenceStatus") CustomPresenceStatus customPresenceStatus) {
        this.customPresenceStatus = customPresenceStatus;
    }
}