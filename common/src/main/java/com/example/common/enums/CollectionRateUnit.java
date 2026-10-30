package com.example.common.enums;

import java.math.BigDecimal;

/** 采集速度的自动换算单位，原始速率统一使用千字节每秒。 */
public enum CollectionRateUnit {
  /** 小于一兆字节每秒时使用千字节单位。 */
  KILOBYTES("kilobytes_per_second", "KB/s", 1D),
  /** 达到一兆字节每秒时使用兆字节单位。 */
  MEGABYTES("megabytes_per_second", "MB/s", 1024D);

  /** 单位的内部英文标识。 */
  private final String value;
  /** 接口返回的标准单位标签。 */
  private final String label;
  /** 原始千字节速率转换到当前单位的除数。 */
  private final double divisor;

  /** 初始化单位标识、展示标签和换算比例。 */
  CollectionRateUnit(String value, String label, double divisor) {
    this.value = value;
    this.label = label;
    this.divisor = divisor;
  }

  /** 返回内部英文标识。 */
  public String getValue() { return value; }

  /** 返回标准单位符号，供调用方直接展示。 */
  public String getLabel() { return label; }

  /** 按未修约的原始速率选择单位，避免边界提前切换。 */
  public static CollectionRateUnit fromKilobytes(double rateKbps) {
    return rateKbps >= MEGABYTES.divisor ? MEGABYTES : KILOBYTES;
  }

  /** 按当前单位换算，并沿用总览保留两位小数的修约规则。 */
  public double convert(double rateKbps) {
    return Math.round(rateKbps / divisor * 100D) / 100D;
  }
  /** 修约后移除无意义的小数零，并在同一字段中拼接标准单位。 */
  public String format(double rateKbps) {
    // 复用单位换算，统一零值与整数速度的展示格式。
    String number = BigDecimal.valueOf(convert(rateKbps)).stripTrailingZeros().toPlainString();
    return number + " " + getLabel();
  }
  /** 按英文值查找已有速率单位。 */
  public static CollectionRateUnit require(String value) {
    for (CollectionRateUnit unit : values()) {
      if (unit.getValue().equals(value)) { return unit; }
    }
    throw new IllegalArgumentException("接收速率单位不正确");
  }

  /** 将限速值换算为每秒字节数，并校验数值范围。 */
  public long bytesPerSecond(BigDecimal limit) {
    if (limit == null || limit.signum() <= 0 || limit.compareTo(new BigDecimal("1000000")) > 0
        || limit.stripTrailingZeros().scale() > 3) {
      throw new IllegalArgumentException("接收速率上限必须大于0且不超过1000000，最多三位小数");
    }
    return limit.multiply(BigDecimal.valueOf(1024D * divisor))
        .setScale(0, java.math.RoundingMode.CEILING).longValueExact();
  }
}
