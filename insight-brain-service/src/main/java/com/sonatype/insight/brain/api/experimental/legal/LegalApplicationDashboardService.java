/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalApplicationComponentsFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Provides legal information for the application dashboard.
 *
 * @since 1.108
 */
@Named
public class LegalApplicationDashboardService
{
  private static final Logger log = LoggerFactory.getLogger(LegalApplicationDashboardService.class);

  private final ProductLicense productLicense;

  private final ApiLicenseLegalHdsService apiLicenseLegalHdsService;

  private final ApplicationDAO applicationDAO;

  private final ApplicationComponentLicenseDAO applicationComponentLicenseDAO;

  private final ComponentObligationDAO componentObligationDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  @Inject
  public LegalApplicationDashboardService(
      ProductLicense productLicense,
      ApiLicenseLegalHdsService apiLicenseLegalHdsService,
      ApplicationDAO applicationDAO,
      ApplicationComponentLicenseDAO applicationComponentLicenseDAO,
      ComponentObligationDAO componentObligationDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      MultiLicenseDAO multiLicenseDAO)
  {
    this.productLicense = productLicense;
    this.apiLicenseLegalHdsService = apiLicenseLegalHdsService;
    this.applicationDAO = applicationDAO;
    this.applicationComponentLicenseDAO = applicationComponentLicenseDAO;
    this.componentObligationDAO = componentObligationDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.multiLicenseDAO = multiLicenseDAO;
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public List<ApiLicenseLegalApplicationComponentDTO> getLicenseLegalApplicationDashboard(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      LicenseLegalApplicationComponentsFilterDTO filter)
  {
    checkLicense();

    Set<String> stageTypeIds = filter != null ? filter.stageTypeIds : null;
    Set<LicenseObligationReviewStatus> reviewStatuses = filter != null ? filter.reviewStatuses : null;
    Set<String> licenseThreatGroupNames = filter != null ? filter.licenseThreatGroupNames : null;

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    Set<String> stageTypeIdsToCheck =
        isEmpty(stageTypeIds) ? StageTypes.getAll().stream().map(StageType::getId).collect(Collectors.toSet())
            : stageTypeIds;

    List<ApplicationComponentLicensesDTO> applicationComponents = applicationComponentLicenseDAO
        .getApplicationComponentEffectiveLicenses(application.getId(), stageTypeIdsToCheck);
    if (isEmpty(applicationComponents)) {
      return Collections.emptyList();
    }

    Set<String> licenseIdsFound = getLicenseIds(applicationComponents);
    Map<String, Set<String>> obligationNamesByLicenseId = getLicenseObligationsFromHds(licenseIdsFound);
    Map<String, String> licenseNamesByLicenseId = getLicenseNames(licenseIdsFound);
    List<ApiLicenseLegalApplicationComponentDTO> result = new ArrayList<>(applicationComponents.size());

    try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
      for (ApplicationComponentLicensesDTO componentLicensesDTO : applicationComponents) {
        if (componentLicensesDTO.getComponentIdentifier() == null) {
          continue;
        }

        Set<String> licenseIds = componentLicensesDTO.getLicenses();

        Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId = licenseThreatGroupDAO
            .getLicenseIdThreatGroupsByOwnerIdAndLicenseIdsWithHierarchy(tx, application.getId(), licenseIds);

        if (isNotEmpty(licenseThreatGroupNames)) {
          licenseIds = filterLicenseIdsByThreatGroups(licenseIds, licenseThreatGroupNames, threatGroupsByLicenseId);
          if (isEmpty(licenseIds)) {
            continue;
          }
        }

        Set<String> allObligationNames = licenseIds.stream()
            .filter(obligationNamesByLicenseId::containsKey)
            .flatMap(licenseId -> obligationNamesByLicenseId.get(licenseId).stream())
            .collect(Collectors.toSet());

        List<ComponentObligation> obligations =
            componentObligationDAO.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(tx,
                application.getId(), componentLicensesDTO.getComponentIdentifier(), allObligationNames);

        int flaggedCount = 0;
        int openCount = 0;
        int addressedCount = 0;
        for (String obligationName : allObligationNames) {
          ObligationStatus status = obligations.stream()
              .filter(o -> o.getObligationName().equals(obligationName))
              .map(ComponentObligation::getStatus)
              .findFirst()
              .orElse(ObligationStatus.OPEN);
          switch (status) {
            case FLAGGED:
              flaggedCount++;
              break;
            case FULFILLED:
            case IGNORED:
              addressedCount++;
              break;
            default:
              openCount++;
          }
        }

        LicenseObligationReviewStatus reviewStatus = LicenseObligationReviewStatus.IN_PROGRESS;
        if (flaggedCount > 0) {
          reviewStatus = LicenseObligationReviewStatus.FLAGGED;
        }
        else if (isEmpty(allObligationNames)) {
          reviewStatus = isEmptyOrUnspecifiedLicenses(licenseIds) ? LicenseObligationReviewStatus.UNREVIEWED
              : LicenseObligationReviewStatus.COMPLETED;
        }
        else if (openCount == allObligationNames.size()) {
          reviewStatus = LicenseObligationReviewStatus.UNREVIEWED;
        }
        else if (addressedCount >= allObligationNames.size()) {
          reviewStatus = LicenseObligationReviewStatus.COMPLETED;
        }

        if (isEmpty(reviewStatuses) || reviewStatuses.contains(reviewStatus)) {
          ApiLicenseLegalApplicationComponentDTO dto = new ApiLicenseLegalApplicationComponentDTO();
          dto.hash = componentLicensesDTO.getHash();
          dto.displayName =
              ComponentDisplayNameUtil.fromIdentifier(componentLicensesDTO.getComponentIdentifier()).toString();
          dto.licenses =
              newApiLicenses(licenseIds, licenseThreatGroupNames, threatGroupsByLicenseId, licenseNamesByLicenseId);
          dto.reviewCompletedCount = addressedCount;
          dto.reviewTotalCount = allObligationNames.size();
          dto.reviewStatus = reviewStatus;
          result.add(dto);
        }
      }
    }

    return result;
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)) {
      log.debug("License does not support Advanced Legal Pack features");
      throw new InvalidLicenseException();
    }
  }

  private Set<String> getLicenseIds(List<ApplicationComponentLicensesDTO> applicationComponents) {
    return applicationComponents.stream()
        .filter(applicationComponent -> isNotEmpty(applicationComponent.getLicenses()))
        .flatMap(applicationComponent -> applicationComponent.getLicenses().stream())
        .collect(Collectors.toSet());
  }

  private Map<String, Set<String>> getLicenseObligationsFromHds(Set<String> licenseIds) {
    return licenseIds.isEmpty() ? Collections.emptyMap()
        : apiLicenseLegalHdsService.getLicenseMetadata(licenseIds).parallelStream()
        .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, licenseMetadata ->
            licenseMetadata.getLicenseObligations().stream()
                .map(LicenseObligationDTO::getName)
                .collect(Collectors.toSet())));
  }

  private Set<String> filterLicenseIdsByThreatGroups(
      Set<String> licenseIds,
      Set<String> licenseThreatGroupNames,
      Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId)
  {
    return licenseIds.stream()
        .filter(licenseId -> {
          Set<String> groupNamesFound = threatGroupsByLicenseId.getOrDefault(licenseId, Collections.emptyList())
              .stream()
              .map(LicenseThreatGroup::getName)
              .collect(Collectors.toSet());
          return CollectionUtils.containsAny(groupNamesFound, licenseThreatGroupNames);
        })
        .collect(Collectors.toSet());
  }

  private Map<String, String> getLicenseNames(Set<String> licenseIds) {
    try (TransactionContext tx = multiLicenseDAO.createTransactionContext()) {
      return licenseIds.stream().collect(Collectors.toMap(Function.identity(), licenseId -> {
        MultiLicense license = multiLicenseDAO.getById(tx, licenseId);
        return license != null ? license.getShortDisplayName() : licenseId;
      }));
    }
  }

  private boolean isEmptyOrUnspecifiedLicenses(Set<String> licenseIds) {
    return isEmpty(licenseIds) || licenseIds.stream().allMatch(License::isEffectivelyUnspecified);
  }

  private Set<ApiLicenseDTOV2> newApiLicenses(
      Set<String> licenseIds,
      Set<String> licenseThreatGroupNamesToInclude,
      Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId,
      Map<String, String> licenseNamesByLicenseId)
  {
    Set<ApiLicenseDTOV2> licenses = new TreeSet<>(Comparator.comparing(dto -> dto.licenseName));
    ApiLicenseDataAdapter licenseDataAdapter = new ApiLicenseDataAdapter();

    for (String licenseId : licenseIds) {
      List<ApiLicenseThreatDTOV2> licenseThreatGroups =
          threatGroupsByLicenseId.getOrDefault(licenseId, Collections.emptyList()).stream()
              .filter(threatGroup -> isEmpty(licenseThreatGroupNamesToInclude)
                  || licenseThreatGroupNamesToInclude.contains(threatGroup.getName()))
              .map(licenseDataAdapter::convert)
              .collect(Collectors.toList());

      licenses.add(new ApiLicenseDTOV2(licenseId, licenseNamesByLicenseId.get(licenseId), licenseThreatGroups));
    }

    return licenses;
  }
}
