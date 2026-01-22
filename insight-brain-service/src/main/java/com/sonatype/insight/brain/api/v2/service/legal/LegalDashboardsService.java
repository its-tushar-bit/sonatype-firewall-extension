/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2.service.legal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Named
public class LegalDashboardsService
{
  private final ApiLicenseLegalHdsService apiLicenseLegalHdsService;

  public static final String FLAGGEDCOUNT = "FLAGGEDCOUNT";

  public static final String ADDRESSEDCOUNT = "ADDRESSEDCOUNT";

  public static final String OPENCOUNT = "OPENCOUNT";

  @Inject
  public LegalDashboardsService(ApiLicenseLegalHdsService apiLicenseLegalHdsService) {
    this.apiLicenseLegalHdsService = apiLicenseLegalHdsService;
  }

  public Map<String, Integer> countObligations(List<ComponentObligation> obligations, Set<String> allObligationNames) {
    Map<String, Integer> countMap = new HashMap<>();
    countMap.put(FLAGGEDCOUNT, 0);
    countMap.put(ADDRESSEDCOUNT, 0);
    countMap.put(OPENCOUNT, 0);

    for (String obligationName : allObligationNames) {

      ObligationStatus status = obligations.stream()
          .filter(o -> o.getObligationName().equals(obligationName))
          .map(ComponentObligation::getStatus)
          .findFirst().orElse(ObligationStatus.OPEN);

      switch (status) {
        case FLAGGED:
          countMap.put(FLAGGEDCOUNT, countMap.get(FLAGGEDCOUNT) + 1);
          break;
        case FULFILLED:
        case IGNORED:
          countMap.put(ADDRESSEDCOUNT, countMap.get(ADDRESSEDCOUNT) + 1);
          break;
        default:
          countMap.put(OPENCOUNT, countMap.get(OPENCOUNT) + 1);
      }
    }
    return countMap;
  }

  public LicenseObligationReviewStatus getReviewStatus(
      int flaggedCount,
      int openCount,
      int addressedCount,
      Set<String> allObligationNames,
      Set<String> multiLicenseIds)
  {
    LicenseObligationReviewStatus reviewStatus = LicenseObligationReviewStatus.IN_PROGRESS;
    if (flaggedCount > 0) {
      reviewStatus = LicenseObligationReviewStatus.FLAGGED;
    }
    else if (isEmpty(allObligationNames)) {
      reviewStatus = isEmptyOrUnspecifiedLicenses(multiLicenseIds) ? LicenseObligationReviewStatus.UNREVIEWED
          : LicenseObligationReviewStatus.COMPLETED;
    }
    else if (openCount == allObligationNames.size()) {
      reviewStatus = LicenseObligationReviewStatus.UNREVIEWED;
    }
    else if (addressedCount >= allObligationNames.size()) {
      reviewStatus = LicenseObligationReviewStatus.COMPLETED;
    }
    return reviewStatus;
  }

  public Map<String, Set<String>> getLicenseObligationsFromHds(Set<String> licenseIds) {
    return licenseIds.isEmpty() ? Collections.emptyMap()
        : apiLicenseLegalHdsService.getLicenseMetadata(licenseIds).parallelStream()
            .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, //
                new TenantAwareFunction<LicenseMetadataDTO, Set<String>>(licenseMetadata -> licenseMetadata
                    .getLicenseObligations().stream().map(LicenseObligationDTO::getName).collect(Collectors.toSet()))));
  }

  private boolean isEmptyOrUnspecifiedLicenses(Set<String> licenseIds) {
    return isEmpty(licenseIds) || licenseIds.stream().allMatch(License::isEffectivelyUnspecified);
  }

  public Set<String> getLicenseIds(List<ApplicationComponentLicensesDTO> applicationComponents) {
    return applicationComponents.stream()
        .filter(applicationComponent -> isNotEmpty(applicationComponent.getLicenses()))
        .flatMap(applicationComponent -> applicationComponent.getLicenses().stream())
        .collect(Collectors.toSet());
  }
}
