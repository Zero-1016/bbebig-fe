package com.bbebig.chatserver.domain.kafka.dto.notification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public abstract class NotificationEventDto {

	Long memberId;

	String type; // NotificationEventType Enum 참고
}
