package com.example.dataprocess.protocol.rpc;

import com.example.common.formula.LuaFormulaEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 接口加载时准备的本地解码快照，字段位置和公式版本在同一帧内保持一致。 */
public final class PdxpDecodePlan {
    public static final PdxpDecodePlan EMPTY = new PdxpDecodePlan(
            Collections.emptyList(), Collections.emptyMap(), Collections.emptyList(), 0L,
            "当前接口未加载有效的参数解析配置");

    private final List<PdxpProtocolField> sourceFields;
    private final Map<String, LuaFormulaEngine.Compiled> formulas;
    private final List<Field> fields;
    private final long requiredBits;
    private final String validationFailure;

    private PdxpDecodePlan(List<PdxpProtocolField> sourceFields, Map<String, LuaFormulaEngine.Compiled> formulas,
            List<Field> fields, long requiredBits, String validationFailure) {
        this.sourceFields = sourceFields;
        this.formulas = formulas;
        this.fields = fields;
        this.requiredBits = requiredBits;
        this.validationFailure = validationFailure;
    }

    /** 按原有序号规则提前校验、排序并计算位偏移。 */
    public static PdxpDecodePlan prepare(List<PdxpProtocolField> source,
            Map<String, LuaFormulaEngine.Compiled> scripts) {
        // 保留字段原顺序供配置访问，公式脚本与解码结果一起发布。
        List<PdxpProtocolField> sourceFields = source == null || source.isEmpty()
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(source));
        Map<String, LuaFormulaEngine.Compiled> formulas = scripts == null || scripts.isEmpty()
                ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(scripts));
        if (sourceFields.isEmpty()) {
            return formulas.isEmpty() ? EMPTY : new PdxpDecodePlan(sourceFields, formulas,
                    Collections.emptyList(), 0L, EMPTY.validationFailure);
        }

        // 配置错误继续由帧处理链路记录异常整帧，避免影响其他运行接口。
        Set<Integer> indexes = new HashSet<>();
        for (PdxpProtocolField field : sourceFields) {
            Integer index = field.getTableIndex();
            Integer width = field.getBitWidth();
            String code = field.getTelemetryCode();
            if (field.getId() == null || index == null || index < 0 || !indexes.add(index)
                    || width == null || width <= 0 || code == null || code.trim().isEmpty()) {
                return new PdxpDecodePlan(sourceFields, formulas, Collections.emptyList(), 0L,
                        "协议字段配置无效，序号：" + index);
            }
        }

        // 同一公式及同一处理参数复用准备结果，例如同一张表中大量重复的线性常量。
        List<PdxpProtocolField> sorted = new ArrayList<>(sourceFields);
        sorted.sort(Comparator.comparingInt(PdxpProtocolField::getTableIndex));
        Map<LuaFormulaEngine.Compiled, Map<String, LuaFormulaEngine.PreparedParameters>> prepared = new HashMap<>();
        List<Field> fields = new ArrayList<>(sorted.size());
        long offset = 0;
        for (PdxpProtocolField field : sorted) {
            String formulaCode = field.getFormulaType() == null ? "" : field.getFormulaType().trim();
            LuaFormulaEngine.Compiled formula = formulas.get(formulaCode);
            LuaFormulaEngine.PreparedParameters parameters = null;
            if (!formulaCode.isEmpty() && formula != null) {
                parameters = prepared.computeIfAbsent(formula, key -> new HashMap<>())
                        .computeIfAbsent(field.getProcessParam(), text -> LuaFormulaEngine.prepareParameters(formula, text));
            }
            fields.add(new Field(field, offset, field.getBitWidth(), field.getTelemetryCode().trim(),
                    formulaCode, parameters));
            offset += field.getBitWidth();
        }
        return new PdxpDecodePlan(sourceFields, formulas, Collections.unmodifiableList(fields), offset, null);
    }

    /** 每帧只检查可用位数，数据不足时报告第一个无法读取的字段。 */
    public List<Field> fieldsForPayload(int length) {
        if (validationFailure != null) {
            throw new IllegalArgumentException(validationFailure);
        }
        long availableBits = length * 8L;
        if (availableBits < requiredBits) {
            for (Field field : fields) {
                if (field.offset + field.width > availableBits) {
                    throw new IllegalArgumentException("参数位宽无效或遥测数据不足，位偏移："
                            + field.offset + "，位宽：" + field.width);
                }
            }
        }
        return fields;
    }

    /** 提供原字段及脚本给保留的配置访问入口。 */
    public List<PdxpProtocolField> getSourceFields() { return sourceFields; }
    public Map<String, LuaFormulaEngine.Compiled> getFormulas() { return formulas; }

    /** 单个字段的位布局及公式参数，帧处理只读取准备结果。 */
    public static final class Field {
        private final PdxpProtocolField config;
        private final long offset;
        private final int width;
        private final String telemetryCode;
        private final String formulaCode;
        private final LuaFormulaEngine.PreparedParameters parameters;

        private Field(PdxpProtocolField config, long offset, int width, String telemetryCode,
                String formulaCode, LuaFormulaEngine.PreparedParameters parameters) {
            this.config = config;
            this.offset = offset;
            this.width = width;
            this.telemetryCode = telemetryCode;
            this.formulaCode = formulaCode;
            this.parameters = parameters;
        }

        public PdxpProtocolField getConfig() { return config; }
        public long getOffset() { return offset; }
        public int getWidth() { return width; }
        public String getTelemetryCode() { return telemetryCode; }
        public String getFormulaCode() { return formulaCode; }
        public LuaFormulaEngine.PreparedParameters getParameters() { return parameters; }
    }
}
