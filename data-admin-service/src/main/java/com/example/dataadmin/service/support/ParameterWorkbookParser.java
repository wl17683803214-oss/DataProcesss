package com.example.dataadmin.service.support;

import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 读取Excel工作簿或制表符TXT，校验完成前不访问数据库。 */
@Component
public class ParameterWorkbookParser {
    /** 单个导入文件最大字节数。 */
    private static final int MAX_FILE_SIZE = 20 * 1024 * 1024;
    /** TXT兼容的GBK字符集。 */
    private static final Charset GBK = Charset.forName("GBK");
    /** 表头及对应的实体属性，避免按固定列位置读取。 */
    private static final String[][] COLUMNS = {
        {"序号", "tableIndex"},
        {"位宽", "bitWidth"},
        {"遥测名称", "telemetryName"},
        {"遥测代号", "telemetryCode"},
        {"公式类型", "formulaType"},
        {"处理公式", "formulaDesc"},
        {"处理参数", "processParam"},
        {"小数位数", "decimalPlaces"},
        {"是否报警", "alarmFlag"},
        {"正常值范围", "normalValue"},
        {"预警值范围", "warningValue"},
        {"状态跳变信息", "stateChangeInfo"},
        {"相关命令", "commandCode"},
        {"所属系统", "systemName"},
        {"控制波道", "controlChannel"},
        {"合并波道", "mergeChannelCount"},
        {"延时波道", "delayChannel"},
        {"存储遥测", "storeFlag"},
        {"校准公式", "calibrationFormula"}
    };

    /** 根据扩展名选择Excel或TXT解析方式。 */
    public Map<String, List<TelemetryParseRuleConfig>> parse(
            String taskId,
            String fileName,
            byte[] content) {
        // 第一步：统一校验文件名称和大小，失败时不能触发旧数据删除。
        validateContent(content);
        String safeFileName = safeFileName(fileName);
        String extension = extension(safeFileName);
        if ("txt".equals(extension)) {
            return parseText(taskId, safeFileName, content);
        }
        if ("xls".equals(extension) || "xlsx".equals(extension)) {
            return parseWorkbook(taskId, content);
        }
        throw new IllegalArgumentException("仅支持.xls、.xlsx或.txt格式的参数文件");
    }

    /** 解析Excel工作簿，返回按页签顺序排列的参数集合。 */
    private Map<String, List<TelemetryParseRuleConfig>> parseWorkbook(
            String taskId,
            byte[] content) {
        Map<String, List<TelemetryParseRuleConfig>> result =
                new LinkedHashMap<String, List<TelemetryParseRuleConfig>>();
        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(content))) {
            // 第一步：按显示格式读取文本，包括前导零；公式单元格只读取缓存结果。
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            formatter.setUseCachedValuesForFormulaCells(true);
            for (Sheet sheet : workbook) {
                String sheetName = sheet.getSheetName();
                Row header = sheet.getRow(sheet.getFirstRowNum());
                if (header == null) {
                    throw new IllegalArgumentException(
                            "工作表【" + sheetName + "】没有表头");
                }
                Map<String, Integer> indexes = workbookHeaders(
                        sheetName, header, formatter);
                List<TelemetryParseRuleConfig> rules =
                        new ArrayList<TelemetryParseRuleConfig>();
                Set<String> serials = new HashSet<String>();
                // 第二步：每一行转换为统一字符串集合，再复用公共字段校验。
                for (int rowIndex = header.getRowNum() + 1;
                        rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    List<String> values = new ArrayList<String>();
                    for (String[] column : COLUMNS) {
                        values.add(formatter.formatCellValue(
                                row.getCell(indexes.get(column[0]))));
                    }
                    addRule(taskId, values, rules, serials,
                            "工作表【" + sheetName + "】第"
                                    + (rowIndex + 1) + "行");
                }
                requireRules(sheetName, rules);
                result.put(sheetName, rules);
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "读取Excel失败，请确认文件为未加密的有效工作簿", exception);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Excel没有可导入的工作表");
        }
        return result;
    }

    /** 解析单设备或单卫星的制表符TXT文件。 */
    private Map<String, List<TelemetryParseRuleConfig>> parseText(
            String taskId,
            String fileName,
            byte[] content) {
        String deviceName = baseName(fileName);
        if (deviceName.length() > 100) {
            throw new IllegalArgumentException("TXT文件名不能超过100个字符");
        }
        List<TelemetryParseRuleConfig> rules =
                new ArrayList<TelemetryParseRuleConfig>();
        Set<String> serials = new HashSet<String>();
        try (BufferedReader reader = new BufferedReader(
                new StringReader(decodeText(content)))) {
            // 第一步：TXT第一行必须是十九列中文表头。
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("TXT文件没有表头");
            }
            List<String> headers = parseTabLine(headerLine, 1);
            Map<String, Integer> indexes = textHeaders(headers);
            String line;
            int lineNumber = 1;
            // 第二步：逐行解析制表符和双引号，空行直接忽略。
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> cells = parseTabLine(line, lineNumber);
                List<String> values = new ArrayList<String>();
                for (String[] column : COLUMNS) {
                    values.add(value(cells, indexes.get(column[0])));
                }
                addRule(taskId, values, rules, serials,
                        "TXT第" + lineNumber + "行");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("读取TXT文件失败", exception);
        }
        requireRules(deviceName, rules);
        Map<String, List<TelemetryParseRuleConfig>> result =
                new LinkedHashMap<String, List<TelemetryParseRuleConfig>>();
        result.put(deviceName, rules);
        return result;
    }

    /** 读取并校验Excel表头。 */
    private Map<String, Integer> workbookHeaders(
            String sheetName,
            Row header,
            DataFormatter formatter) {
        List<String> titles = new ArrayList<String>();
        for (Cell cell : header) {
            // 空白列也保留位置，确保字段索引与工作簿一致。
            while (titles.size() < cell.getColumnIndex()) {
                titles.add("");
            }
            titles.add(formatter.formatCellValue(cell));
        }
        return headers(titles, "工作表【" + sheetName + "】");
    }

    /** 读取并校验TXT表头，兼容旧文件最后一列的反斜线名称。 */
    private Map<String, Integer> textHeaders(List<String> titles) {
        if (titles.size() != COLUMNS.length) {
            throw new IllegalArgumentException(
                    "TXT表头必须包含19列，实际为" + titles.size() + "列");
        }
        // 旧TXT最后一列表头使用反斜线，按校准公式列处理。
        if ("\\".equals(titles.get(COLUMNS.length - 1).trim())) {
            titles.set(COLUMNS.length - 1, "校准公式");
        }
        return headers(titles, "TXT文件");
    }

    /** 建立中文表头到列号的映射。 */
    private Map<String, Integer> headers(
            List<String> titles,
            String sourceName) {
        Map<String, Integer> indexes = new HashMap<String, Integer>();
        for (int index = 0; index < titles.size(); index++) {
            String title = titles.get(index) == null
                    ? "" : titles.get(index).trim();
            if (indexes.put(title, index) != null && !title.isEmpty()) {
                throw new IllegalArgumentException(
                        sourceName + "表头重复：" + title);
            }
        }
        for (String[] column : COLUMNS) {
            if (!indexes.containsKey(column[0])) {
                throw new IllegalArgumentException(
                        sourceName + "缺少表头：" + column[0]);
            }
        }
        return indexes;
    }

    /** 把一行文本构造成参数实体并执行公共校验。 */
    private void addRule(
            String taskId,
            List<String> values,
            List<TelemetryParseRuleConfig> rules,
            Set<String> serials,
            String rowName) {
        TelemetryParseRuleConfig rule = new TelemetryParseRuleConfig();
        rule.setTaskId(taskId);
        BeanWrapper bean = new BeanWrapperImpl(rule);
        boolean populated = false;
        for (int index = 0; index < COLUMNS.length; index++) {
            String property = COLUMNS[index][1];
            String text = value(values, index).trim();
            // 范围横线以及旧TXT校准公式列的反斜线均表示空值。
            if ("normalValue".equals(property)
                    || "warningValue".equals(property)) {
                text = normalizeRangeValue(text);
            } else if ("calibrationFormula".equals(property)
                    && "\\".equals(text)) {
                text = "";
            }
            populated |= !text.isEmpty();
            bean.setPropertyValue(property, text.isEmpty() ? null : text);
        }
        if (!populated) {
            return;
        }
        try {
            validate(rule);
            if (!serials.add(rule.getTableIndex())) {
                throw new IllegalArgumentException("序号重复");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    rowName + "：" + exception.getMessage());
        }
        rules.add(rule);
    }

    /** 按制表符解析一行，同时支持双引号和连续空列。 */
    private List<String> parseTabLine(String line, int lineNumber) {
        List<String> result = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                // 引号内连续两个双引号表示一个普通双引号。
                if (quoted && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == '\t' && !quoted) {
                result.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException(
                    "TXT第" + lineNumber + "行双引号没有闭合");
        }
        result.add(value.toString());
        return result;
    }

    /** 优先按UTF-8严格解码，失败时兼容现有GBK参数文件。 */
    private String decodeText(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            return removeBom(text);
        } catch (CharacterCodingException ignored) {
            // 当前历史TXT使用GBK编码，UTF-8严格解码失败后再回退。
            return removeBom(new String(content, GBK));
        }
    }

    /** 删除文本开头的Unicode字节序标记。 */
    private String removeBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF'
                ? value.substring(1) : value;
    }

    /** 取得上传文件的安全文件名。 */
    private String safeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("无法识别导入文件名称");
        }
        String normalized = fileName.trim().replace('\\', '/');
        String result = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("无法识别导入文件名称");
        }
        return result;
    }

    /** 取得小写文件扩展名。 */
    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 || index == fileName.length() - 1
                ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /** 取得不包含扩展名的TXT设备卫星名称。 */
    private String baseName(String fileName) {
        int index = fileName.lastIndexOf('.');
        String result = (index <= 0
                ? fileName : fileName.substring(0, index)).trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("TXT文件名不能为空");
        }
        return result;
    }

    /** 读取可能不存在的文本列。 */
    private String value(List<String> values, Integer index) {
        return index == null || index < 0 || index >= values.size()
                || values.get(index) == null ? "" : values.get(index);
    }

    /** 校验导入文件内容大小。 */
    private void validateContent(byte[] content) {
        if (content == null || content.length == 0
                || content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "请选择不超过20MB的Excel或TXT文件");
        }
    }

    /** 检查一个设备或页签至少包含一条有效参数。 */
    private void requireRules(
            String sourceName,
            List<TelemetryParseRuleConfig> rules) {
        if (rules.isEmpty()) {
            throw new IllegalArgumentException(
                    "【" + sourceName + "】没有有效参数");
        }
    }

    /** 将范围字段中的常见横线占位符转换为空文本。 */
    private String normalizeRangeValue(String value) {
        // 只处理整个单元格为横线的情况，避免破坏负数和状态范围。
        if ("-".equals(value) || "—".equals(value) || "–".equals(value)) {
            return "";
        }
        return value;
    }

    /** 保存原始字符串，仅检查必填、长度和已有是否枚举。 */
    public void validate(TelemetryParseRuleConfig rule) {
        // 第一步：所有来源统一检查字段长度，避免导入和手工保存规则不同。
        BeanWrapper bean = new BeanWrapperImpl(rule);
        Set<String> longFields = new HashSet<String>(Arrays.asList(
                "formulaDesc", "processParam", "normalValue", "warningValue",
                "stateChangeInfo", "commandCode"));
        for (String[] column : COLUMNS) {
            String value = (String) bean.getPropertyValue(column[1]);
            if (value != null && !longFields.contains(column[1])
                    && value.length() > 100) {
                throw new IllegalArgumentException(
                        column[0] + "不能超过100个字符");
            }
        }
        for (String field : Arrays.asList(
                "tableIndex", "bitWidth", "telemetryName",
                "telemetryCode", "formulaType")) {
            String value = (String) bean.getPropertyValue(field);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "序号、位宽、遥测名称、遥测代号、公式类型不能为空");
            }
        }
        // 第二步：复用已有枚举，空白开关按否处理，其他内容保持文本原值。
        for (String field : Arrays.asList("alarmFlag", "storeFlag")) {
            String value = (String) bean.getPropertyValue(field);
            if (value == null || value.trim().isEmpty()) {
                bean.setPropertyValue(field, "0");
            } else if (EnumData.labelOf(
                    BusinessEnums.YesNo.values(), value) == null) {
                throw new IllegalArgumentException(
                        "是否报警和存储遥测只能填写0或1");
            }
        }
    }
}
