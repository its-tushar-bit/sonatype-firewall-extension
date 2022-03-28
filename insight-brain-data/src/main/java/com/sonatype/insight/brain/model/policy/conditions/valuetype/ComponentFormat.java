/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * Thin wrapper over {@link com.sonatype.clm.dto.model.component.ComponentIdentifier#getSupportedFormats()} only
 * to be able serialize it into json as key-value pairs (like: {"id":"maven","name":"maven"}), as expected by the
 * policy UI.
 */
public class ComponentFormat
{
  private static final List<ComponentFormat> all = new ArrayList<>();

  static {
    for (String format : ComponentIdentifier.getSupportedFormats()) {
      all.add(new ComponentFormat(format, format));
    }
  }

  private final String id;

  private final String name;

  private ComponentFormat(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public static List<ComponentFormat> getAll() {
    return Collections.unmodifiableList(all);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return id;
  }
}
