/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @since 1.13.0
 */
public class ApiComponentEvaluationResultDTOV2
{
  @JsonSerialize(using = ISODateSerializer.class)
  public Date submittedDate;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date evaluationDate;

  public String applicationId;

  public List<ApiComponentDetailsDTOV2> results = new ArrayList<>();

  public boolean isError = false;

  public String errorMessage;
}
