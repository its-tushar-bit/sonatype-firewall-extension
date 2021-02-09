/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.List;

import com.sonatype.insight.brain.model.legal.ObligationStatus;

public class ApiLicenseLegalObligationDTO
{
  public String name;

  public ObligationStatus status;

  public String comment;

  public List<ComponentObligationAttributionDTO> attributions;

  public ApiLicenseLegalObligationDTO() {
    // for jackson
  }

  public ApiLicenseLegalObligationDTO(
      String name,
      ObligationStatus status,
      String comment,
      List<ComponentObligationAttributionDTO> attributions)
  {
    this.name = name;
    this.status = status;
    this.comment = comment;
    this.attributions = attributions;
  }
}
