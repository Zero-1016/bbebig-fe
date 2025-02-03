package com.bbebig.commonmodule.kafka.dto.notification;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper=false)
public class DmMemberActionEventDto extends NotificationEventDto {

	private Long targetMemberId;

	private Long channelId;

	private String targetMemberNickName;

	private String targetMemberProfileImageUrl;

	private String action; // JOIN, LEAVE, UPDATE
}
