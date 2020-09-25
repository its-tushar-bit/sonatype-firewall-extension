/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IntegrityRating
{
  private static final Map<String, IntegrityRating> byId = new LinkedHashMap<>();

  private final String id;

  private final String name;

  static {
    byId.put("0", new IntegrityRating("0", "Normal"));
    byId.put("1", new IntegrityRating("1", "Suspicious"));
  }

  public IntegrityRating(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public static IntegrityRating getById(String id) {
    return byId.get(id);
  }

  public static List<IntegrityRating> getAll() {
    return new ArrayList<>(byId.values());
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "IntegrityRating{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
