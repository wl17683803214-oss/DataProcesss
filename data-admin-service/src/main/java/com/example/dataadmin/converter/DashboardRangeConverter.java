package com.example.dataadmin.converter;

import com.example.dataadmin.enums.DashboardRange;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** 将总览接口中的3h、24h、7d参数转换为时间范围枚举。 */
@Component
public class DashboardRangeConverter implements Converter<String, DashboardRange> {

  @Override
  public DashboardRange convert(String source) {
    return DashboardRange.fromValue(source);
  }
}
