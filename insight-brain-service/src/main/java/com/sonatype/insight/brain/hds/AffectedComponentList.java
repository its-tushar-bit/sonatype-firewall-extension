/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Wrapper for a list of components affected by a CVE vulnerability.
 * Used for proper JSON deserialization from HDS API response.
 */
public class AffectedComponentList
{
  private final List<AffectedComponentDTO> components;

  public AffectedComponentList() {
    this.components = new ArrayList<>();
  }

  @JsonCreator
  public AffectedComponentList(List<AffectedComponentDTO> components) {
    this.components = components != null ? components : new ArrayList<>();
  }

  @JsonValue
  public List<AffectedComponentDTO> getComponents() {
    return components;
  }
}
