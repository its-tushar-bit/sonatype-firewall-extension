/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class FirewallFilterField
{
  public static final String MULTI_VALUE_SEPARATOR = ",";

  private final FirewallFilterableField field;

  private final String value;

  public FirewallFilterField(final FirewallFilterableField field, final String value) {
    this.field = field;
    this.value = value;
  }

  public FirewallFilterField(final FirewallFilterableField field, final Set<String> values) {
    this.field = field;
    this.value = String.join(MULTI_VALUE_SEPARATOR, values);
  }

  public FirewallFilterField(final FirewallFilterableField field, final Object value) {
    this.field = field;
    if (value instanceof Collection<?>) {
      this.value = String.join(MULTI_VALUE_SEPARATOR, (Set<String>) value);
    }
    else {
      this.value = Optional.ofNullable(value).map(String::valueOf).orElse(null);
    }
  }

  public FirewallFilterableField getField() {
    return field;
  }

  public String getValue() {
    return value;
  }

  public enum FirewallFilterableField
  {
    POLICY_ID,
    COMPONENT_NAME,
    REPOSITORY_PUBLIC_ID,
    QUARANTINE_TIME,
  }
}
