package com.example.dataprocess.tool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 只计算数值、变量和四则运算，不执行脚本或方法调用。 */
public final class TelemetryFormulaEvaluator {
    /** 支持小数和科学计数法常量。 */
    private static final Pattern NUMBER = Pattern.compile(
            "(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");
    /** 除法使用足够覆盖双精度的十进制计算精度。 */
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    /** 工具类不允许实例化。 */
    private TelemetryFormulaEvaluator() {
    }

    /** 将解码值绑定到x，将斜线分隔的处理参数依次绑定到a至w。 */
    public static BigDecimal evaluate(String formula, String parameters, BigDecimal x) {
        String expression = formula == null ? "" : formula.trim();
        // 空值及导入表中的横线占位符表示无需计算。
        if (expression.isEmpty() || "-".equals(expression)
                || "—".equals(expression) || "–".equals(expression)) {
            return x;
        }
        expression = expression.replaceAll("\\s+", "");
        if (expression.startsWith("y=")) {
            expression = expression.substring(2);
        }
        Map<Character, BigDecimal> variables = new HashMap<Character, BigDecimal>();
        variables.put('x', x);
        // 参数分隔符只用于配置值，不影响公式中的除法符号。
        if (parameters != null && !parameters.trim().isEmpty()) {
            String[] values = parameters.split("/", -1);
            if (values.length > 23) {
                throw new IllegalArgumentException("处理参数数量不能超过23个");
            }
            for (int index = 0; index < values.length; index++) {
                variables.put((char) ('a' + index), new BigDecimal(values[index].trim()));
            }
        }
        Parser parser = new Parser(expression, variables);
        BigDecimal result = parser.expression();
        if (parser.position != expression.length()) {
            throw new IllegalArgumentException("处理公式存在无法识别的内容：" + formula);
        }
        return result;
    }

    /** 每次求值单独创建解析器，避免处理线程之间共享游标。 */
    private static final class Parser {
        /** 已移除空白的公式。 */
        private final String source;
        /** 当前公式可以引用的变量。 */
        private final Map<Character, BigDecimal> variables;
        /** 下一个待解析字符位置。 */
        private int position;

        /** 初始化当前公式及变量。 */
        private Parser(String source, Map<Character, BigDecimal> variables) {
            this.source = source;
            this.variables = variables;
        }

        /** 先完成乘除，再依次处理加减。 */
        private BigDecimal expression() {
            BigDecimal value = term();
            while (position < source.length()) {
                if (take('+')) {
                    value = value.add(term());
                } else if (take('-')) {
                    value = value.subtract(term());
                } else {
                    break;
                }
            }
            return value;
        }

        /** 同级乘除按从左到右的顺序计算。 */
        private BigDecimal term() {
            BigDecimal value = factor();
            while (position < source.length()) {
                if (take('*')) {
                    value = value.multiply(factor());
                } else if (take('/')) {
                    value = value.divide(factor(), PRECISION);
                } else {
                    break;
                }
            }
            return value;
        }

        /** 读取正负号、括号、变量或者数值常量。 */
        private BigDecimal factor() {
            if (take('+')) {
                return factor();
            }
            if (take('-')) {
                return factor().negate();
            }
            if (take('(')) {
                BigDecimal value = expression();
                if (!take(')')) {
                    throw new IllegalArgumentException("处理公式括号不匹配");
                }
                return value;
            }
            if (position < source.length() && variables.containsKey(source.charAt(position))) {
                return variables.get(source.charAt(position++));
            }
            Matcher matcher = NUMBER.matcher(source);
            matcher.region(position, source.length());
            if (matcher.lookingAt()) {
                position = matcher.end();
                return new BigDecimal(matcher.group());
            }
            throw new IllegalArgumentException("处理公式缺少操作数或变量未配置，位置：" + position);
        }

        /** 匹配指定符号并移动游标。 */
        private boolean take(char symbol) {
            if (position < source.length() && source.charAt(position) == symbol) {
                position++;
                return true;
            }
            return false;
        }
    }
}
