package io.hhplus.tdd.unit;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.RequestPointNotPositiveException;
import io.hhplus.tdd.point.exception.MaxPointExceededException;
import io.hhplus.tdd.point.exception.OutOfPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PointServiceUnitTest extends TddApplicationUnitTest {

    /*
     * 테스트 작성 이유 : 사용자가 포인트를 성공적으로 조회하는 것을 확인합니다.
     */
    @DisplayName("사용자가 현재 남은 포인트를 조회할 수 있다.")
    @Test
    void whenUserGetPoint_ThenSeeCurrentPoint() {
        // given
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, 100, System.currentTimeMillis()));

        // when
        UserPoint result = BEAN_CONTAINER.pointService.point(userPoint.id());

        // then
        assertThat(result.id()).isEqualTo(userPoint.id());
        assertThat(result.point()).isEqualTo(userPoint.point());
    }

    /*
     * 테스트 작성 이유 : 사용자는 충전한 내역과 이용한 내역 모두를 조회할 수 있어야 합니다.
     */
    @DisplayName("사용자가 포인트 충전/이용 내역을 모두 조회할 수 있다.")
    @Test
    void whenUserGetPoint_ThenSeeAllHistories() {
        // given
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, 100, System.currentTimeMillis()));
        PointHistory pointHistory1 = BEAN_CONTAINER.pointHistoryRepository.save(new PointHistory(1, userPoint.id(), 100, TransactionType.CHARGE, System.currentTimeMillis()));
        PointHistory pointHistory2 = BEAN_CONTAINER.pointHistoryRepository.save(new PointHistory(2, userPoint.id(), 50, TransactionType.USE, System.currentTimeMillis()));

        // when
        List<PointHistory> pointHistories = BEAN_CONTAINER.pointService.history(1);

        // then
        assertThat(pointHistories).hasSize(2)
                .containsExactlyInAnyOrder(pointHistory1, pointHistory2);
    }

    /*
     * 테스트 작성 이유 : UserPointTable 이 upsert 를 하고 있기에 자동 가입을 하나의 요구사항으로 파악했습니다.
     */
    @DisplayName("등록되지 않은 사용자는 자동으로 가입할 수 있다.")
    @Test
    void whenNotUserGetPoint_thenNoPoint() {
        // given
        long notExistUserId = 999L;

        // when
        UserPoint result = BEAN_CONTAINER.pointService.point(notExistUserId);

        // then
        assertThat(result.id()).isEqualTo(notExistUserId);
        assertThat(result.point()).isEqualTo(0);
    }

    /*
     * 테스트 작성 이유 : 사용자는 포인트를 충전할 수 있어야 합니다.
     */
    @DisplayName("사용자가 포인트를 충전할 수 있다.")
    @Test
    void userCanChargePoint() {
        // given
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, 100, System.currentTimeMillis()));
        long currentPoint = userPoint.point();
        long chargeAmount = 100;

        // when
        UserPoint result = BEAN_CONTAINER.pointService.charge(userPoint.id(), chargeAmount);

        // then
        assertThat(result.id()).isEqualTo(userPoint.id());
        assertThat(result.point()).isEqualTo(currentPoint + chargeAmount);
    }

    /*
     * 테스트 작성 이유 : 0원 또는 음수는 충전 가능 금액이 되어선 안 됩니다.
     * 보통은 @Valid, @Validated 등을 사용하여 검증하지만, 해당 프로젝트에 validation 의존성이 추가되지 않았기에 모든 비즈니르 로직은 서비스에서 검증한다고 가정합니다.
     */
    @DisplayName("충전 금액은 0보다 커야 한다.")
    @Test
    void whenChargeAmountLessOrEqualZero_thenCannotCharge() {
        // given
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, 100, System.currentTimeMillis()));
        long chargeAmount = 0;

        // when
        // then
        assertThatThrownBy(() -> BEAN_CONTAINER.pointService.charge(userPoint.id(), chargeAmount))
                .isInstanceOf(RequestPointNotPositiveException.class);
    }

    /*
     * 테스트 작성 이유 : 사용자는 최대 100,000 포인트까지 보유할 수 있어야 한다는 정책을 추가했습니다.
     * 이에 대한 테스트를 작성합니다.
     */
    @DisplayName("충전 금액은 100,000보다 적어야 한다.")
    @Test
    void whenChargeAmountMoreThanMaxPoint_thenCannotCharge() {
        // given
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, 0, System.currentTimeMillis()));
        long chargeAmount = 100001;

        // when
        // then
        assertThatThrownBy(() -> BEAN_CONTAINER.pointService.charge(userPoint.id(), chargeAmount))
                .isInstanceOf(MaxPointExceededException.class);
    }

    /*
     * 테스트 작성 이유 : 사용자는 포인트를 사용할 수 있어야 합니다. 이때 사용자는 제약 없이 남은 포인트를 100% 모두 소진할 수 있습니다.(경계값 분석)
     */
    @DisplayName("사용자가 포인트를 사용할 수 있다.")
    @Test
    void userCanUsePoint() {
        // given
        long point = 100;
        long useAmount = 100;
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, point, System.currentTimeMillis()));

        // when
        UserPoint result = BEAN_CONTAINER.pointService.use(userPoint.id(), useAmount);

        // then
        assertThat(result.id()).isEqualTo(userPoint.id());
        assertThat(result.point()).isZero();
    }

    /*
     * 테스트 작성 이유 : 0원 또는 음수는 사용 가능 금액이 되어선 안 됩니다.
     * 보통은 @Valid, @Validated 등을 사용하여 검증하지만, 해당 프로젝트에 validation 의존성이 추가되지 않았기에 모든 비즈니르 로직은 서비스에서 검증한다고 가정합니다.
     */
    @DisplayName("사용할 포인트는 0원보다 커야 한다.")
    @Test
    void whenUseAmountLessOrEqualZero_thenCannotUse() {
        // given
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, 100, System.currentTimeMillis()));
        long useAmount = 0;

        // when
        // then
        assertThatThrownBy(() -> BEAN_CONTAINER.pointService.use(userPoint.id(), useAmount))
                .isInstanceOf(RequestPointNotPositiveException.class);
    }

    /*
     * 테스트 작성 이유 : "잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다." 라는 요구사항을 만족시키기 위해 테스트를 작성합니다.
     */
    @DisplayName("잔고가 부족하면 포인트를 사용할 수 없다.")
    @Test
    void whenPointNotEnoughThenCannotUse() {
        // given
        UserPoint userPoint = BEAN_CONTAINER.userPointRepository.save(new UserPoint(1, 10, System.currentTimeMillis()));
        long useAmount = userPoint.point() + 1;

        // when
        // then
        assertThatThrownBy(() -> BEAN_CONTAINER.pointService.use(userPoint.id(), useAmount))
                .isInstanceOf(OutOfPointException.class);
    }
}
