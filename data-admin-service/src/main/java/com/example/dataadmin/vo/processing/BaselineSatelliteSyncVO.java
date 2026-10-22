package com.example.dataadmin.vo.processing;

/** 基线卫星参数同步统计。 */
public class BaselineSatelliteSyncVO {
    /** 基线实际返回的卫星数量。 */
    private final int queriedSatelliteCount;
    /** 成功导入参数的卫星数量。 */
    private final int syncedSatelliteCount;
    /** 没有遥测数据而跳过的卫星数量。 */
    private final int skippedSatelliteCount;
    /** 成功导入的参数总数。 */
    private final int importedParameterCount;

    /** 保存本次完整同步的统计结果。 */
    public BaselineSatelliteSyncVO(int queried, int synced, int skipped, int imported) {
        // 统一在事务成功后生成结果，避免返回未提交的统计。
        this.queriedSatelliteCount = queried;
        this.syncedSatelliteCount = synced;
        this.skippedSatelliteCount = skipped;
        this.importedParameterCount = imported;
    }

    /** 返回查询卫星数。 */
    public int getQueriedSatelliteCount() { return queriedSatelliteCount; }
    /** 返回同步卫星数。 */
    public int getSyncedSatelliteCount() { return syncedSatelliteCount; }
    /** 返回跳过卫星数。 */
    public int getSkippedSatelliteCount() { return skippedSatelliteCount; }
    /** 返回导入参数数。 */
    public int getImportedParameterCount() { return importedParameterCount; }
}
