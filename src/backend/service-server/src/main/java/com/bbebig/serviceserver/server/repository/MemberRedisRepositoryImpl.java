package com.bbebig.serviceserver.server.repository;

import com.bbebig.commonmodule.redis.domain.MemberPresenceStatus;
import com.bbebig.commonmodule.redis.domain.RecentServerChannelInfo;
import com.bbebig.commonmodule.redis.repository.MemberRedisRepository;
import com.bbebig.commonmodule.redis.util.MemberRedisKeys;
import com.bbebig.commonmodule.redis.util.MemberRedisTTL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRedisRepositoryImpl implements MemberRedisRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * 개별 유저의 참여중인 서버 목록을 Hash 구조로 저장
	 * ex) member:{memberId}:serverList => Set<ServerId>
	 */
	@Override
	public void saveMemberServerSet(Long memberId, List<Long> serverIdList) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		redisTemplate.opsForSet().add(key, serverIdList.toArray());
	}

	// 개별 유저의 참여중인 서버 목록에 서버 추가
	@Override
	public void addMemberServerToSet(Long memberId, Long serverId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		redisTemplate.opsForSet().add(key, serverId);
	}

	// 개별 유저의 참여중인 서버 목록에서 서버 삭제
	@Override
	public void removeMemberServerFromSet(Long memberId, Long serverId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		redisTemplate.opsForSet().remove(key, serverId);
	}

	// 개별 유저의 참여중인 서버 목록이 존재하는지 확인
	@Override
	public boolean existsMemberServerList(Long memberId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	// 개별 유저의 참여중인 서버 목록 반환
	@Override
	public Set<Long> getMemberServerList(Long memberId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		Set<Object> serverSet = redisTemplate.opsForSet().members(key);
		if (serverSet == null) {
			return Set.of();
		}
		return serverSet.stream()
				.map(this::convertToLong)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * 개별 유저의 DM 채널 목록을 Hash 구조로 저장
	 * ex) member:{memberId}:dmList => Set<ChannelId>
	 */
	@Override
	public void saveMemberDmSet(Long memberId, List<Long> channelIdList) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		redisTemplate.opsForSet().add(key, channelIdList.toArray());
	}

	// 개별 유저의 DM 채널 목록에 채널 추가
	@Override
	public void addMemberDmToSet(Long memberId, Long channelId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		redisTemplate.opsForSet().add(key, channelId);
	}

	// 개별 유저의 DM 채널 목록에서 채널 삭제
	@Override
	public void removeMemberDmFromSet(Long memberId, Long channelId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		redisTemplate.opsForSet().remove(key, channelId);
	}

	// 개별 유저의 DM 채널 목록이 존재하는지 확인
	@Override
	public boolean existsMemberDmList(Long memberId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	// 개별 유저의 DM 채널 목록 반환
	@Override
	public Set<Long> getMemberDmList(Long memberId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		Set<Object> dmSet = redisTemplate.opsForSet().members(key);
		if (dmSet == null) {
			return Set.of();
		}
		return dmSet.stream()
				.map(this::convertToLong)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * 개별 유저의 presence 상태 및 Device Info를 Json을 통째로 저장
	 * ex) member:{memberId}:memberStatus => MemberPresenceStatus
	 */
	@Override
	public void saveMemberPresenceStatus(Long memberId, MemberPresenceStatus status) {
		String key = MemberRedisKeys.getMemberStatusKey(memberId);
		redisTemplate.opsForValue().set(key, status);
	}

	// 개별 유저 presence 상태 정보 조회
	@Override
	public MemberPresenceStatus getMemberPresenceStatus(Long memberId) {
		String key = MemberRedisKeys.getMemberStatusKey(memberId);
		Object obj = redisTemplate.opsForValue().get(key);
		if (obj instanceof MemberPresenceStatus) {
			return (MemberPresenceStatus) obj;
		}
		return null;
	}

	/**
	 * 개별 유저의 서버 채널별 마지막 접속 시간 및 읽음 정보를 Hash 구조로 저장
	 * ex) member:{memberId}:recentServerChannels => Hash<ChannelId, RecentServerChannelInfo>
	 */
	@Override
	public void saveMemberRecentServerChannels(Long memberId, Long channelId, RecentServerChannelInfo recentServerChannelInfo) {
		String key = MemberRedisKeys.getMemberRecentServerChannelsKey(memberId);
		redisTemplate.opsForHash().put(key, channelId, recentServerChannelInfo);

		redisTemplate.expire(key, MemberRedisTTL.getRecentChannelInfoTTLDate(), TimeUnit.DAYS);
	}

	// 개별 유저의 서버 채널별 마지막 접속 시간 및 읽음 정보 조회
	@Override
	public RecentServerChannelInfo getMemberRecentServerChannel(Long memberId, Long channelId) {
		String key = MemberRedisKeys.getMemberRecentServerChannelsKey(memberId);
		Object obj = redisTemplate.opsForHash().get(key, channelId);
		if (obj instanceof RecentServerChannelInfo) {
			return (RecentServerChannelInfo) obj;
		}
		return null;
	}

	// 개별 유저의 서버 채널별 마지막 접속 시간 및 읽음 정보 삭제
	@Override
	public void deleteMemberRecentServerChannel(Long memberId, Long channelId) {
		String key = MemberRedisKeys.getMemberRecentServerChannelsKey(memberId);
		redisTemplate.opsForHash().delete(key, channelId);
	}

	// 개별 유저의 서버 채널별 마지막 접속 시간 및 읽음 정보 전체 삭제
	@Override
	public void deleteMemberAllRecentServerChannels(Long memberId) {
		String key = MemberRedisKeys.getMemberRecentServerChannelsKey(memberId);
		redisTemplate.delete(key);
	}

	// 객체를 Long으로 변환 (내부 메서드)
	public Long convertToLong(Object obj) {
		try {
			if (obj instanceof Long) {
				return (Long) obj;
			}
			return Long.valueOf(obj.toString());
		} catch (NumberFormatException e) {
			log.error("convertToLong: 변환 실패, obj={}", obj, e);
			return null;
		}
	}
}
