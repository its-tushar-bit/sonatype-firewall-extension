/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
* @since 1.13.0
*/
public class DisplayFieldValue
{
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
  private String field;

  private String value;

  public String getField() {
    return field;
  }

  public void setField(final String field) {
    this.field = field;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

  // Default constructor
  public DisplayFieldValue() {}

  public DisplayFieldValue(String field, String value) {
    this.field = field;
    this.value = value;
  }
}
