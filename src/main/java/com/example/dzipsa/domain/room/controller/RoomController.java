package com.example.dzipsa.domain.room.controller;

import com.example.dzipsa.domain.room.dto.request.RoomCreateRequest;
import com.example.dzipsa.domain.room.dto.request.RoomJoinRequest;
import com.example.dzipsa.domain.room.dto.request.RoomUpdateRequest;
import com.example.dzipsa.domain.room.dto.response.RoomMemberResponse;
import com.example.dzipsa.domain.room.dto.response.RoomResponse;
import com.example.dzipsa.domain.room.service.RoomService;
import com.example.dzipsa.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Rooms", description = "방 관련 API")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "방 생성", description = "방 생성 후 24시간 유효한 6자리 초대 코드가 반환됩니다.")
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody RoomCreateRequest request,
            @AuthenticationPrincipal User user) {
        RoomResponse response = roomService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/join")
    @Operation(summary = "방 초대 코드로 입장", description = "6자리 초대 코드를 사용하여 방에 입장합니다.")
    public ResponseEntity<RoomResponse> joinRoom(
            @Valid @RequestBody RoomJoinRequest request,
            @AuthenticationPrincipal User user) {
        RoomResponse response = roomService.joinRoom(request.getInvitationCode(), user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{roomId}/leave")
    @Operation(summary = "방 나가기", description = "방장이 나갈 경우 가장 오래된 멤버에게 방장이 위임됩니다.")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal User user) {
        roomService.leaveRoom(roomId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "내 방 조회", description = "현재 참여 중인 방 정보를 조회합니다.")
    public ResponseEntity<RoomResponse> getMyRoom(
            @AuthenticationPrincipal User user) {
        RoomResponse response = roomService.getMyRoom(user);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    @Operation(summary = "내 방 정보 수정", description = "현재 참여 중인 방의 이름 또는 가훈/목표를 수정합니다. 방장만 가능합니다.")
    public ResponseEntity<RoomResponse> updateMyRoom(
            @Valid @RequestBody RoomUpdateRequest request,
            @AuthenticationPrincipal User user) {
        RoomResponse response = roomService.updateMyRoom(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "방 단건 조회")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.getRoom(roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/members")
    @Operation(summary = "방 구성원 목록 조회", description = "현재 참여 중인 방의 모든 구성원 정보를 조회합니다.")
    public ResponseEntity<List<RoomMemberResponse>> getRoomMembers(
            @Parameter(description = "나 자신을 제외할지 여부 (기본값: false)")
            @RequestParam(defaultValue = "false") boolean excludeMe,
            @AuthenticationPrincipal User user) {
        List<RoomMemberResponse> members = roomService.getRoomMembers(user, excludeMe);
        return ResponseEntity.ok(members);
    }
}
