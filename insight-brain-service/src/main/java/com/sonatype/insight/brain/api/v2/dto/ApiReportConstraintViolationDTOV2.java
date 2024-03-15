/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.64
 */
public class ApiReportConstraintViolationDTOV2
{
  public String constraintId;

  public String constraintName;

  public List<ApiReportConstraintConditionDTOV2> conditions = new ArrayList<>();
}
