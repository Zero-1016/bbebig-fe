package com.bbebig.chatserver.domain.kafka.config;
import com.bbebig.chatserver.domain.kafka.util.ServerEventDeserializer;
import com.bbebig.chatserver.domain.kafka.dto.serverEvent.ServerEventDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaServerEventConsumerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapAddress;

	@Value("${spring.kafka.consumer.group-id.server-event}")
	private String baseGroupId;

	@Value("${eureka.instance.instance-id}")
	private String instanceId;

	private Map<String, Object> consumerConfigurations() {
		String groupId = baseGroupId + "-" + instanceId;
		Map<String, Object> configurations = new HashMap<>();
		configurations.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		configurations.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		configurations.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configurations.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ServerEventDeserializer.class);
		configurations.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
		configurations.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // 최신 메시지
		return configurations;
	}

	@Bean
	public ConsumerFactory<String, ServerEventDto> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(consumerConfigurations());
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, ServerEventDto> serverEventListener() {
		ConcurrentKafkaListenerContainerFactory<String, ServerEventDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		factory.setConcurrency(1); // 단일 컨슈머 설정
		return factory;
	}
}
