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

  public static final IntegrityRating NORMAL = new IntegrityRating("0", "Normal");

  public static final IntegrityRating SUSPICIOUS = new IntegrityRating("1", "Suspicious");

  public static final IntegrityRating PENDING = new IntegrityRating("2", "Pending");

  public static final IntegrityRating NOT_APPLICABLE = new IntegrityRating("3", "Not Applicable");

  private final String id;

  private final String name;

  static {
    byId.put(NORMAL.getId(), NORMAL);
    byId.put(SUSPICIOUS.getId(), SUSPICIOUS);
    byId.put(PENDING.getId(), PENDING);
    byId.put(NOT_APPLICABLE.getId(), NOT_APPLICABLE);
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
