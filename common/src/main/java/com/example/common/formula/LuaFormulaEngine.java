package com.example.common.formula;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import com.example.common.enums.FormulaParameterType;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 统一公式协议：脚本接收原始字节，由公式决定解码方式，不开放任意Java访问。 */
public final class LuaFormulaEngine {
    /** 单个源码最多六十四千字节，避免导入超大脚本。 */
    public static final int MAX_SOURCE_BYTES = 65536;
    /** 单参数原码和处理参数均设置边界，避免异常配置占用过多资源。 */
    private static final int MAX_RAW_BYTES = 65536;
    /** 每次执行最多十万条指令。 */
    private static final int MAX_INSTRUCTIONS = 100000;
    /** 每个线程最多缓存三十二个公式环境，超出后淘汰最久未使用的环境。 */
    private static final int MAX_PREPARED_FORMULAS = 32;
    /** 业务公式只计算当前输入；环境按线程隔离，不在多个线程间共享可变状态。 */
    private static final ThreadLocal<PreparedFormulaCache> PREPARED =
            ThreadLocal.withInitial(PreparedFormulaCache::new);

    /** 工具类禁止实例化。 */
    private LuaFormulaEngine() { }

    /** 编译并校验文本脚本，执行环境在各处理线程首次调用时创建。 */
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
            // 声明在计算调用之前读取，旧脚本未声明时仍全部使用数值参数。
            compiled.parameterTypes = parameterTypes(definition.get("parameterTypes"));
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

    /** 复用当前线程的公式环境，每次调用重新构造输入并重置执行预算。 */
    public static Result execute(Compiled compiled, byte[] raw, int bitWidth, String parameters) {
        // 保留原有调用入口，普通脚本不需要构造关联遥测上下文。
        return execute(compiled, raw, bitWidth, parameters, Collections.emptyMap());
    }

    /** 关联上下文只来自调用方的当前帧，不查询数据库、不使用上帧值。 */
    public static Result execute(Compiled compiled, byte[] raw, int bitWidth, String parameters,
            Map<String, byte[]> telemetryRawData) {
        return execute(compiled, raw, bitWidth, parameters, null, telemetryRawData);
    }

    /** 复用接口加载时已解析的参数，每次仍按当前帧提供关联原码。 */
    public static Result executePrepared(PreparedParameters parameters, byte[] raw, int bitWidth,
            Map<String, byte[]> telemetryRawData) {
        return execute(parameters.compiled, raw, bitWidth, null, parameters, telemetryRawData);
    }

    private static Result execute(Compiled compiled, byte[] raw, int bitWidth, String text,
            PreparedParameters parameters, Map<String, byte[]> telemetryRawData) {
        if (compiled == null) {
            throw new IllegalArgumentException("未找到对应的公式脚本");
        }
        if (raw == null || raw.length > MAX_RAW_BYTES || bitWidth <= 0
                || raw.length != (bitWidth + 7L) / 8L) {
            throw new IllegalArgumentException("原码长度与位宽不一致，或原码超过64KB");
        }
        boolean completed = false;
        try {
            // 第一步：按公式编号及编译实例选择环境，并开始本次执行预算。
            PreparedFormula prepared = prepare(compiled);
            // 第二步：原码保持取位顺序，每次创建新的输入表，数组下标从1开始。
            LuaTable input = new LuaTable();
            LuaTable bytes = new LuaTable();
            for (int index = 0; index < raw.length; index++) {
                bytes.set(index + 1, LuaValue.valueOf(raw[index] & 255));
            }
            input.set("rawBytes", bytes);
            input.set("bitWidth", LuaValue.valueOf(bitWidth));
            // 离线调用仍接受原字符串，实时处理直接复用准备好的常量和引用位置。
            PreparedParameters arguments = parameters == null ? prepareParameters(compiled, text) : parameters;
            input.set("params", parameterTable(arguments, telemetryRawData));
            // 第三步：执行缓存的计算入口；首次模块初始化与计算共享执行预算。
            Result value = result(prepared.calculate.call(input));
            completed = true;
            return value;
        } catch (LimitExceeded error) {
            throw new IllegalArgumentException(error.getMessage());
        } catch (StackOverflowError error) {
            throw new IllegalArgumentException("公式嵌套层级过深");
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("公式计算失败：" + exception.getMessage(), exception);
        } finally {
            // 第四步：异常可能留下脚本调用栈，只丢弃本公式环境，其他公式继续复用。
            if (!completed) {
                PREPARED.get().remove(compiled.code);
            }
        }
    }

    /** 同一线程按最近使用顺序缓存公式，新编译实例替换同编号的旧环境。 */
    private static PreparedFormula prepare(Compiled compiled) {
        // 第一步：编译实例一致才复用，避免配置更新后执行旧版本公式。
        PreparedFormulaCache cache = PREPARED.get();
        PreparedFormula prepared = cache.get(compiled.code);
        if (prepared != null && prepared.compiled == compiled) {
            ((Budget) prepared.globals.debuglib).reset();
            return prepared;
        }
        // 第二步：先释放同编号旧环境，新版本初始化失败也不回退到旧脚本。
        cache.remove(compiled.code);
        Globals globals = environment();
        ((Budget) globals.debuglib).reset();
        LuaValue calculate = definition(compiled, globals).get("calculate");
        // 第三步：完整初始化成功后加入缓存，超过容量时自动淘汰最久未使用项。
        prepared = new PreparedFormula(compiled, globals, calculate);
        cache.put(compiled.code, prepared);
        return prepared;
    }

    /** 每个线程独占有界缓存，公式重新编译不会以新键累积同编号旧版本。 */
    private static final class PreparedFormulaCache extends LinkedHashMap<String, PreparedFormula> {
        /** 使用访问顺序维护最近调用的公式，不按导入时间淘汰。 */
        private PreparedFormulaCache() {
            super(MAX_PREPARED_FORMULAS, 0.75f, true);
        }

        /** 仅限制环境数量，被淘汰的公式下次调用时仍可重新创建。 */
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PreparedFormula> eldest) {
            return size() > MAX_PREPARED_FORMULAS;
        }
    }

    /** 保存单个处理线程专用的公式实例。 */
    private static final class PreparedFormula {
        private final Compiled compiled;
        private final Globals globals;
        private final LuaValue calculate;

        private PreparedFormula(Compiled compiled, Globals globals, LuaValue calculate) {
            this.compiled = compiled;
            this.globals = globals;
            this.calculate = calculate;
        }
    }

    /** 校验声明必须是连续数组，未知类型、空数组及稀疏下标均拒绝导入。 */
    private static FormulaParameterType[] parameterTypes(LuaValue declaration) {
        if (declaration.isnil()) { return new FormulaParameterType[0]; }
        if (!declaration.istable()) {
            throw new IllegalArgumentException("参数类型声明parameterTypes必须是数组");
        }
        LuaTable table = declaration.checktable();
        int length = table.length();
        if (length < 1 || length > 256 || table.keys().length != length) {
            throw new IllegalArgumentException("参数类型声明必须包含1到256个连续数组项");
        }
        for (LuaValue key : table.keys()) {
            if (!key.isint() || key.toint() < 1 || key.toint() > length) {
                throw new IllegalArgumentException("参数类型声明必须从下标1开始连续填写");
            }
        }
        FormulaParameterType[] types = new FormulaParameterType[length];
        for (int index = 0; index < length; index++) {
            LuaValue value = table.get(index + 1);
            if (value.type() != LuaValue.TSTRING) {
                throw new IllegalArgumentException("参数类型声明的每一项必须是类型字符串");
            }
            types[index] = FormulaParameterType.require(value.checkjstring());
        }
        return types;
    }

    /** 预先拆分处理参数，保留位置及格式，错误留给单参数的现有失败处理。 */
    public static PreparedParameters prepareParameters(Compiled compiled, String text) {
        String configuration = text == null ? "" : text.trim();
        if (configuration.length() > 8192) {
            return new PreparedParameters(compiled, null, "处理参数超过8192个字符");
        }
        String[] values = configuration.isEmpty() ? new String[0] : configuration.split("/", -1);
        if (values.length > 256) {
            return new PreparedParameters(compiled, null, "处理参数不能超过256个");
        }
        PreparedParameter[] arguments = new PreparedParameter[Math.max(values.length, compiled.parameterTypes.length)];
        for (int index = 0; index < arguments.length; index++) {
            FormulaParameterType type = index < compiled.parameterTypes.length
                    ? compiled.parameterTypes[index] : FormulaParameterType.NUMBER;
            String value = index < values.length ? values[index].trim() : null;
            if (type == FormulaParameterType.TELEMETRY_RAW) {
                // 引用只固定代号，取值和原码有效性随帧判断。
                arguments[index] = value == null || value.isEmpty()
                        ? new PreparedParameter(null, null, "第" + (index + 1) + "个处理参数必须填写关联遥测代号")
                        : new PreparedParameter(null, value, null);
            } else if (value != null) {
                // 数字文本保持原样，Lua仍能使用原字符串和原有默认值。
                try {
                    decimal(value);
                    arguments[index] = new PreparedParameter(LuaValue.valueOf(value), null, null);
                } catch (RuntimeException exception) {
                    arguments[index] = new PreparedParameter(null, null, invalidNumericParameter(index, type));
                }
            }
        }
        return new PreparedParameters(compiled, arguments, null);
    }

    /** 按原顺序校验当帧引用并填充新表，脚本对表的修改不会传到下一次。 */
    private static LuaTable parameterTable(PreparedParameters parameters, Map<String, byte[]> telemetryRawData) {
        if (parameters.failure != null) {
            throw new IllegalArgumentException(parameters.failure);
        }
        LuaTable result = new LuaTable();
        for (int index = 0; index < parameters.arguments.length; index++) {
            PreparedParameter argument = parameters.arguments[index];
            if (argument == null) {
                continue;
            }
            if (argument.failure != null) {
                throw new IllegalArgumentException(argument.failure);
            }
            LuaValue value = argument.constant;
            if (argument.telemetryCode != null) {
                value = referencedParameter(index, argument.telemetryCode, telemetryRawData);
            }
            result.set(index + 1, value);
        }
        return result;
    }

    /** 关联原码只读当前帧，校验规则与原来的数字参数一致。 */
    private static LuaValue referencedParameter(int index, String code, Map<String, byte[]> telemetryRawData) {
        byte[] raw = telemetryRawData == null ? null : telemetryRawData.get(code);
        if (raw == null || raw.length == 0) {
            throw new IllegalArgumentException("第" + (index + 1) + "个参数引用的遥测在当前帧没有原码：" + code);
        }
        if (raw.length > MAX_RAW_BYTES) {
            throw new IllegalArgumentException("关联遥测原码超过64KB：" + code);
        }
        String value = unsigned(raw, ByteOrder.LITTLE_ENDIAN);
        try {
            decimal(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(invalidNumericParameter(index, FormulaParameterType.TELEMETRY_RAW));
        }
        return LuaValue.valueOf(value);
    }

    /** 保留处理参数无效时的原有位置和类型提示。 */
    private static String invalidNumericParameter(int index, FormulaParameterType type) {
        return "第" + (index + 1) + "个处理参数的" + type.getLabel() + "必须为有效且精度允许的数值";
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
            return LuaValue.valueOf(unsigned(raw, order(args.arg(2))));
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

    /** 共享整数解码逻辑，仅反转副本，当前帧其他参数仍保留原始字节。 */
    private static String unsigned(byte[] raw, ByteOrder order) {
        byte[] value = raw.clone();
        if (order == ByteOrder.LITTLE_ENDIAN) { reverse(value); }
        return new BigInteger(1, value).toString();
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
        private long deadline = System.nanoTime() + 100000000L;

        /** 重新开始单次计算的指令和时间预算。 */
        private void reset() {
            instructions = 0;
            deadline = System.nanoTime() + 100000000L;
        }

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

    /** 已编译公式保存可跨线程共享的字节码和导入时元数据。 */
    public static final class Compiled {
        private final Prototype prototype;
        private String code;
        private String name;
        /** 编译时校验后的参数类型，只在Java内使用，不暴露可变脚本定义。 */
        private FormulaParameterType[] parameterTypes;
        private Compiled(Prototype prototype) { this.prototype = prototype; }
        /** 读取公式编号。 */
        public String getCode() { return code; }
        /** 读取公式名称。 */
        public String getName() { return name; }
    }

    /** 可跨帧共享的公式入参配置，既不保存当前帧数据也不保存可变Lua表。 */
    public static final class PreparedParameters {
        private final Compiled compiled;
        private final PreparedParameter[] arguments;
        private final String failure;

        private PreparedParameters(Compiled compiled, PreparedParameter[] arguments, String failure) {
            this.compiled = compiled;
            this.arguments = arguments;
            this.failure = failure;
        }
    }

    /** 单个入参可以是十进制常量、当前帧引用或待报告的配置错误。 */
    private static final class PreparedParameter {
        private final LuaValue constant;
        private final String telemetryCode;
        private final String failure;

        private PreparedParameter(LuaValue constant, String telemetryCode, String failure) {
            this.constant = constant;
            this.telemetryCode = telemetryCode;
            this.failure = failure;
        }
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
