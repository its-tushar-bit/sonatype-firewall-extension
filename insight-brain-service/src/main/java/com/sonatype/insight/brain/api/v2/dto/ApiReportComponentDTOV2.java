/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.cyclonedx.model.Swid;

/**
 *
 * @since 1.13.0
 */
public class ApiReportComponentDTOV2
    extends ApiComponentDTOV2
{
  public String matchState;

  // occurrences of component, in no particular order
  public List<String> pathnames = new ArrayList<>();

  public List<String> filenames = new ArrayList<>();

  public String identificationSource;

  public ApiLicenseDataDTOV2 licenseData;

  public ApiSecurityDataDTO securityData;

  @JsonInclude(Include.NON_NULL)
  public ApiDependencyDataDTO dependencyData;

  @JsonInclude(Include.NON_NULL)
  public ApiAiModelDataDTO aiModelData;

  public String cpe;

  public Swid swid;
}
