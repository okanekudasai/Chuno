package com.leesfamily.chuno.room;

import com.leesfamily.chuno.common.model.Location;
import com.leesfamily.chuno.room.model.RoomResponse;
import com.leesfamily.chuno.room.model.RoomEntity;
import com.leesfamily.chuno.room.model.RoomRequest;
import com.leesfamily.chuno.room.model.dto.*;
import com.leesfamily.chuno.room.model.*;
import com.leesfamily.chuno.user.UserRepository;
import com.leesfamily.chuno.user.UserService;
import com.leesfamily.chuno.user.model.dto.UserInventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.awt.*;
import java.time.LocalDate;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    final private EntityManager em;
    final private RoomRepository roomRepository;
    final private UserRepository userRepository;
    final private PushRepository pushRepository;
    final private UserService userService;

    @Transactional(readOnly = true)
    public List<RoomResponse> getNearByRooms(Location loc) {
        Double latitude = loc.getLat();
        Double longitude = loc.getLng();
//        String pointFormat = String.format("'LINESTRING(%f %f, %f %f)')", x1, y1, x2, y2);
        Query query = em.createNativeQuery("SELECT r.*, u.*, " +
                        " (6371*acos(cos(radians(" + latitude + ")) " +
                        " * cos(radians(r.lat)) " +
                        " * cos(radians(r.lng) - radians(" + longitude + ")) " +
                        " + sin(radians(" + latitude + ")) * sin(radians(r.lat)))) as distance," +
                        " IF(p.room_id = r.room_id, true, false) as isPushed "
                        + " FROM rooms AS r " +
                        " LEFT JOIN users AS u " +
                        " ON r.host_id = u.user_id " +
                        " LEFT JOIN pushes p " +
                        " ON p.user_id = u.user_id "
//                        + " WHERE distance  " +
                        + " ORDER BY distance ", RoomEntity.class)
                .setMaxResults(20);

        List<RoomEntity> rooms = query.getResultList();

        List<RoomResponse> roomRes = rooms.stream().map(room ->
                new RoomResponse(room, loc)
        ).collect(Collectors.toList());
        return roomRes;
    }

    public RoomResponse getRoomById(Long roomId) {
        return new RoomResponse(roomRepository.findById(roomId).get());
    }

    public RoomEntity insRoom(RoomRequest room, Long host_id) {
        DateTime dt = new DateTime();
        dt.setHour(room.getHour());
        dt.setMinute(room.getMinute());
        LocalDate now = LocalDate.now();
        dt.setYear(now.getYear());
        dt.setMonth(now.getMonthValue());
        dt.setDay(now.getDayOfMonth());

        RoomEntity roomEntity = RoomEntity.builder()
                .lng(room.getLng())
                .lat(room.getLat())
                .title(room.getTitle())
                .isPublic(room.isPublic())
                .radius(room.getRadius())
                .password(room.getPassword())
                .dateTime(dt)
                .host(userRepository.findById(host_id).get())
                .build();
        RoomEntity res = roomRepository.save(roomEntity);
        roomRepository.flush();
        return res;
    }

    public Map<String, Object> joinRoom(long roomId, Long userId) {
        Optional<RoomEntity> res = roomRepository.findById(roomId);
        Map<String, Object> resMap = new HashMap<>();
        if (res.isPresent()) {
            RoomEntity room = res.get();
            int currentPlayers = room.getCurrentPlayers();
            int maxPlayers = room.getMaxPlayers();
            if (currentPlayers == maxPlayers) {
                resMap.put("code", "2");
            } else {
                room.setCurrentPlayers(currentPlayers + 1);
                roomRepository.saveAndFlush(room);
                RoomResponse dto = new RoomResponse(room, null);
                resMap.put("result", dto);
                resMap.put("code", "1");
            }
        } else {
            resMap.put("code", 0);
        }
        return resMap;
    }


    public List<RoomResponse> getRoomsByConditinos(RoomListByConditionsDto roomListByConditionDto) {
        Double latitude = roomListByConditionDto.getLoc().getLat(); // 위도
        Double longitude = roomListByConditionDto.getLoc().getLat(); // 경도

        // condition
        // 조건은 방제목으로 구현

        Query query = em.createNativeQuery("SELECT r.*, u.*, " +
                        " (6371*acos(cos(radians(" + latitude + ")) " +
                        " * cos(radians(r.lat)) " +
                        " * cos(radians(r.lng) - radians(" + longitude + ")) " +
                        " + sin(radians(" + latitude + ")) * sin(radians(r.lat)))) as distance," +
                        " IF(p.room_id = r.room_id, true, false) as isPushed "
                        + " FROM rooms AS r " +
                        " LEFT JOIN users AS u " +
                        " ON r.host_id = u.user_id " +
                        " LEFT JOIN pushes p " +
                        " ON p.user_id = u.user_id " +
                        " WHERE r.title LIKE '%" + roomListByConditionDto.getKeyword() + "%' " +
                        " ORDER BY distance ", RoomEntity.class)
                .setMaxResults(20);

        List<RoomEntity> rooms = query.getResultList();

        List<RoomResponse> roomRes = rooms.stream().map(room ->
                new RoomResponse(room, roomListByConditionDto.getLoc())
        ).collect(Collectors.toList());
        return roomRes;
    }

    public PushEntity pushRoom(long roomId, Long userId) {
        PushEntity pushEntity = PushEntity.builder()
                .room(roomRepository.findById(roomId).get())
                .user(userRepository.findById(userId).get())
                .build();
        PushEntity res = pushRepository.saveAndFlush(pushEntity);
        return res;
    }


    public RoomGameStartResponseDto startRoom(RoomGameStartRequestDto roomStartRequestDto) {
        // (1) 방 테이블 조회
        // json test
        // {
        //    "roomId" : 1,
        //    "userIdList" : [
        //        1, 2
        //    ]
        // }
        RoomStartDto roomStartDto = roomRepository.findById(roomStartRequestDto.getRoomId())
                .map(RoomStartDto::of)
                .orElseThrow(() -> new RuntimeException("room 정보가 조회되지 않는다."));

        log.info("room 정보 조회 : " + roomStartDto.getTitle() + " id : " + roomStartDto.getId() + " 사용자 : " + roomStartDto.getCurrentPlayers());

        // 현재 들어와있는 사용자 수 update
        roomStartDto.setCurrentPlayers(roomStartRequestDto.getUserIdList().size());

        // (2) 노비문서 위도, 경도 회원 x 2
        // 참고 : https://gist.github.com/fuxingloh/5f53a618ce3c80b0abaf
        // - 방 정보에서 반지름과 중심 좌표를 받는다. (위도 : lat, 경도 : lng, 반지름 : radius)
        // - 현재 반지름 기준 random

        // 좌표 인원 수 x 2개 위치 구하는 함수
        List<RoomGameStartSlaveDocumentDto> randomLatLng = randomLatLngCoordinate(roomStartDto);
        log.info("현재 좌표 : " + roomStartDto.getLat() + " " + roomStartDto.getLng());
        log.info("반지름 : " + roomStartDto.getRadius());
        log.info("좌표 : " + randomLatLng.get(0).getLat() + " " + randomLatLng.get(0).getLng()
                + " " + randomLatLng.get(1).getLat() + " " + randomLatLng.get(1).getLng()
                + " " + randomLatLng.get(2).getLat() + " " + randomLatLng.get(2).getLng()
        );

        // (3) 노비, 추노꾼 랜덤
        // 사용자 정보를 조회한 후, 거기서 아이템을 조회해햐아 한다.
        // user 기반 item 조회
        // chaser, runner
        List<RoomGameStartDecideChaserRunnerDto> roomGameUserChaserOrRunnerItemCntResultList = randomUserChaserRunner(roomStartRequestDto);

        return RoomGameStartResponseDto.of(roomStartDto, randomLatLng, roomGameUserChaserOrRunnerItemCntResultList);

    }

    private List<RoomGameStartDecideChaserRunnerDto> randomUserChaserRunner(RoomGameStartRequestDto roomStartRequestDto) {
        // 사용자들에게 추노꾼, 노비 랜덤 지정
        int userCount = roomStartRequestDto.getUserIdList().size();
        boolean[] visited = new boolean[userCount];
        List<RoomGameStartDecideChaserRunnerDto> roomGameUserChaserOrRunnerItemCntDtoList = new ArrayList<>();
        int addUserCnt = 0;

        // 노비 선정
        while (true) {
            // randomIndex는 0 ~ n - 1까지
            int randomIndex = (int) (Math.random() * (userCount));

//            log.info("랜덤 시작");
//            for(int i =0 ; i< 20; i++){
//                log.info("i : " +((int) (Math.random() * (userCount-1))+1));
//            }

            log.info("현재나온 random Index : " + randomIndex);
            if (!visited[randomIndex]) {
                visited[randomIndex] = true;
                // 인벤토리를 가져온다.
                // 넘거야할 것
                // 아이템 횟수
                log.info("random Index : " + randomIndex);
                log.info("getProfile 넣기전 : " + roomStartRequestDto.getUserIdList().get(randomIndex));
                log.info("items : " + userService.getProfile(roomStartRequestDto.getUserIdList().get(randomIndex)).getItems()[0]);
                log.info("items : " + userService.getProfile(roomStartRequestDto.getUserIdList().get(randomIndex)).getItems()[1]);
                log.info("items : " + userService.getProfile(roomStartRequestDto.getUserIdList().get(randomIndex)).getItems()[2]);
                int[] beItems = userService.getProfile(roomStartRequestDto.getUserIdList().get(randomIndex)).getItems();
                int[] items = Arrays.copyOfRange(beItems, 0, 4);

                Long userId = roomStartRequestDto.getUserIdList().get(randomIndex); // userId
                List<Integer> list = Arrays.stream(items).boxed().collect(Collectors.toList()); // int형 -> list형으로
                roomGameUserChaserOrRunnerItemCntDtoList.add(new RoomGameStartDecideChaserRunnerDto(userId, list));

                addUserCnt += 1;
            }

            // 절반이상 노비가 판결되면 종료
            // 인원 수 : 8
            // 0 ~ 3 : 노비
            // 4 ~ 7 : 추노
            if (addUserCnt >= userCount / 2) break;
        }

        // 추노 선정
        for (int userIdx = 0; userIdx < userCount; userIdx++) {
            if (!visited[userIdx]) {
                // 아직 포지션 정해지지 못했다면
                log.info("index : " + userIdx);
                log.info("for문 index : " + userService.getProfile(roomStartRequestDto.getUserIdList().get(userIdx)).getNickname());

                int[] beItems = userService.getProfile(roomStartRequestDto.getUserIdList().get(userIdx)).getItems();
                int[] items = Arrays.copyOfRange(beItems, 4, 8);
                Long userId = roomStartRequestDto.getUserIdList().get(userIdx); // userId
                List<Integer> list = Arrays.stream(items).boxed().collect(Collectors.toList()); // int형 -> list형으로
                roomGameUserChaserOrRunnerItemCntDtoList.add(new RoomGameStartDecideChaserRunnerDto(userId, list));
            }
        }

        return roomGameUserChaserOrRunnerItemCntDtoList;
    }

    private List<RoomGameStartSlaveDocumentDto> randomLatLngCoordinate(RoomStartDto roomStartDto) {
        // random 좌표를 구한다.
        List<RoomGameStartSlaveDocumentDto> resList = new ArrayList<>();
        // n 번 돌린다.
//        Random random = new Random();
        double lat = roomStartDto.getLat();
        double lng = roomStartDto.getLng();
        double radius = roomStartDto.getRadius() * 0.7;
        double radiusInDegrees = radius / 111000f;
        int idx = 0;
        while(idx < roomStartDto.getCurrentPlayers() * 2) {
            double u = Math.random();
            double v = Math.random();
            double w = radiusInDegrees * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            double x = w * Math.cos(t);
            double y = w * Math.sin(t);

            // Adjust the x-coordinate for the shrinking of the east-west distances
            double new_x = x / Math.cos(lat);

            // 거리를 계산
//            double distance = (6371 * Math.acos(Math.cos(Math.toRadians(lat))
//                    * Math.cos(Math.toRadians(new_x + lng)) * Math.cos(Math.toRadians(y + lat) - Math.toRadians(lng))
//                    + Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(new_x + lng))));

            resList.add(new RoomGameStartSlaveDocumentDto(y + lat, new_x + lng));
            idx += 1;
//            System.out.println("idx : " + idx + "distance : " + distance + " radius" + radius + " new_x : " + new_x);
        }
        // Convert radius from meters to degrees

        return resList;
    }

    public RoomGameEndResponseDto endRoom(RoomGameEndRequestDto roomGameEndRequestDto, Long userId) {
        return roomRepository.endRoom(roomGameEndRequestDto, userId);
    }
}
