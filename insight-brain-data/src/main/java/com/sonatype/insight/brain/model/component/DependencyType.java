/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DependencyType
{
  public static final DependencyType DIRECT = new DependencyType("direct", "Direct");

  public static final DependencyType TRANSITIVE = new DependencyType("transitive", "Transitive");

  public static final DependencyType INNER_SOURCE = new DependencyType("innersource", "InnerSource");

  private final String id;

  private final String name;

  private static final Map<String, DependencyType> byId = new LinkedHashMap<>();

  static {
    byId.put(DIRECT.getId(), DIRECT);
    byId.put(TRANSITIVE.getId(), TRANSITIVE);
    byId.put(INNER_SOURCE.getId(), INNER_SOURCE);
  }

  DependencyType(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  public static DependencyType getById(final String id) {
    return byId.get(id);
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public static List<DependencyType> getAll() {
    return Collections.unmodifiableList(new ArrayList<>(byId.values()));
  }

  @Override
  public String toString() {
    return "DependencyType{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
