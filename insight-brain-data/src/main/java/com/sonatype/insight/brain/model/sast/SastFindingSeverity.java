/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.model.sast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SastFindingSeverity
{
  public static final SastFindingSeverity NONE = new SastFindingSeverity(0, "None");

  public static final SastFindingSeverity LOW = new SastFindingSeverity(1, "Low");

  public static final SastFindingSeverity MEDIUM = new SastFindingSeverity(2, "Medium");

  public static final SastFindingSeverity HIGH = new SastFindingSeverity(3, "High");

  public static final SastFindingSeverity CRITICAL = new SastFindingSeverity(4, "Critical");

  private static final Map<Integer, SastFindingSeverity> severityById = new LinkedHashMap<>();

  private static final Map<String, SastFindingSeverity> severityByName = new LinkedHashMap<>();

  static {
    severityById.put(NONE.getId(), NONE);
    severityById.put(LOW.getId(), LOW);
    severityById.put(MEDIUM.getId(), MEDIUM);
    severityById.put(HIGH.getId(), HIGH);
    severityById.put(CRITICAL.getId(), CRITICAL);

    severityByName.put(NONE.getName(), NONE);
    severityByName.put(LOW.getName(), LOW);
    severityByName.put(MEDIUM.getName(), MEDIUM);
    severityByName.put(HIGH.getName(), HIGH);
    severityByName.put(CRITICAL.getName(), CRITICAL);
  }

  private final int id;

  private final String name;

  private SastFindingSeverity(final int id, final String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public static SastFindingSeverity getById(final int id) {
    return severityById.get(id);
  }

  public static SastFindingSeverity getByName(final String name) {
    return severityByName.get(name);
  }

  public static List<SastFindingSeverity> getAll() {
    return new ArrayList<>(severityById.values());
  }

  @Override
  public String toString() {
    return "SastFindingSeverity{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
