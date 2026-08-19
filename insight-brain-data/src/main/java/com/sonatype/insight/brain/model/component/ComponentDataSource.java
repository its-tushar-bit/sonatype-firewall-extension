/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class ComponentDataSource
{
  private final String id;

  private final String name;

  public static final ComponentDataSource LICENSE = new ComponentDataSource("license", "License");

  public static final ComponentDataSource IDENTITY = new ComponentDataSource("identity", "Identity");

  private static Map<String, ComponentDataSource> all =
      ImmutableMap.of("license", LICENSE, "identity", IDENTITY);

  public ComponentDataSource(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  public static ComponentDataSource getById(String id) {
    return all.get(id);
  }

  public static List<ComponentDataSource> getAll() {
    return new ArrayList<>(all.values());
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "ComponentDataSource{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
