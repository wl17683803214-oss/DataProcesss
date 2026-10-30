package com.example.common.formula;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 统一公式协议：脚本接收原始字节，由公式决定解码方式，不开放任意Java访问。 */
public final class LuaFormulaEngine {
    /** 单个源码最多六十四千字节，避免导入超大脚本。 */
    public static final int MAX_SOURCE_BYTES = 65536;
    /** 单参数原码和处理参数均设置边界，避免异常配置占用过多资源。 */
    private static final int MAX_RAW_BYTES = 65536;
    /** 每次执行最多十万条指令。 */
    private static final int MAX_INSTRUCTIONS = 100000;

    /** 工具类禁止实例化。 */
    private LuaFormulaEngine() { }

    /** 只编译文本脚本；调用时创建独立环境，编译结果可安全共享。 */
    public static Compiled compile(String source) {
        if (source == null || source.getBytes(StandardCharsets.UTF_8).length > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("公式源码为空或超过64KB");
        }
        try {
            // 拒绝二进制字节码，只允许经语法检查的源码。
            Prototype prototype = LuaC.instance.compile(new ByteArrayInputStream(
                    source.getBytes(StandardCharsets.UTF_8)), "公式");
            validatePrototype(prototype, 0);
            Compiled compiled = new Compiled(prototype);
            LuaValue definition = definition(compiled, environment());
            compiled.code = definition.get("code").checkjstring();
            compiled.name = definition.get("name").checkjstring();
            if (!compiled.code.matches("[0-9]{1,20}") || compiled.name.trim().isEmpty()
                    || compiled.name.length() > 100) {
                throw new IllegalArgumentException("公式编号须为1到20位数字，名称须为1到100个字符");
            }
            return compiled;
        } catch (LimitExceeded error) {
            throw new IllegalArgumentException(error.getMessage());
        } catch (StackOverflowError error) {
            throw new IllegalArgumentException("公式嵌套层级过深");
        } catch (Exception exception) {
            throw new IllegalArgumentException("公式加载失败：" + exception.getMessage(), exception);
        }
    }

    /** 限制函数嵌套，禁止可在少量指令内指数扩张内存的字符串拼接。 */
    private static void validatePrototype(Prototype prototype, int depth) {
        if (depth > 32) { throw new IllegalArgumentException("公式函数嵌套不能超过32层"); }
        for (int instruction : prototype.code) {
            if (org.luaj.vm2.Lua.GET_OPCODE(instruction) == org.luaj.vm2.Lua.OP_CONCAT) {
                throw new IllegalArgumentException("公式不支持字符串拼接，请使用固定文本或转换工具");
            }
        }
        if (prototype.p != null) {
            for (Prototype child : prototype.p) { validatePrototype(child, depth + 1); }
        }
    }

    /** 每次调用使用独立变量及环境，避免上一帧或其他线程修改本帧结果。 */
    public static Result execute(Compiled compiled, byte[] raw, int bitWidth, String parameters) {
        if (compiled == null) {
            throw new IllegalArgumentException("未找到对应的公式脚本");
        }
        if (raw == null || raw.length > MAX_RAW_BYTES || bitWidth <= 0
                || raw.length != (bitWidth + 7L) / 8L) {
            throw new IllegalArgumentException("原码长度与位宽不一致，或原码超过64KB");
        }
        try {
            // 原码保持取位顺序，不预先转换整数；数组下标从1开始。
            Globals globals = environment();
            LuaTable input = new LuaTable();
            LuaTable bytes = new LuaTable();
            for (int index = 0; index < raw.length; index++) {
                bytes.set(index + 1, LuaValue.valueOf(raw[index] & 255));
            }
            input.set("rawBytes", bytes);
            input.set("bitWidth", LuaValue.valueOf(bitWidth));
            input.set("params", parameters(parameters));
            // 模块初始化和计算入口共享同一次指令及时间预算。
            LuaValue output = definition(compiled, globals).get("calculate").call(input);
            return result(output);
        } catch (LimitExceeded error) {
            throw new IllegalArgumentException(error.getMessage());
        } catch (StackOverflowError error) {
            throw new IllegalArgumentException("公式嵌套层级过深");
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("公式计算失败：" + exception.getMessage(), exception);
        }
    }

    /** 数值参数以十进制文本传递，精确运算工具不会先损失长整数精度。 */
    private static LuaTable parameters(String text) {
        LuaTable result = new LuaTable();
        if (text == null || text.trim().isEmpty()) { return result; }
        if (text.length() > 8192) { throw new IllegalArgumentException("处理参数超过8192个字符"); }
        String[] values = text.split("/", -1);
        if (values.length > 256) { throw new IllegalArgumentException("处理参数不能超过256个"); }
        for (int index = 0; index < values.length; index++) {
            // 空项不允许悄悄占用参数位置，默认值只在未传参数时生效。
            try {
                decimal(values[index].trim());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("第" + (index + 1) + "个处理参数必须为有效数值");
            }
            result.set(index + 1, LuaValue.valueOf(values[index].trim()));
        }
        return result;
    }

    /** 校验统一入口，导入时不要求所有公式能接受同一份测试原码。 */
    private static LuaValue definition(Compiled compiled, Globals globals) {
        LuaValue value = new LuaClosure(compiled.prototype, globals).call();
        if (!value.istable() || !value.get("calculate").isfunction()) {
            throw new IllegalArgumentException("脚本必须返回定义表，且包含calculate函数");
        }
        return value;
    }

    /** 只保留有限基础函数及数学库，不加载字符串、协程、文件、网络和Java反射能力。 */
    private static Globals environment() {
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new JseMathLib());
        globals.load(new Budget());
        Set<String> allowed = new HashSet<String>(Arrays.asList(
                "assert", "error", "tonumber", "tostring", "type", "ipairs", "pairs", "next", "select", "math"));
        for (LuaValue key : globals.keys()) {
            if (!allowed.contains(key.tojstring())) { globals.set(key, LuaValue.NIL); }
        }
        // 仅暴露显式注册的纯转换工具，不提供Java对象或类名入口。
        globals.set("tools", tools());
        globals.get("math").set("random", LuaValue.NIL);
        globals.get("math").set("randomseed", LuaValue.NIL);
        return globals;
    }

    /** 构造公式可使用的字节转换与精确计算函数。 */
    private static LuaTable tools() {
        LuaTable table = new LuaTable();
        table.set("unsigned", function(args -> {
            byte[] raw = bytes(args.arg1());
            if (order(args.arg(2)) == ByteOrder.LITTLE_ENDIAN) { reverse(raw); }
            return LuaValue.valueOf(new BigInteger(1, raw).toString());
        }));
        table.set("float32", function(args -> {
            byte[] raw = bytes(args.arg1());
            if (raw.length != 4) { throw new IllegalArgumentException("浮点原码必须是4字节"); }
            return finite(ByteBuffer.wrap(raw).order(order(args.arg(2))).getFloat());
        }));
        table.set("float64", function(args -> {
            byte[] raw = bytes(args.arg1());
            if (raw.length != 8) { throw new IllegalArgumentException("双精度原码必须是8字节"); }
            return finite(ByteBuffer.wrap(raw).order(order(args.arg(2))).getDouble());
        }));
        table.set("toFloat32", function(args -> finite((float) args.arg1().checkdouble())));
        table.set("linear", function(args -> {
            // 线性公式使用十进制运算，可精确处理无符号64位原码。
            BigDecimal value = decimal(args.arg1().checkjstring());
            BigDecimal a = decimal(args.arg(2).checkjstring());
            BigDecimal b = decimal(args.arg(3).checkjstring());
            BigDecimal result = value.multiply(a).add(b);
            return LuaValue.valueOf(result.toPlainString());
        }));
        table.set("utf8", function(args -> {
            try {
                // 严格检查编码，禁止无效字节被替换为不可追溯的字符。
                String text = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes(args.arg1()))).toString();
                return LuaValue.valueOf(text);
            } catch (CharacterCodingException exception) {
                throw new IllegalArgumentException("原码不是有效的UTF-8文本");
            }
        }));
        table.set("timeText", function(args -> {
            long ticks = decimal(args.arg1().checkjstring()).longValueExact();
            if (ticks < -9007199254740992L || ticks > 9007199254740992L) {
                throw new IllegalArgumentException("时间计数超出消息精确表示范围");
            }
            // 工具定义协议时间语义，只有脚本显式调用才采用此起点。
            String text = LocalDateTime.of(2000, 1, 1, 12, 0)
                    .plusSeconds(Math.floorDiv(ticks, 10000L))
                    .plusNanos(Math.floorMod(ticks, 10000L) * 100000L)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS"));
            return LuaValue.valueOf(text);
        }));
        return table;
    }

    /** 限制数值长度、精度及指数，避免异常科学计数法生成巨型字符串。 */
    private static BigDecimal decimal(String text) {
        if (text.length() > 512) { throw new IllegalArgumentException("数值文本过长"); }
        BigDecimal value = new BigDecimal(text);
        if (value.precision() > 340 || Math.abs((long) value.scale()) > 340) {
            throw new IllegalArgumentException("数值精度或指数超过允许范围");
        }
        return value;
    }

    /** 统一校验脚本输出，复用消息协议已有类型枚举。 */
    private static Result result(LuaValue output) {
        if (!output.istable()) { throw new IllegalArgumentException("公式结果必须是表"); }
        ValueType type;
        try {
            type = ValueType.valueOf("VALUE_TYPE_" + output.get("valueType").checkjstring());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("结果类型仅支持FLOAT、DOUBLE、TEXT、TIME、RAW");
        }
        if (type != ValueType.VALUE_TYPE_FLOAT && type != ValueType.VALUE_TYPE_DOUBLE
                && type != ValueType.VALUE_TYPE_TEXT && type != ValueType.VALUE_TYPE_TIME
                && type != ValueType.VALUE_TYPE_RAW) {
            throw new IllegalArgumentException("公式返回了不支持的结果类型");
        }
        String text = output.get("valueText").isnil() ? null : output.get("valueText").checkjstring();
        if (text != null && text.length() > MAX_RAW_BYTES) {
            throw new IllegalArgumentException("公式文本结果过长");
        }
        if (type == ValueType.VALUE_TYPE_TEXT) {
            if (text == null) { throw new IllegalArgumentException("文本公式必须返回valueText"); }
            return new Result(type, null, text);
        }
        BigDecimal value = decimal(output.get("value").checkjstring());
        if (!Double.isFinite(value.doubleValue())) { throw new IllegalArgumentException("公式数值超出有效范围"); }
        if (type == ValueType.VALUE_TYPE_FLOAT && !Float.isFinite(value.floatValue())) {
            throw new IllegalArgumentException("公式数值超出单精度范围");
        }
        if (type == ValueType.VALUE_TYPE_TIME) {
            value.longValueExact();
            if (value.abs().compareTo(new BigDecimal("9007199254740992")) > 0 || text == null) {
                throw new IllegalArgumentException("时间公式必须返回精确整数计数及valueText");
            }
        }
        return new Result(type, value, text);
    }

    /** 复制脚本字节表，禁止非整数、越界值和超长原码。 */
    private static byte[] bytes(LuaValue value) {
        LuaTable table = value.checktable();
        int length = table.length();
        if (length <= 0 || length > MAX_RAW_BYTES) { throw new IllegalArgumentException("原码字节数量无效"); }
        byte[] raw = new byte[length];
        for (int index = 0; index < length; index++) {
            double number = table.get(index + 1).checkdouble();
            if (number < 0 || number > 255 || number != Math.rint(number)) {
                throw new IllegalArgumentException("原码每个字节必须是0到255的整数");
            }
            raw[index] = (byte) number;
        }
        return raw;
    }

    /** 字节序必须显式指定，避免隐式默认值造成错误解码。 */
    private static ByteOrder order(LuaValue value) {
        String order = value.checkjstring();
        if ("LE".equals(order)) { return ByteOrder.LITTLE_ENDIAN; }
        if ("BE".equals(order)) { return ByteOrder.BIG_ENDIAN; }
        throw new IllegalArgumentException("字节序必须是LE或BE");
    }

    /** 只调整工具内部副本，传入公式的原始字节不变。 */
    private static void reverse(byte[] raw) {
        for (int left = 0, right = raw.length - 1; left < right; left++, right--) {
            byte value = raw[left];
            raw[left] = raw[right];
            raw[right] = value;
        }
    }

    /** 拒绝无穷大及非数值浮点结果。 */
    private static LuaValue finite(double value) {
        if (!Double.isFinite(value)) { throw new IllegalArgumentException("浮点原码或计算结果不是有限数值"); }
        return LuaValue.valueOf(value);
    }

    /** 用显式函数接口注册工具，避免自动暴露Java类。 */
    private interface Operation { LuaValue apply(Varargs args); }

    /** 包装有限的纯计算工具。 */
    private static VarArgFunction function(final Operation operation) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) { return operation.apply(args); }
        };
    }

    /** 每个环境独享预算，不使用异步取消，死循环会在执行线程内被终止。 */
    private static final class Budget extends DebugLib {
        private int instructions;
        private final long deadline = System.nanoTime() + 100000000L;

        /** 每条指令计数，周期检查一百毫秒预算。 */
        @Override
        public void onInstruction(int pc, Varargs values, int top) {
            if (++instructions > MAX_INSTRUCTIONS
                    || ((instructions & 255) == 0 && System.nanoTime() > deadline)) {
                throw new LimitExceeded("公式超过执行指令或时间限制");
            }
        }
    }

    /** 不使用可被Lua捕获后继续执行的普通异常表示预算耗尽。 */
    private static final class LimitExceeded extends Error {
        private LimitExceeded(String message) { super(message); }
    }

    /** 已编译公式只保存不可变字节码和导入时元数据。 */
    public static final class Compiled {
        private final Prototype prototype;
        private String code;
        private String name;
        private Compiled(Prototype prototype) { this.prototype = prototype; }
        /** 读取公式编号。 */
        public String getCode() { return code; }
        /** 读取公式名称。 */
        public String getName() { return name; }
    }

    /** Java使用的已校验结果，不包含脚本环境。 */
    public static final class Result {
        private final ValueType type;
        private final BigDecimal value;
        private final String text;
        private Result(ValueType type, BigDecimal value, String text) {
            this.type = type;
            this.value = value;
            this.text = text;
        }
        /** 读取已有消息类型枚举。 */
        public ValueType getType() { return type; }
        /** 读取精确数值。 */
        public BigDecimal getValue() { return value; }
        /** 读取展示文本。 */
        public String getText() { return text; }
    }
}
