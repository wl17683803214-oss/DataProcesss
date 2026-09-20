package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.enums.DashboardRange;
import com.example.dataadmin.service.DashboardService;
import com.example.dataadmin.vo.dashboard.CollectionDashboardVO;
import com.example.dataadmin.vo.dashboard.ProcessingDashboardVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证总览控制器把采集和处理查询交给真实业务服务。 */
class DashboardControllerTest {

    /** 总览业务服务。 */
    private final DashboardService service = mock(DashboardService.class);
    /** 待验证控制器。 */
    private final DashboardController controller =
            new DashboardController(service);

    /** 采集总览应原样返回业务服务查询结果。 */
    @Test
    void shouldReturnCollectionDashboard() {
        CollectionDashboardVO expected = new CollectionDashboardVO();
        when(service.getCollectionDashboard(
                "TASK-2001", DashboardRange.THREE_HOURS)).thenReturn(expected);

        ApiResponse<CollectionDashboardVO> response = controller.collection(
                "TASK-2001", DashboardRange.THREE_HOURS);

        assertSame(expected, response.getData());
        verify(service).getCollectionDashboard(
                "TASK-2001", DashboardRange.THREE_HOURS);
    }

    /** 处理总览应原样返回业务服务查询结果。 */
    @Test
    void shouldReturnProcessingDashboard() {
        ProcessingDashboardVO expected = new ProcessingDashboardVO();
        when(service.getProcessingDashboard(
                "TASK-2002", DashboardRange.TWENTY_FOUR_HOURS)).thenReturn(expected);

        ApiResponse<ProcessingDashboardVO> response = controller.processing(
                "TASK-2002", DashboardRange.TWENTY_FOUR_HOURS);

        assertSame(expected, response.getData());
        verify(service).getProcessingDashboard(
                "TASK-2002", DashboardRange.TWENTY_FOUR_HOURS);
    }
}
