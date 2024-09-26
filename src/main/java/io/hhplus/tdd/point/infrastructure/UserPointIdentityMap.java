package io.hhplus.tdd.point.infrastructure;

import io.hhplus.tdd.point.entity.UserPoint;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UserPointIdentityMap {

    private static final Map<Long, UserPoint> userPointMap = new ConcurrentHashMap<>();

    public static void put(UserPoint userPoint) {
        userPointMap.put(userPoint.id(), userPoint);
    }

    public static void remove(UserPoint userPoint) {
        UserPoint findUserPoint = userPointMap.get(userPoint.id());
        if (findUserPoint != null && userPoint.point() == findUserPoint.point()) {
            userPointMap.remove(userPoint.id());
        }
    }

    public static Optional<UserPoint> find(long userId) {
        return Optional.ofNullable(userPointMap.get(userId));
    }
}
