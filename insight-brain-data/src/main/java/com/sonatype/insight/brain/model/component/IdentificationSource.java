/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The source system that identified a component.
 * 
 * @since 1.4.1
 */
public class IdentificationSource
{
  private static final Map<String, IdentificationSource> byId = new LinkedHashMap<>();

  private static final List<IdentificationSource> all = new ArrayList<>();

  // Note: The order the values are defined here determines the order they are displayed in the UI
  public static final IdentificationSource SONATYPE = new IdentificationSource("Sonatype", "Sonatype");

  public static final IdentificationSource MANUAL = new IdentificationSource("Manual", "Manual");

  public static final IdentificationSource CLAIR = new IdentificationSource("Clair", "Clair");

  private static final List<String> CLM_IDENTIFICATION_SOURCES = Arrays.asList(MANUAL.getId(), SONATYPE.getId());

  private final String id;

  private final String name;

  private IdentificationSource(String id, String name) {
    this.id = id;
    this.name = name;
    byId.put(id, this);
    all.add(this);
  }

  public static IdentificationSource getById(String id) {
    if (id == null) {
      return SONATYPE;
    }
    return byId.get(id);
  }

  /**
   * Returns an identification source if the id is existing/known or else will create a new one for the provided id
   */
  public static IdentificationSource getOrMake(String id) {
    final IdentificationSource resolved = getById(id);
    return resolved != null ? resolved : new IdentificationSource(id, id);
  }

  public static List<IdentificationSource> getAll() {
    return Collections.unmodifiableList(all);
  }

  public static boolean isThirdPartyIdentificationSource(final String identificationSource) {
    return identificationSource != null && !CLM_IDENTIFICATION_SOURCES.contains(identificationSource);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getId();
  }
}
