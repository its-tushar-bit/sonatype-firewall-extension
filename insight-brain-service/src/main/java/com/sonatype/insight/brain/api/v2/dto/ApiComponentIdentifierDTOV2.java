/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.google.common.base.Objects;

/**
 * @since 1.13.0
 */
public class ApiComponentIdentifierDTOV2
{
  private String format;

  private SortedMap<String, String> coordinates = new TreeMap<>();

  public ApiComponentIdentifierDTOV2() {
  }

  private ApiComponentIdentifierDTOV2(ComponentIdentifier componentIdentifier) {
    this.format = componentIdentifier.getFormat();
    this.coordinates.putAll(componentIdentifier.getCoordinates());
  }

  public static ApiComponentIdentifierDTOV2 fromComponentIdentifier(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }
    return new ApiComponentIdentifierDTOV2(componentIdentifier);
  }

  public static ComponentIdentifier toComponentIdentifier(ApiComponentIdentifierDTOV2 dto) {
    return dto == null ? null : dto.toComponentIdentifier();
  }

  public ComponentIdentifier toComponentIdentifier() {
    return new ComponentIdentifier(getFormat(), getCoordinates());
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiComponentIdentifierDTOV2 that = (ApiComponentIdentifierDTOV2) o;
    return Objects.equal(format, that.format) &&
        Objects.equal(coordinates, that.coordinates);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(format, coordinates);
  }
}
