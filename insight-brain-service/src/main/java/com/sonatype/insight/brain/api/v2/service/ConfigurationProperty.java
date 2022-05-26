/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ConfigurationProperty
{
  protected static final ConfigurationProperty[] PROPERTIES = new ConfigurationProperty[]{
      new ConfigurationProperty(SystemConfigurationProperty.BASE_URL, String.class, (tx, s) -> s,
          (tx, o) -> ConfigurationUtils.urlValueToString(o)),
      new ConfigurationProperty(SystemConfigurationProperty.FORCE_BASE_URL, Boolean.class,
          (tx, s) -> Boolean.parseBoolean(s),
          ConfigurationUtils::forceBaseUrlToString)
  };

  protected static final Map<String, ConfigurationProperty> PROPERTY_BY_NAME = Arrays.stream(PROPERTIES).collect(
      Collectors.toMap(ConfigurationProperty::getName, Function.identity()));

  private final String name;

  private final Class<?> type;

  private final BiFunction<TransactionContext, String, Object> stringToValue;

  private final BiFunction<TransactionContext, Object, String> valueToString;

  public ConfigurationProperty(
      final String name,
      final Class<?> type,
      final BiFunction<TransactionContext, String, Object> stringToValue,
      final BiFunction<TransactionContext, Object, String> valueToString)
  {
    this.name = name;
    this.type = type;
    this.stringToValue = stringToValue;
    this.valueToString = valueToString;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  public BiFunction<TransactionContext, String, Object> getStringToValue() {
    return stringToValue;
  }

  public BiFunction<TransactionContext, Object, String> getValueToString() {
    return valueToString;
  }
}
