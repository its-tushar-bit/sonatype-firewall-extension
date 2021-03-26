/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.model.ApplicationComponent;

public class ApiLicenseLegalComponentDashboardDTO
{
  public ApiLicenseLegalComponentDashboardDTO() {
    // for Jackson
  }

  public ApiLicenseLegalComponentDashboardDTO(ApplicationComponent applicationComponent) {
    this.hash = applicationComponent.getHash();
    this.displayName = applicationComponent.getComponentIdentifier() != null
        ? ComponentDisplayNameUtil.fromIdentifier(applicationComponent.getComponentIdentifier()).toString()
        : applicationComponent.getPathnames().get(0);
  }

  public String hash;

  public String displayName;

  public Set<String> licenseNames = new HashSet<>();

  public int applicationOccurrences;

  public int reviewCompletedCount;

  public int reviewTotalCount;
}
