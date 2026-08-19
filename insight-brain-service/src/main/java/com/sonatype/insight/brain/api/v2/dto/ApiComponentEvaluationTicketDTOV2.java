/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @since 1.13.0
 */
public class ApiComponentEvaluationTicketDTOV2
{
  public String resultId;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date submittedDate;

  public String applicationId;

  public String resultsUrl;
}
