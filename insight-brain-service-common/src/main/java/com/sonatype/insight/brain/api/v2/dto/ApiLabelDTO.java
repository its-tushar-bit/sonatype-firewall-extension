/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiLabelDTO
{
  public String id;

  public String label;

  public String description;

  public String color;

  public String ownerId;

  public String ownerType;

  public ApiLabelDTO() {
  }

  public ApiLabelDTO(String label, String description, String color) {
    this.label = label;
    this.description = description;
    this.color = color;
  }
}
