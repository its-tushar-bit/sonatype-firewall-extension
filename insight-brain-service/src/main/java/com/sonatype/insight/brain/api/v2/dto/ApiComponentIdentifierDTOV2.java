/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * @since 1.13.0
 */
public class ApiComponentIdentifierDTOV2
{
  private String format;

  private SortedMap<String, String> coordinates = new TreeMap<>();

  public ApiComponentIdentifierDTOV2() {
  }

  public ApiComponentIdentifierDTOV2(ComponentIdentifier componentIdentifier) {
    this.format = componentIdentifier.getFormat();
    this.coordinates.putAll(componentIdentifier.getCoordinates());
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getFormat() {
    return format;
  }

  public void setCoordinates(Map<String, String> coordinates) {
    this.coordinates.clear();
    this.coordinates.putAll(coordinates);
  }

  public SortedMap<String, String> getCoordinates() {
    return Collections.unmodifiableSortedMap(coordinates);
  }
}
