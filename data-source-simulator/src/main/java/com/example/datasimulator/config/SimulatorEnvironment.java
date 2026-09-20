package com.example.datasimulator.config;

/** 模拟源运行环境配置读取工具。 */
public final class SimulatorEnvironment {

    /** 工具类不允许实例化。 */
    private SimulatorEnvironment() {
    }

    /** 读取文本环境变量，未配置时返回默认值。 */
    public static String text(String name, String defaultValue) {
        // 第一步：读取容器或操作系统提供的环境变量。
        String value = System.getenv(name);
        // 第二步：空值和空白值统一回退到代码默认配置。
        return value == null || value.trim().isEmpty()
                ? defaultValue
                : value.trim();
    }

    /** 读取整数环境变量，格式错误时明确阻止模拟源启动。 */
    public static int integer(String name, int defaultValue) {
        // 第一步：将默认值转换为文本并交给统一入口处理空值。
        String value = text(name, String.valueOf(defaultValue));
        try {
            // 第二步：返回解析后的整数配置。
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            // 第三步：保留环境变量名称，便于部署时快速定位错误配置。
            throw new IllegalArgumentException(
                    "环境变量" + name + "必须是整数，当前值：" + value,
                    exception);
        }
    }

    /** 读取长整数环境变量，格式错误时明确阻止模拟源启动。 */
    public static long longValue(String name, long defaultValue) {
        // 第一步：将默认值转换为文本并交给统一入口处理空值。
        String value = text(name, String.valueOf(defaultValue));
        try {
            // 第二步：返回解析后的长整数配置。
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            // 第三步：保留环境变量名称，便于部署时快速定位错误配置。
            throw new IllegalArgumentException(
                    "环境变量" + name + "必须是长整数，当前值：" + value,
                    exception);
        }
    }

    /** 读取布尔环境变量，只接受true或false。 */
    public static boolean bool(String name, boolean defaultValue) {
        // 第一步：读取并规范化布尔配置文本。
        String value = text(name, String.valueOf(defaultValue));
        // 第二步：只允许明确的布尔值，避免拼写错误被静默处理。
        if (!"true".equalsIgnoreCase(value)
                && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException(
                    "环境变量" + name + "必须是true或false，当前值：" + value);
        }
        // 第三步：返回经过校验的布尔配置。
        return Boolean.parseBoolean(value);
    }
}
