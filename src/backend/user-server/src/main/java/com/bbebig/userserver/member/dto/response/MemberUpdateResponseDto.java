package com.bbebig.userserver.member.dto.response;

import com.bbebig.userserver.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MemberUpdateResponseDto {

    private final Long id;
    private final String name;
    private final String nickname;
    private final LocalDate birthdate;
    private final String profileImageUrl;

    public static MemberUpdateResponseDto convertToMemberUpdateResponseDto(Member member) {
        return MemberUpdateResponseDto.builder()
                .id(member.getId())
                .name(member.getName())
                .nickname(member.getNickname())
                .birthdate(member.getBirthdate())
                .profileImageUrl(member.getProfileImageUrl())
                .build();
    }
}
