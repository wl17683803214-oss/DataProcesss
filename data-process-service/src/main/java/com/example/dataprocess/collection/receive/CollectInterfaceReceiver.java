package com.example.dataprocess.collection.receive;

/** 采集接口接收任务的统一定义。 */
public interface CollectInterfaceReceiver extends Runnable {

    /** 停止数据接收并释放当前接收器占用的网络资源。 */
    void stop();
}
