package com.bbebig.commonmodule.kafka.dto.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper=false)
public class DmActionEventDto extends NotificationEventDto {

	private Long channelId;

	private Long channelName;

	private String status; // CREATE, UPDATE, DELETE

	@JsonCreator
	public DmActionEventDto(
			@JsonProperty("notificationId") Long notificationId,
			@JsonProperty("notificationEventType") NotificationEventType notificationEventType,
			@JsonProperty("channelId") Long channelId,
			@JsonProperty("channelName") Long channelName,
			@JsonProperty("status") String status
	) {
		super(notificationId, notificationEventType);
		this.channelId = channelId;
		this.channelName = channelName;
		this.status = status;
	}

}
