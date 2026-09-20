package com.example.datasimulator.fep;

import com.example.datasimulator.fep.config.FepSimulatorConfig;
import com.example.datasimulator.fep.config.FepTransferType;
import com.example.datasimulator.fep.notify.FepDataProcessFlowNotifier;
import com.example.datasimulator.fep.sender.FepFileSender;
import com.example.datasimulator.fep.transport.FepTransport;
import com.example.datasimulator.fep.transport.TcpFepTransport;
import com.example.datasimulator.fep.transport.UdpFepTransport;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 文件交换模拟源独立启动入口。 */
public final class FepSimulatorApplication {

    /** 启动类不允许实例化。 */
    private FepSimulatorApplication() {
    }

    /** 根据配置的传输方式发送配置文件。 */
    public static void main(String[] args) {
        // 第一步：直接读取集中配置，启动时不再要求填写程序参数。
        FepTransferType transferType = FepSimulatorConfig.TRANSFER_TYPE;
        Path filePath = Paths.get(FepSimulatorConfig.FILE_PATH);
        FepDataProcessFlowNotifier flowNotifier =
                new FepDataProcessFlowNotifier();
        boolean collectionStarted = false;

        System.out.println("文件交换模拟源开始运行，传输方式："
                + transferType.getName() + "，文件：" + filePath);
        try {
            // 第二步：按传输方式通知数据处理侧启动对应FEP测试接收任务。
            if (FepSimulatorConfig.AUTO_START_COLLECTION) {
                flowNotifier.startCollection(transferType);
                collectionStarted = true;
            }

            // 第三步：网络连接建立后执行统一的文件交换发送流程。
            try (FepTransport transport = createTransport(transferType)) {
                FepFileSender sender = new FepFileSender(
                        transport,
                        FepSimulatorConfig.DATA_UNIT_LENGTH,
                        FepSimulatorConfig.SEND_INTERVAL_MILLIS);
                sender.send(filePath);
            }
        } catch (InterruptedException exception) {
            // 第四步：恢复中断标志，保证外部停止信号可以继续向上传递。
            Thread.currentThread().interrupt();
            System.err.println("文件发送被中断");
        } catch (Exception exception) {
            System.err.println("文件发送失败，原因：" + exception.getMessage());
        } finally {
            // 第五步：只有明确配置自动停止时才释放FEP测试接收任务。
            if (collectionStarted
                    && FepSimulatorConfig.AUTO_STOP_COLLECTION_AFTER_SEND) {
                try {
                    flowNotifier.stopCollection();
                } catch (Exception exception) {
                    System.err.println("数据处理侧FEP测试采集停止失败，原因："
                            + exception.getMessage());
                }
            }
        }
    }

    /** 根据配置创建对应的网络传输实现。 */
    private static FepTransport createTransport(
            FepTransferType transferType) throws Exception {
        // 这里只选择网络传输方式，文件交换协议流程不会重复实现。
        switch (transferType) {
            case TCP:
                return new TcpFepTransport(
                        FepSimulatorConfig.TARGET_HOST,
                        FepSimulatorConfig.TCP_TARGET_PORT,
                        FepSimulatorConfig.CONNECT_TIMEOUT_MILLIS,
                        FepSimulatorConfig.RESPONSE_TIMEOUT_MILLIS);
            case UDP:
                return new UdpFepTransport(
                        FepSimulatorConfig.TARGET_HOST,
                        FepSimulatorConfig.UDP_TARGET_PORT,
                        FepSimulatorConfig.RESPONSE_TIMEOUT_MILLIS);
            default:
                throw new IllegalArgumentException("不支持的传输方式");
        }
    }
}
