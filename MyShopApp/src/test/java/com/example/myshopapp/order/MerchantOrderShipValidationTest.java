package com.example.myshopapp.order;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.logistics.LogisticsService;
import com.example.myshopapp.notification.NotificationService;
import com.example.myshopapp.order.repository.SubOrderRepository;
import com.example.myshopapp.shop.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 发货必须使用真实物流信息，拒绝空值与模拟单号。 */
@ExtendWith(MockitoExtension.class)
class MerchantOrderShipValidationTest {

    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private ShopRepository shopRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LogisticsService logisticsService;

    @Test
    void blankCompanyIsRejected() {
        assertRejected("", "SF1234567890");
    }

    @Test
    void blankTrackingNoIsRejected() {
        assertRejected("顺丰速运", "  ");
    }

    @Test
    void mockTrackingNoIsRejected() {
        assertRejected("顺丰速运", "mock20261118");
    }

    @Test
    void realLogisticsIsAccepted() throws Exception {
        invoke("顺丰速运", "SF1234567890");
    }

    private void assertRejected(String company, String trackingNo) {
        assertThatThrownBy(() -> invoke(company, trackingNo))
                .hasRootCauseInstanceOf(BusinessException.class);
    }

    private void invoke(String company, String trackingNo) throws Exception {
        Method method = MerchantOrderService.class.getDeclaredMethod(
                "requireRealLogistics", String.class, String.class);
        method.setAccessible(true);
        method.invoke(service(), company, trackingNo);
    }

    private MerchantOrderService service() {
        return new MerchantOrderService(subOrderRepository, shopRepository,
                notificationService, logisticsService);
    }
}
