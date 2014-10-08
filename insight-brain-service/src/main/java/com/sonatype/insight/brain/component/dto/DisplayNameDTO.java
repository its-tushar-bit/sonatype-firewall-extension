/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component.dto;

import java.util.List;

import com.sonatype.insight.brain.component.DisplayFieldValue;

/**
 * @since 1.13
 */
public class DisplayNameDTO
{
  public List<DisplayFieldValue> parts;

  public DisplayNameDTO() {
  }

  public DisplayNameDTO(final List<DisplayFieldValue> parts) {
    this.parts = parts;
  }
}
