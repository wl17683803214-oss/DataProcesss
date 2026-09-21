package com.example.datasimulator.pdxp;

import com.example.datasimulator.pdxp.config.PdxpSimulatorConfig;
import com.example.datasimulator.pdxp.file.FixedFrameFileReader;
import com.example.datasimulator.pdxp.notify.DataProcessFlowNotifier;
import com.example.datasimulator.pdxp.packet.PdxpPacketBuilder;
import com.example.datasimulator.pdxp.packet.SimulatorPacketBuilder;
import com.example.datasimulator.pdxp.sender.UdpSimulatorSender;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** UDP模拟数据源主程序。 */
public final class PdxpSimulatorApplication {

    /** 主程序类不允许实例化。 */
    private PdxpSimulatorApplication() {
    }

    /** 按文件顺序发送全部固定长度遥测帧。 */
    public static void main(String[] args) {
        // 第一步：命令行第一个参数可以临时覆盖配置类中的输入文件路径。
        Path filePath = args.length > 0
                ? Paths.get(args[0])
                : Paths.get(PdxpSimulatorConfig.FILE_PATH);
        SendProgress sendProgress = new SendProgress();
        boolean collectionStarted = false;
        DataProcessFlowNotifier flowNotifier =
                new DataProcessFlowNotifier();

        try {
            // 第二步：按照配置先通知数据处理侧启动测试采集流程。
            if (PdxpSimulatorConfig.AUTO_START_COLLECTION) {
                flowNotifier.startCollection();
                collectionStarted = true;
            }

            // 第三步：文件读取器和UDP套接字在发送结束后统一自动关闭。
            sendFile(filePath, sendProgress);
        } catch (InterruptedException exception) {
            // 恢复线程中断标志，确保外部停止请求不会被吞掉。
            Thread.currentThread().interrupt();
            System.err.println("模拟数据发送被中断，已发送帧数："
                    + sendProgress.getSentCount());
        } catch (Exception exception) {
            System.err.println("模拟数据发送失败，已发送帧数："
                    + sendProgress.getSentCount()
                    + "，原因：" + exception.getMessage());
        } finally {
            // 第四步：仅在明确开启自动停止时通知数据处理侧释放测试采集端口。
            if (collectionStarted
                    && PdxpSimulatorConfig.AUTO_STOP_COLLECTION_AFTER_SEND) {
                try {
                    flowNotifier.stopCollection();
                } catch (IOException exception) {
                    System.err.println("数据处理侧测试采集停止失败，原因："
                            + exception.getMessage());
                }
            }
        }
    }

    /** 读取模拟文件并逐帧发送UDP数据。 */
    private static void sendFile(
            Path filePath,
            SendProgress sendProgress) throws Exception {
        // 第一步：UDP套接字和报文构造器在所有文件轮次之间复用。
        try (UdpSimulatorSender sender = new UdpSimulatorSender(
                     PdxpSimulatorConfig.TARGET_HOST,
                     PdxpSimulatorConfig.TARGET_PORT)) {
            PdxpPacketBuilder pdxpPacketBuilder = new PdxpPacketBuilder();
            SimulatorPacketBuilder packetBuilder =
                    new SimulatorPacketBuilder(pdxpPacketBuilder);
            int completedRoundCount = 0;

            // 第二步：每轮重新打开文件，从第一帧开始顺序读取。
            while (true) {
                // 外部发出中断信号后及时结束循环发送。
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("模拟数据发送被中断");
                }
                int roundStartCount = sendProgress.getSentCount();
                try (FixedFrameFileReader fileReader =
                             new FixedFrameFileReader(
                                     filePath,
                                     PdxpSimulatorConfig.INPUT_FRAME_LENGTH)) {
                    // 第三步：每个固定长度遥测帧构造一个UDP报文并依次发送。
                    byte[] inputFrame;
                    while ((inputFrame = fileReader.readNextFrame()) != null) {
                        // 拆分原始遥测帧的四字节帧头和后续遥测原始数据。
                        byte[] telemetryHeader = Arrays.copyOfRange(
                                inputFrame,
                                0,
                                PdxpSimulatorConfig.TELEMETRY_HEADER_LENGTH);
                        byte[] telemetryData = Arrays.copyOfRange(
                                inputFrame,
                                PdxpSimulatorConfig.TELEMETRY_HEADER_LENGTH,
                                inputFrame.length);
                        // 发送前输出当前帧的遥测帧头和原始数据十六进制内容。
                        System.out.println("第"
                                + (sendProgress.getSentCount() + 1)
                                + "帧遥测帧头：" + toHex(telemetryHeader)
                                + "，遥测数据：" + toHex(telemetryData));
                        byte[] udpPacket = packetBuilder.build(inputFrame);
                        sender.send(udpPacket);
                        sendProgress.recordSentFrame();
                        System.out.println("第" + sendProgress.getSentCount()
                                + "帧发送成功，UDP报文长度："
                                + udpPacket.length);

                        // 第四步：每帧发送后按照配置间隔控制模拟数据速率。
                        if (PdxpSimulatorConfig.SEND_INTERVAL_MILLIS > 0) {
                            Thread.sleep(
                                    PdxpSimulatorConfig.SEND_INTERVAL_MILLIS);
                        }
                    }
                }

                // 第五步：空文件无法形成有效循环，直接提示并停止发送。
                int roundSentCount = sendProgress.getSentCount()
                        - roundStartCount;
                if (roundSentCount == 0) {
                    throw new IOException("模拟源文件中没有可发送的完整帧");
                }

                // 第六步：记录当前完整文件轮次及累计发送数量。
                completedRoundCount++;
                System.out.println("模拟源文件第" + completedRoundCount
                        + "轮发送完成，本轮发送帧数：" + roundSentCount
                        + "，累计发送帧数：" + sendProgress.getSentCount());

                // 第七步：未开启循环时保持原有行为，完成一轮后正常退出。
                if (!PdxpSimulatorConfig.LOOP_READ_ENABLED) {
                    System.out.println("模拟数据发送完成，成功发送帧数："
                            + sendProgress.getSentCount());
                    return;
                }
            }
        }
    }

    /** 保存本次模拟发送的实时进度。 */
    private static final class SendProgress {

        /** 已经成功发送的帧数。 */
        private int sentCount;

        /** 记录一帧已经成功发送。 */
        private void recordSentFrame() {
            // UDP发送完成后才增加计数，失败帧不计入成功数量。
            sentCount++;
        }

        /** 取得当前已经成功发送的帧数。 */
        private int getSentCount() {
            return sentCount;
        }
    }

    /** 将字节数组转换为连续的大写十六进制文本。 */
    private static String toHex(byte[] data) {
        // 第一步：预分配两倍字符长度，避免循环中反复扩容。
        StringBuilder builder = new StringBuilder(data.length * 2);
        // 第二步：逐字节转为两位大写十六进制字符。
        for (byte value : data) {
            builder.append(String.format("%02X", value & 0xFF));
        }
        // 第三步：返回不含空格的十六进制文本。
        return builder.toString();
    }
}
