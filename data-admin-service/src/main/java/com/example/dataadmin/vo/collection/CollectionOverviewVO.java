package com.example.dataadmin.vo.collection;

/**
 * 数据采集页面顶部统计数据。
 */
public class CollectionOverviewVO {

    /** 采集接口总数。 */
    private Long interfaceTotal;

    /** 已启用接口数。 */
    private Long onlineTotal;

    /** 今日采集量。 */
    private Long todayCollectionCount;

    public Long getInterfaceTotal() {
        return interfaceTotal;
    }

    public void setInterfaceTotal(Long interfaceTotal) {
        this.interfaceTotal = interfaceTotal;
    }

    public Long getOnlineTotal() {
        return onlineTotal;
    }

    public void setOnlineTotal(Long onlineTotal) {
        this.onlineTotal = onlineTotal;
    }

    public Long getTodayCollectionCount() {
        return todayCollectionCount;
    }

    public void setTodayCollectionCount(Long todayCollectionCount) {
        this.todayCollectionCount = todayCollectionCount;
    }
}
