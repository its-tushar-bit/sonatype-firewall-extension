/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalApplicationComponentsFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.legal.LegalDashboardsService;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

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

  private final InnerSourceApplicationDAO innerSourceApplicationDAO;

  private final LegalDashboardsService legalDashboardService;

  private final OwnerDAO ownerDAO;

  @Inject
  public LegalApplicationDashboardService(
      ProductLicense productLicense,
      ApiLicenseLegalHdsService apiLicenseLegalHdsService,
      ApplicationDAO applicationDAO,
      ApplicationComponentLicenseDAO applicationComponentLicenseDAO,
      ComponentObligationDAO componentObligationDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      MultiLicenseDAO multiLicenseDAO,
      InnerSourceApplicationDAO innerSourceApplicationDAO,
      LegalDashboardsService legalDashboardService,
      OwnerDAO ownerDAO)
  {
    this.productLicense = productLicense;
    this.apiLicenseLegalHdsService = apiLicenseLegalHdsService;
    this.applicationDAO = applicationDAO;
    this.applicationComponentLicenseDAO = applicationComponentLicenseDAO;
    this.componentObligationDAO = componentObligationDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.innerSourceApplicationDAO = innerSourceApplicationDAO;
    this.legalDashboardService = legalDashboardService;
    this.ownerDAO = ownerDAO;
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public List<ApiLicenseLegalApplicationComponentDTO> getLicenseLegalApplicationDashboard(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      LicenseLegalApplicationComponentsFilterDTO filter)
  {
    checkLicense();

    Set<String> stageTypeIds = filter != null ? filter.stageTypeIds : null;
    Set<LicenseObligationReviewStatus> reviewStatuses = checkReviewStatus(filter);
    Set<String> licenseThreatGroupNames = checkLicenseThreat(filter);

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    Set<String> stageTypeIdsToCheck =
        isEmpty(stageTypeIds)
            ? StageTypes.getAll().stream().map(StageType::getId).collect(Collectors.toSet())
            : stageTypeIds;

    List<ApplicationComponentLicensesDTO> applicationComponentLicensesDTOS = applicationComponentLicenseDAO
        .getApplicationComponentEffectiveLicenses(application.getId(), stageTypeIdsToCheck);
    if (isEmpty(applicationComponentLicensesDTOS)) {
      return Collections.emptyList();
    }

    Set<PackageUrlIdentifier> componentPurls = applicationComponentLicensesDTOS.stream()
        .map(ac -> InnerSourceUtils.getVersionlessPackageUrl(ac.getComponentIdentifier()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<String> innerSourcePackageUrls = innerSourceApplicationDAO.getByPackageUrls(componentPurls)
        .stream()
        .map(InnerSourceApplication::getPackageUrl)
        .collect(Collectors.toSet());

    applicationComponentLicensesDTOS.removeIf(c -> LegalComponentIdentifierUtil
        .isComponentAKnownInnerSource(innerSourcePackageUrls, c.getComponentIdentifier()));

    Set<String> licenseIdsFound = legalDashboardService.getLicenseIds(applicationComponentLicensesDTOS);
    Map<String, Set<String>> obligationNamesByLicenseId = getLicenseObligationsFromHds(licenseIdsFound);
    Map<String, String> licenseNamesByLicenseId = getLicenseNames(licenseIdsFound);
    List<ApiLicenseLegalApplicationComponentDTO> result = new ArrayList<>(applicationComponentLicensesDTOS.size());

    List<String> ownerIds = ownerDAO.getOwnerIds(application.getId());

    try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
      for (ApplicationComponentLicensesDTO componentLicensesDTO : applicationComponentLicensesDTOS) {
        if (componentLicensesDTO.getComponentIdentifier() == null) {
          continue;
        }

        Set<String> multiLicenseIds = componentLicensesDTO.getLicenses();

        Map<String, Set<String>> multiLicenseIdToSingleLicenseId =
            buildMultiLicenseIdToSingleLicenseIdsMap(multiLicenseIds);

        Set<String> singleLicenseIds = multiLicenseIdToSingleLicenseId.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId = licenseThreatGroupDAO
            .getLicenseIdThreatGroupsByOwnerIdsAndLicenseIds(tx, ownerIds, singleLicenseIds);

        if (isNotEmpty(licenseThreatGroupNames)) {
          singleLicenseIds =
              filterLicenseIdsByThreatGroups(singleLicenseIds, licenseThreatGroupNames, threatGroupsByLicenseId);
          if (isEmpty(singleLicenseIds)) {
            continue;
          }
        }

        Set<String> allObligationNames = singleLicenseIds.stream()
            .filter(obligationNamesByLicenseId::containsKey)
            .flatMap(licenseId -> obligationNamesByLicenseId.get(licenseId).stream())
            .collect(Collectors.toSet());

        List<ComponentObligation> obligations = componentObligationDAO
            .getByOwnerIdsAndComponentIdentifierAndObligationNames(tx, ownerIds,
                componentLicensesDTO.getComponentIdentifier(), allObligationNames);

        Map<String, Integer> countMap = legalDashboardService.countObligations(obligations, allObligationNames);

        int flaggedCount = countMap.get(LegalDashboardsService.FLAGGEDCOUNT);
        int openCount = countMap.get(LegalDashboardsService.OPENCOUNT);
        int addressedCount = countMap.get(LegalDashboardsService.ADDRESSEDCOUNT);

        LicenseObligationReviewStatus reviewStatus = legalDashboardService.getReviewStatus(flaggedCount, openCount,
            addressedCount, allObligationNames, multiLicenseIds);

        if (isEmpty(reviewStatuses) || reviewStatuses.contains(reviewStatus)) {
          ApiLicenseLegalApplicationComponentDTO dto = new ApiLicenseLegalApplicationComponentDTO();
          dto.hash = componentLicensesDTO.getHash();
          dto.displayName =
              ComponentDisplayNameUtil.fromIdentifier(componentLicensesDTO.getComponentIdentifier()).toString();
          dto.licenses =
              newApiLicenses(multiLicenseIdToSingleLicenseId, licenseThreatGroupNames, threatGroupsByLicenseId,
                  licenseNamesByLicenseId);
          dto.reviewCompletedCount = addressedCount;
          dto.reviewTotalCount = allObligationNames.size();
          dto.reviewStatus = reviewStatus;
          result.add(dto);
        }
      }
    }

    Collections.sort(result, Comparator.comparing(dto -> dto.displayName, String.CASE_INSENSITIVE_ORDER));

    return result;
  }

  /**
   * Given a Collection of multiLicenseIds, build a map where the key is the multiLicenseId and the value is a set of
   * singleLicenseIds for that multiLicense.
   *
   * @param multiLicenseIds the multiLicenses which will be the keys
   * @return a map of multiLicense to singleLicenses
   */
  private HashMap<String, Set<String>> buildMultiLicenseIdToSingleLicenseIdsMap(
      final Collection<String> multiLicenseIds)
  {
    return multiLicenseIds.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            multiLicenseId -> multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicenseId)
                .stream()
                .map(License::getId)
                .collect(Collectors.toSet()),
            (prev, next) -> next,
            HashMap::new));
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)) {
      log.debug("License does not support Advanced Legal Pack features");
      throw new InvalidLicenseException();
    }
  }

  private Map<String, Set<String>> getLicenseObligationsFromHds(Set<String> licenseIds) {
    return licenseIds.isEmpty()
        ? Collections.emptyMap()
        : apiLicenseLegalHdsService.getLicenseMetadata(licenseIds)
            .parallelStream()
            .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId,
                licenseMetadata -> licenseMetadata.getLicenseObligations()
                    .stream()
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

  private Set<LicenseObligationReviewStatus> checkReviewStatus(LicenseLegalApplicationComponentsFilterDTO filter) {
    return filter != null ? filter.reviewStatuses : Collections.emptySet();
  }

  private Set<String> checkLicenseThreat(LicenseLegalApplicationComponentsFilterDTO filter) {
    return filter != null ? filter.licenseThreatGroupNames : Collections.emptySet();
  }

  private Set<ApiLicenseDTOV2> newApiLicenses(
      Map<String, Set<String>> multiLicenseIdToSingleLicenseIds,
      Set<String> licenseThreatGroupNamesToInclude,
      Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId,
      Map<String, String> licenseNamesByLicenseId)
  {
    Set<ApiLicenseDTOV2> licenses = new TreeSet<>(Comparator.comparing(dto -> dto.licenseName));
    ApiLicenseDataAdapter licenseDataAdapter = new ApiLicenseDataAdapter(multiLicenseDAO);

    for (Entry<String, Set<String>> e : multiLicenseIdToSingleLicenseIds.entrySet()) {

      List<ApiLicenseThreatDTOV2> licenseThreatGroups = new ArrayList<>();

      for (String singleLicense : e.getValue()) {
        // Fetch all license threat groups associated with this license, filter based on user selection
        final List<LicenseThreatGroup> singleLicenseThreatGroups =
            threatGroupsByLicenseId.getOrDefault(singleLicense, Collections.emptyList());

        licenseThreatGroups.addAll(
            singleLicenseThreatGroups.stream()
                .filter(threatGroup -> isEmpty(licenseThreatGroupNamesToInclude)
                    || licenseThreatGroupNamesToInclude.contains(threatGroup.getName()))
                .map(licenseDataAdapter::convert)
                .collect(Collectors.toList()));
      }

      licenses.add(new ApiLicenseDTOV2(e.getKey(), licenseNamesByLicenseId.get(e.getKey()), licenseThreatGroups));
    }

    return licenses;
  }
}
