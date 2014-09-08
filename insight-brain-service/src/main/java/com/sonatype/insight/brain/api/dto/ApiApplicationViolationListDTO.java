/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiApplicationViolationListDTO
{
  public List<ApiApplicationViolationDTO> applicationViolations = new ArrayList<>();
}
