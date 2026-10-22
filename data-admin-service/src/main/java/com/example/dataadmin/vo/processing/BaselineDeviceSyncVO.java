package com.example.dataadmin.vo.processing;

/** 基线设备参数表同步统计。 */
public class BaselineDeviceSyncVO {
    /** 基线查询到的参数表数量。 */
    private final int queriedTableCount;
    /** 按工作表导入的设备数量。 */
    private final int syncedDeviceCount;
    /** 参数数据为空而跳过的参数表数量。 */
    private final int skippedTableCount;
    /** 导入的参数总数。 */
    private final int importedParameterCount;

    /** 在写事务成功后构造本次统计。 */
    public BaselineDeviceSyncVO(int queried, int synced, int skipped, int imported) {
        // 参数表与设备数量分开统计，一张参数表可能包含多个设备工作表。
        this.queriedTableCount = queried;
        this.syncedDeviceCount = synced;
        this.skippedTableCount = skipped;
        this.importedParameterCount = imported;
    }

    /** 返回参数表数量。 */
    public int getQueriedTableCount() { return queriedTableCount; }
    /** 返回设备数量。 */
    public int getSyncedDeviceCount() { return syncedDeviceCount; }
    /** 返回跳过数量。 */
    public int getSkippedTableCount() { return skippedTableCount; }
    /** 返回参数数量。 */
    public int getImportedParameterCount() { return importedParameterCount; }
}
