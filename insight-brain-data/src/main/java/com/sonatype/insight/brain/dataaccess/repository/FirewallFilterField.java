/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

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
  }
}
