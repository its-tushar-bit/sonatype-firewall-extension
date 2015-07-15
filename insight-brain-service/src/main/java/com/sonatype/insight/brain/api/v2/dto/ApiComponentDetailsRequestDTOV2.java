/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.16.0
 */
public class ApiComponentDetailsRequestDTOV2
{
  public List<ApiComponentDTOV2> components = new ArrayList<>();
}
