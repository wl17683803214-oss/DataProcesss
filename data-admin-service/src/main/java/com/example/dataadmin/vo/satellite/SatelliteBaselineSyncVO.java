package com.example.dataadmin.vo.satellite;

import java.util.ArrayList;
import java.util.List;

/** 基线卫星逐条新增的结果，明确展示成功及失败，不隐瞒部分成功。 */
public class SatelliteBaselineSyncVO {
    /** 基线查询到的记录数，重复代号仍分别计数。 */
    private int queriedCount;
    /** 已新增并完成远程下发的记录数。 */
    private int createdCount;
    /** 同步失败的记录，原有卫星不受影响。 */
    private final List<Failure> failures = new ArrayList<>();

    /** 获取查询数量。 */
    public int getQueriedCount() { return queriedCount; }
    /** 设置查询数量。 */
    public void setQueriedCount(int queriedCount) { this.queriedCount = queriedCount; }
    /** 获取成功数量。 */
    public int getCreatedCount() { return createdCount; }
    /** 记录一条成功。 */
    public void addCreated() { createdCount++; }
    /** 获取失败数量。 */
    public int getFailedCount() { return failures.size(); }
    /** 获取失败详情。 */
    public List<Failure> getFailures() { return failures; }

    /** 用基线行号区分名称和代号完全相同的记录。 */
    public static class Failure {
        /** 本次查询结果中的行号，从一开始。 */
        private final int rowNumber;
        /** 卫星名称。 */
        private final String satelliteName;
        /** 卫星代号。 */
        private final String satelliteCode;
        /** 中文失败原因。 */
        private final String message;

        /** 保存失败记录的定位信息。 */
        public Failure(int rowNumber, String satelliteName, String satelliteCode, String message) {
            this.rowNumber = rowNumber;
            this.satelliteName = satelliteName;
            this.satelliteCode = satelliteCode;
            this.message = message;
        }

        /** 获取行号。 */
        public int getRowNumber() { return rowNumber; }
        /** 获取卫星名称。 */
        public String getSatelliteName() { return satelliteName; }
        /** 获取卫星代号。 */
        public String getSatelliteCode() { return satelliteCode; }
        /** 获取中文原因。 */
        public String getMessage() { return message; }
    }
}
