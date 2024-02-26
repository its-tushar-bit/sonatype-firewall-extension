/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

import com.sonatype.insight.brain.model.configuration.CallFlowAlgorithm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiCallFlowAnalysisConfigDTO
{
  public String id;

  public Boolean enabled;

  public List<String> namespaces;

  @JsonInclude(Include.NON_EMPTY)
  public CallFlowAlgorithm algorithm;

  @JsonInclude(Include.NON_EMPTY)
  public Integer threadCount;

  public String ownerId;
}
