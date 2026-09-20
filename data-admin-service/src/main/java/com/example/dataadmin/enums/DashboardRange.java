package com.example.dataadmin.enums;

/** 总览接口支持的统计时间范围。 */
public enum DashboardRange implements LabeledEnum {

  THREE_HOURS("3h", "近3小时", 3, 5, false),
  TWENTY_FOUR_HOURS("24h", "近24小时", 24, 30, false),
  SEVEN_DAYS("7d", "近7天", 168, 240, true);

  private final String value;
  private final String label;
  private final int hours;
  private final int bucketMinutes;
  private final boolean dateLabel;

  DashboardRange(
      String value,
      String label,
      int hours,
      int bucketMinutes,
      boolean dateLabel) {
    this.value = value;
    this.label = label;
    this.hours = hours;
    this.bucketMinutes = bucketMinutes;
    this.dateLabel = dateLabel;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String getLabel() {
    return label;
  }

  public int getHours() {
    return hours;
  }

  public int getBucketMinutes() {
    return bucketMinutes;
  }

  public boolean isDateLabel() {
    return dateLabel;
  }

  /** 将接口参数值转换为对应的枚举。 */
  public static DashboardRange fromValue(String value) {
    for (DashboardRange range : values()) {
      if (range.value.equalsIgnoreCase(value)) {
        return range;
      }
    }
    throw new IllegalArgumentException("时间范围只能为3h、24h或7d");
  }
}
