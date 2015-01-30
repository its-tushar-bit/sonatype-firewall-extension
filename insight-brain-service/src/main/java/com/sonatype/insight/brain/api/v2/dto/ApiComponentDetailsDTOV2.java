/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.brain.api.v1.dto.ApiLicenseDataDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.utils.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @since 1.13.0
 */
public class ApiComponentDetailsDTOV2
{
  public ApiComponentDTOV2 component;

  public boolean proprietary;

  public String matchState;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date catalogDate;

  public ApiLicenseDataDTO licenseData;

  public ApiSecurityDataDTO securityData;

  public ApiComponentPolicyViolationListDTOV2 policyData;
}
