/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.scan.HealthCheckReportSecurityRowDTO;

public class ThirdPartyHealthCheckReportSecurityRowDTO
    extends HealthCheckReportSecurityRowDTO
{
  public String fixedVersion;

  public String description;

  public String severity;

  public String ratingMethod;

  public String recommendations;

  public String advisories;

  public ThirdPartyHealthCheckReportSecurityRowDTO(
      final ComponentIdentifier componentIdentifier,
      final String hash)
  {
    super(componentIdentifier, hash);
  }

  //for jackson
  ThirdPartyHealthCheckReportSecurityRowDTO() {
    this(null, null);
  }
}
