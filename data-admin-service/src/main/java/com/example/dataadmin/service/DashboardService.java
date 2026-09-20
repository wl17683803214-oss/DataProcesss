package com.example.dataadmin.service;

import com.example.dataadmin.enums.DashboardRange;
import com.example.dataadmin.vo.dashboard.CollectionDashboardVO;
import com.example.dataadmin.vo.dashboard.ProcessingDashboardVO;

/** 系统总览业务接口。 */
public interface DashboardService {
  /** 查询数据采集总览。 */
  CollectionDashboardVO getCollectionDashboard(String taskId, DashboardRange range);

  /** 查询数据处理总览。 */
  ProcessingDashboardVO getProcessingDashboard(String taskId, DashboardRange range);
}
