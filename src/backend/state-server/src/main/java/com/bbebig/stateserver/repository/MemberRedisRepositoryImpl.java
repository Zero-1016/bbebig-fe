package com.bbebig.stateserver.repository;

import com.bbebig.commonmodule.redis.domain.MemberPresenceStatus;
import com.bbebig.commonmodule.redis.repository.MemberRedisRepository;
import com.bbebig.commonmodule.redis.util.MemberRedisKeys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRedisRepositoryImpl implements MemberRedisRepository {

	private final RedisTemplate<String, Long> redisSetTemplate;
	private final RedisTemplate<String, MemberPresenceStatus> redisMemberStatusTemplate;

	private SetOperations<String, Long> setOperations;
	private ValueOperations<String, MemberPresenceStatus> valueOperations;

	@PostConstruct
	public void initRedisOps() {
		this.setOperations = redisSetTemplate.opsForSet();
		this.valueOperations = redisMemberStatusTemplate.opsForValue();
	}

	/**
	 * 개별 유저의 참여중인 서버 목록을 Set 구조로 저장
	 * ex) member:{memberId}:serverList => Set<ServerId>
	 */
	@Override
	public void saveMemberServerSet(Long memberId, List<Long> serverIdList) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		setOperations.add(key, serverIdList.toArray(new Long[0]));
	}

	// 개별 유저의 참여중인 서버 목록에 서버 추가
	@Override
	public void addMemberServerToSet(Long memberId, Long serverId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		setOperations.add(key, serverId);
	}

	// 개별 유저의 참여중인 서버 목록에서 서버 삭제
	@Override
	public void removeMemberServerFromSet(Long memberId, Long serverId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		setOperations.remove(key, serverId);
	}

	// 개별 유저의 참여중인 서버 목록이 존재하는지 확인
	@Override
	public boolean existsMemberServerList(Long memberId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		// hasKey는 어떤 템플릿(여기서는 redisSetTemplate)을 써도 됨
		return Boolean.TRUE.equals(redisSetTemplate.hasKey(key));
	}

	// 개별 유저의 참여중인 서버 목록 반환
	@Override
	public Set<Long> getMemberServerList(Long memberId) {
		String key = MemberRedisKeys.getMemberServerListKey(memberId);
		return setOperations.members(key);
	}

	/**
	 * 개별 유저의 DM 채널 목록을 Set 구조로 저장
	 * ex) member:{memberId}:dmList => Set<ChannelId>
	 */
	@Override
	public void saveMemberDmSet(Long memberId, List<Long> channelIdList) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		setOperations.add(key, channelIdList.toArray(new Long[0]));
	}

	// 개별 유저의 DM 채널 목록에 채널 추가
	@Override
	public void addMemberDmToSet(Long memberId, Long channelId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		setOperations.add(key, channelId);
	}

	// 개별 유저의 DM 채널 목록에서 채널 삭제
	@Override
	public void removeMemberDmFromSet(Long memberId, Long channelId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		setOperations.remove(key, channelId);
	}

	// 개별 유저의 DM 채널 목록이 존재하는지 확인
	@Override
	public boolean existsMemberDmList(Long memberId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		return Boolean.TRUE.equals(redisSetTemplate.hasKey(key));
	}

	// 개별 유저의 DM 채널 목록 반환
	@Override
	public Set<Long> getMemberDmList(Long memberId) {
		String key = MemberRedisKeys.getMemberDmListKey(memberId);
		return setOperations.members(key);
	}

	/**
	 * 개별 유저의 presence 상태 및 Device Info를 JSON 형태로 저장
	 * ex) member:{memberId}:memberStatus => MemberPresenceStatus
	 */
	@Override
	public void saveMemberPresenceStatus(Long memberId, MemberPresenceStatus status) {
		String key = MemberRedisKeys.getMemberStatusKey(memberId);
		valueOperations.set(key, status);
	}

	// 개별 유저 presence 상태 정보 조회
	@Override
	public MemberPresenceStatus getMemberPresenceStatus(Long memberId) {
		String key = MemberRedisKeys.getMemberStatusKey(memberId);
		return valueOperations.get(key);
	}
}
