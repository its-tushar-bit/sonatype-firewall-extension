/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayFieldValue;

/**
 * @since 1.13.0
 */
public class DisplayFieldValueBuilder
{
  private List<ComponentDisplayFieldValue> displayFieldValues = new ArrayList<>();

  public DisplayFieldValueBuilder addFieldAndValue(String field, String value) {
    displayFieldValues.add(new ComponentDisplayFieldValue(field, value));
    return this;
  }

  public DisplayFieldValueBuilder addValue(String value) {
    addFieldAndValue(null, value);
    return this;
  }

  public List<ComponentDisplayFieldValue> build() {
    return displayFieldValues;
  }
}
