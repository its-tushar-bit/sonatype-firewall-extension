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

public class HygieneRating
{
  private final String id;

  private final String name;

  private static final Map<String, HygieneRating> byId = new LinkedHashMap<>();

  static {
    byId.put("1", new HygieneRating("1", "Exemplar"));
    byId.put("4", new HygieneRating("4", "Laggard"));
  }

  public HygieneRating(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public static HygieneRating getById(String id) {
    return byId.get(id);
  }

  public static List<HygieneRating> getAll() {
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
    return "HygieneRating{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
