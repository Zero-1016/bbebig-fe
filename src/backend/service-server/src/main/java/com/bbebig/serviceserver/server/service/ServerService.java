package com.bbebig.serviceserver.server.service;

import com.bbebig.commonmodule.clientDto.serviceServer.CommonServiceServerClientResponseDto;
import com.bbebig.commonmodule.global.response.code.error.ErrorStatus;
import com.bbebig.commonmodule.global.response.exception.ErrorHandler;
import com.bbebig.commonmodule.kafka.dto.serverEvent.ServerActionEventDto;
import com.bbebig.commonmodule.kafka.dto.serverEvent.ServerEventType;
import com.bbebig.serviceserver.category.entity.Category;
import com.bbebig.serviceserver.category.repository.CategoryRepository;
import com.bbebig.serviceserver.channel.entity.Channel;
import com.bbebig.serviceserver.channel.entity.ChannelMember;
import com.bbebig.serviceserver.channel.entity.ChannelType;
import com.bbebig.serviceserver.channel.repository.ChannelMemberRepository;
import com.bbebig.serviceserver.channel.repository.ChannelRepository;
import com.bbebig.serviceserver.global.kafka.KafkaProducerService;
import com.bbebig.serviceserver.server.dto.request.ServerCreateRequestDto;
import com.bbebig.serviceserver.server.dto.request.ServerUpdateRequestDto;
import com.bbebig.serviceserver.server.dto.response.*;
import com.bbebig.serviceserver.server.entity.RoleType;
import com.bbebig.serviceserver.server.entity.Server;
import com.bbebig.serviceserver.server.entity.ServerMember;
import com.bbebig.serviceserver.server.repository.MemberRedisRepositoryImpl;
import com.bbebig.serviceserver.server.repository.ServerMemberRepository;
import com.bbebig.serviceserver.server.repository.ServerRedisRepositoryImpl;
import com.bbebig.serviceserver.server.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final CategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;

    private final ServerRedisRepositoryImpl serverRedisRepository;
    private final MemberRedisRepositoryImpl memberRedisRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * 서버 생성
     */
    public ServerCreateResponseDto createServer(Long memberId, ServerCreateRequestDto serverCreateRequestDto) {
        Server server = Server.builder()
                .name(serverCreateRequestDto.getServerName())
                .ownerId(memberId)
                .serverImageUrl(serverCreateRequestDto.getServerImageUrl())
                .build();

        // TODO: 마일스톤2 에서 Passport 에 member 정보 넣기
        ServerMember serverMember = ServerMember.builder()
                .server(server)
                .memberId(memberId)
                .memberNickname(null)
                .memberProfileImageUrl(null)
                .roleType(RoleType.OWNER)
                .build();

        Category chatCategory = Category.builder()
                .server(server)
                .name("채팅 채널")
                .position(1)
                .build();

        Channel chatChannel = Channel.builder()
                .server(server)
                .category(chatCategory)
                .name("일반")
                .position(1)
                .channelType(ChannelType.CHAT)
                .privateStatus(false)
                .build();

        ChannelMember chatChannelMember = ChannelMember.builder()
                .channel(chatChannel)
                .serverMember(serverMember)
                .build();

        Category streamCategory = Category.builder()
                .server(server)
                .name("음성 채널")
                .position(2)
                .build();

        Channel streamChannel = Channel.builder()
                .server(server)
                .category(streamCategory)
                .name("일반")
                .position(1)
                .channelType(ChannelType.VOICE)
                .privateStatus(false)
                .build();

        ChannelMember streamChannelMember = ChannelMember.builder()
                .channel(streamChannel)
                .serverMember(serverMember)
                .build();

        serverRepository.save(server);
        serverMemberRepository.save(serverMember);
        categoryRepository.save(chatCategory);
        categoryRepository.save(streamCategory);
        channelRepository.save(chatChannel);
        channelMemberRepository.save(chatChannelMember);
        channelRepository.save(streamChannel);
        channelMemberRepository.save(streamChannelMember);

        makeServerChannelListCache(server.getId());
        // 방장이 참여하고 있는 서버 목록 캐시 데이터에 추가
        memberRedisRepository.addMemberServerToSet(memberId, server.getId());

        // Kafka로 데이터 발행
        ServerActionEventDto serverActionEventDto = ServerActionEventDto.builder()
                .serverId(server.getId())
                .type(ServerEventType.SERVER_ACTION)
                .serverName(server.getName())
                .profileImageUrl(server.getServerImageUrl())
                .status("CREATE")
                .build();
        kafkaProducerService.sendServerEvent(serverActionEventDto);

        return ServerCreateResponseDto.convertToServerCreateResponseDto(server);
    }

    /**
     * 서버 정보 조회
     */
    public ServerReadResponseDto readServer(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SERVER_NOT_FOUND));

        return ServerReadResponseDto.convertToServerReadResponseDto(server);
    }

    /**
     * 멤버가 속한 서버 목록 조회
     */
    public ServerListReadResponseDto readServerList(Long memberId) {
        List<ServerMember> serverMembers = serverMemberRepository.findAllByMemberId(memberId);

        List<Server> servers = serverMembers.stream()
                .map(ServerMember::getServer)
                .toList();

        return ServerListReadResponseDto.convertToServerListReadResponseDto(servers);
    }

    /**
     * 서버 업데이트
     */
    public ServerUpdateResponseDto updateServer(Long memberId, Long serverId, ServerUpdateRequestDto serverUpdateRequestDto) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SERVER_NOT_FOUND));

        // 서버장 권한 체크
        checkServerOwner(memberId, server);

        server.update(serverUpdateRequestDto);

        // Kafka로 데이터 발행
        ServerActionEventDto serverActionEventDto = ServerActionEventDto.builder()
                .serverId(server.getId())
                .type(ServerEventType.SERVER_ACTION)
                .serverName(server.getName())
                .profileImageUrl(server.getServerImageUrl())
                .status("UPDATE")
                .build();
        kafkaProducerService.sendServerEvent(serverActionEventDto);

        return ServerUpdateResponseDto.convertToServerUpdateResponseDto(server);
    }

    /**
     * 서버 삭제
     */
    public ServerDeleteResponseDto deleteServer(Long memberId, Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SERVER_NOT_FOUND));

        // 서버장 권한 체크
        checkServerOwner(memberId, server);

        // TODO: ServerMember 삭제, Channel 삭제, ChannelMember 삭제, Category 삭제

        serverRepository.delete(server);

        deleteAllServerRelatedCache(serverId);

        // Kafka로 데이터 발행
        ServerActionEventDto serverActionEventDto = ServerActionEventDto.builder()
                .serverId(server.getId())
                .type(ServerEventType.SERVER_ACTION)
                .serverName(server.getName())
                .profileImageUrl(server.getServerImageUrl())
                .status("DELETE")
                .build();
        kafkaProducerService.sendServerEvent(serverActionEventDto);

        return ServerDeleteResponseDto.convertToServerDeleteResponseDto(server);
    }

    /**
     * 서버에 속해있는 채널 목록 조회
     * FeignClient 를 통해 호출
     * 만약 Redis 에 캐싱된 데이터가 없다면 캐싱하는 로직을 포함
    */
    public CommonServiceServerClientResponseDto.ServerChannelListResponseDto getServerChannelList(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SERVER_NOT_FOUND));

        Set<Long> serverChannelList = serverRedisRepository.getServerChannelList(serverId);
        if (serverChannelList.isEmpty()) {
            List<Long> longs = makeServerChannelListCache(serverId);
            serverChannelList.addAll(longs);
        }
        return CommonServiceServerClientResponseDto.ServerChannelListResponseDto.builder()
                .serverId(serverId)
                .channelIdList(serverChannelList.stream().toList())
                .build();
    }

    /**
     * 서버에 속해있는 멤버 목록을 조회
     * FeignClient 를 통해 호출
     * 만약 Redis 에 캐싱된 데이터가 없다면 캐싱하는 로직을 포함
     */
    public CommonServiceServerClientResponseDto.ServerMemberListResponseDto getServerMemberList(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SERVER_NOT_FOUND));

        Set<Long> serverMemberList = serverRedisRepository.getServerMemberList(serverId);
        if (serverMemberList.isEmpty()) {
            List<Long> longs = makeServerMemberListCache(serverId);
            serverMemberList.addAll(longs);
        }
        return CommonServiceServerClientResponseDto.ServerMemberListResponseDto.builder()
                .serverId(serverId)
                .ownerId(server.getOwnerId())
                .memberIdList(serverMemberList.stream().toList())
                .build();
    }

    /**
     * 멤버별로 참여하고 있는 서버 목록 조회
     * FeignClient 를 통해 호출
     * 만약 Redis 에 캐싱된 데이터가 없다면 캐싱하는 로직을 포함
     */
    public CommonServiceServerClientResponseDto.MemberServerListResponseDto getMemberServerList(Long memberId) {
        Set<Long> memberServerList = memberRedisRepository.getMemberServerList(memberId);
        if (memberServerList.isEmpty()) {
            List<Long> longs = makeMemberServerListCache(memberId);
            memberServerList.addAll(longs);
        }
        return CommonServiceServerClientResponseDto.MemberServerListResponseDto.builder()
                .memberId(memberId)
                .serverIdList(memberServerList.stream().toList())
                .build();
    }

    /**
     * 서버에 속해있는 채널 목록을 조회하여 Redis 에 캐싱
    */
    public List<Long> makeServerChannelListCache(Long serverId) {
        List<Channel> channels = channelRepository.findAllByServerId(serverId);
        if (channels.isEmpty()) {
            throw new ErrorHandler(ErrorStatus.SERVER_CHANNELS_NOT_FOUND);
        }
        List<Long> channelIdList = channels.stream().map(Channel::getId).toList();
        if (!channelIdList.isEmpty()) {
            serverRedisRepository.saveServerChannelSet(serverId, channelIdList);
        }
        return channelIdList;
    }

    /**
     * 서버에 속해있는 멤버 목록을 조회하여 Redis 에 캐싱
    */
    public List<Long> makeServerMemberListCache(Long serverId) {
        List<ServerMember> serverMembers = serverMemberRepository.findAllByServerId(serverId);
        if (serverMembers.isEmpty()) {
            throw new ErrorHandler(ErrorStatus.SERVER_MEMBERS_NOT_FOUND);
        }
        List<Long> memberIdList = serverMembers.stream().map(ServerMember::getMemberId).toList();
        if (!memberIdList.isEmpty()) {
            serverRedisRepository.saveServerMemberSet(serverId, memberIdList);
        }
        return memberIdList;
    }

    /**
     *  멤버가 참여중인 서버 목록을 조회하여 Redis 에 캐싱
     */
    public List<Long> makeMemberServerListCache(Long memberId) {
        List<ServerMember> serverMembers = serverMemberRepository.findAllByMemberId(memberId);
        if (serverMembers.isEmpty()) {
            throw new ErrorHandler(ErrorStatus.MEMBER_PARTICIPATE_SERVER_NOT_FOUND);
        }
        List<Long> serverIdList = serverMembers.stream().map(serverMember -> serverMember.getServer().getId()).toList();
        if (!serverIdList.isEmpty()) {
            memberRedisRepository.saveMemberServerSet(memberId, serverIdList);
        }
        return serverIdList;
    }

    // 서버 삭제 시 관련된 캐시 삭제
    private void deleteAllServerRelatedCache(Long serverId) {
        serverRedisRepository.getServerMemberList(serverId).forEach(memberIdInServer ->
                memberRedisRepository.removeMemberServerFromSet(memberIdInServer, serverId));
        serverRedisRepository.deleteServerChannelList(serverId);
        serverRedisRepository.deleteServerMemberList(serverId);
    }

    // 서버장 권한 체크
    private void checkServerOwner(Long memberId, Server server) {
        if (!server.getOwnerId().equals(memberId)) {
            throw new ErrorHandler(ErrorStatus.SERVER_OWNER_FORBIDDEN);
        }
    }
}
