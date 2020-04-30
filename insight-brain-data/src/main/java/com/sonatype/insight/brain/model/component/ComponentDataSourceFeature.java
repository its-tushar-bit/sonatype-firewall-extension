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

public class ComponentDataSourceFeature
{
  private final String id;

  private final String name;

  private static Map<String, ComponentDataSourceFeature> all = ImmutableMap.of(
      "license", new ComponentDataSourceFeature("license", "License"),
      "identity", new ComponentDataSourceFeature("identity", "Identity")
  );

  public ComponentDataSourceFeature(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  public static ComponentDataSourceFeature getById(String id) {
    return all.get(id);
  }

  public static List<ComponentDataSourceFeature> getAll() {
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
    return "ComponentDataSourceFeature{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
