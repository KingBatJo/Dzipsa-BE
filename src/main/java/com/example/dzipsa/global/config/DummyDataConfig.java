package com.example.dzipsa.global.config;

import com.example.dzipsa.domain.room.entity.Room;
import com.example.dzipsa.domain.room.entity.RoomMember;
import com.example.dzipsa.domain.room.repository.RoomMemberRepository;
import com.example.dzipsa.domain.room.repository.RoomRepository;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.entity.enums.UserRole;
import com.example.dzipsa.domain.user.entity.enums.UserStatus;
import com.example.dzipsa.domain.user.repository.UserRepository;
import com.example.dzipsa.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DummyDataConfig {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RedisUtil redisUtil;

    @Bean
    public CommandLineRunner initDummyData() {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("[DummyDataConfig] 데이터가 이미 존재하여 더미 데이터를 생성하지 않습니다.");
                return;
            }

            log.info("[DummyDataConfig] 더미 데이터 생성을 시작합니다.");

            // 1. 테스트 유저 생성 (총 3명)
            User user1 = User.builder()
                    .email("test1@example.com")
                    .nickname("테스트유저1")
                    .status(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .build();
            user1.updateProfile("테스트유저1", "1");
            userRepository.save(user1);

            User user2 = User.builder()
                    .email("test2@example.com")
                    .nickname("테스트유저2")
                    .status(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .build();
            user2.updateProfile("테스트유저2", "2");
            userRepository.save(user2);

            User user3 = User.builder()
                    .email("test3@example.com")
                    .nickname("테스트유저3")
                    .status(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .build();
            user3.updateProfile("테스트유저3", "3");
            userRepository.save(user3);

            // 2. 더미 방 생성 (user1이 방장)
            Room dummyRoom = Room.builder()
                    .name("테스트 방")
                    .motto("즐거운 우리집")
                    .ownerId(user1.getId())
                    .build();
            roomRepository.save(dummyRoom);

            // 3. 방 멤버 추가
            // user1 (방장 - Room 빌더에서 기본 membersCount = 1)
            RoomMember roomMember1 = RoomMember.builder()
                    .roomId(dummyRoom.getId())
                    .userId(user1.getId())
                    .build();
            roomMemberRepository.save(roomMember1);

            // user2 추가
            RoomMember roomMember2 = RoomMember.builder()
                    .roomId(dummyRoom.getId())
                    .userId(user2.getId())
                    .build();
            roomMemberRepository.save(roomMember2);
            dummyRoom.increaseMemberCount();

            // user3 추가
            RoomMember roomMember3 = RoomMember.builder()
                    .roomId(dummyRoom.getId())
                    .userId(user3.getId())
                    .build();
            roomMemberRepository.save(roomMember3);
            dummyRoom.increaseMemberCount();

            roomRepository.save(dummyRoom); // membersCount 업데이트 저장

            // 4. 고정 초대 코드 설정 (123456)
            String dummyCode = "123456";
            String codeKey = "invitation_code:" + dummyCode;
            String roomKey = "room_invitation:" + dummyRoom.getId();

            redisUtil.set(codeKey, String.valueOf(dummyRoom.getId()), 24, TimeUnit.HOURS);
            redisUtil.set(roomKey, dummyCode, 24, TimeUnit.HOURS);

            log.info("[DummyDataConfig] 더미 데이터 생성 완료. roomId={}, invitationCode={}", dummyRoom.getId(), dummyCode);
        };
    }
}
