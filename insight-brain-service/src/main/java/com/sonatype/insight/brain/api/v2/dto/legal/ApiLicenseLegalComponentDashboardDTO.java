/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTOV2;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.OwnerComponentLicensesDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ApiLicenseLegalComponentDashboardDTO
{
  public ApiLicenseLegalComponentDashboardDTO() {
    // for Jackson
  }

  public ApiLicenseLegalComponentDashboardDTO(OwnerComponent applicationComponent) {
    this.hash = applicationComponent.getHash();
    this.componentIdentifier = applicationComponent.getComponentIdentifier();
    this.displayName = componentIdentifier != null
        ? ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString()
        : applicationComponent.getPathnames().get(0);
  }

  public ApiLicenseLegalComponentDashboardDTO(OwnerComponentLicensesDTO applicationComponentLicensesDTO) {
    this.hash = applicationComponentLicensesDTO.getHash();
    this.componentIdentifier = applicationComponentLicensesDTO.getComponentIdentifier();
    this.displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
  }

  public String hash;

  @JsonIgnore
  public ComponentIdentifier componentIdentifier;

  public String displayName;

  public Set<ApiLicenseDTOV2> licenses = new HashSet<>();

  public int applicationOccurrences;

  public int reviewCompletedCount;

  public int reviewTotalCount;

  @JsonIgnore
  public LicenseObligationReviewStatus reviewStatus;
}
