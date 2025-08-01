/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @since 1.13.0
 */
public class ApiComponentDetailsDTOV2
{
  public ApiComponentDTOV2 component;

  public String matchState;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date catalogDate;

  @Schema(nullable = true)
  public Integer relativePopularity;

  public ApiLicenseDataDTO licenseData;

  @Schema(nullable = true)
  public String integrityRating;

  @Schema(nullable = true)
  public String hygieneRating;

  public ApiSecurityDataDTO securityData;

  @JsonInclude(Include.NON_NULL)
  public ApiComponentPolicyViolationListDTOV2 policyData;

  /**
   * @since 1.100
   */
  @JsonInclude(Include.NON_NULL)
  public ApiComponentProjectDataDTO projectData;
}
