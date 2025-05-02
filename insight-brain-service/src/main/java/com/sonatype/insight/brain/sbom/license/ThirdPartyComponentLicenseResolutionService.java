/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Named
public class ThirdPartyComponentLicenseResolutionService
{
  private final LicenseOverrideDAO licenseOverrideDAO;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private static final Set<LicenseOverrideStatus> LICENSE_OVERRIDE_STATUSES =
      Set.of(LicenseOverrideStatus.SELECTED, LicenseOverrideStatus.OVERRIDDEN) ;

  @Inject
  public ThirdPartyComponentLicenseResolutionService(LicenseOverrideDAO licenseOverrideDAO,
                                                     ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO)
  {
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
  }

  public Map<ThirdPartyFileCoordinate, Set<ResolvedLicenseDTO>> resolveLicenses(
      Application application,
      List<ThirdPartyFileCoordinate> thirdPartyFileCoordinates)
  {
    List<Pair<ThirdPartyFileCoordinate, ComponentIdentifier>> componentIdentifiers = new ArrayList<>();
    Map<String, ThirdPartyFileCoordinate> thirdPartyFileCoordinateById = new HashMap<>();
    for (ThirdPartyFileCoordinate thirdPartyFileCoordinate : thirdPartyFileCoordinates) {
      thirdPartyFileCoordinateById.put(thirdPartyFileCoordinate.getId(), thirdPartyFileCoordinate);
      if (StringUtils.isNotEmpty(thirdPartyFileCoordinate.getPackageUrl())) {
        componentIdentifiers.add(Pair.of(thirdPartyFileCoordinate,
            new PackageUrlIdentifier(thirdPartyFileCoordinate.getPackageUrl()).toComponentIdentifier()));
      }
    }
    Set<String> componentIdsWithoutLicenseOverrides = new HashSet<>();
    Map<ThirdPartyFileCoordinate, LicenseOverride> licenseOverrides = findThirdPartyFileCoordinatesWithOverrides(
        application, componentIdentifiers, componentIdsWithoutLicenseOverrides);
    Map<ThirdPartyFileCoordinate, Set<ThirdPartyCoordinateLicense>> componentsToLicenses =
        findThirdPartyFileCoordinatesWithoutOverrides(thirdPartyFileCoordinateById,
            componentIdsWithoutLicenseOverrides);
    return mapThirdPartyFileCoordinateToResolvedLicenseDTO(thirdPartyFileCoordinates, licenseOverrides,
        componentsToLicenses);
  }

  private Map<ThirdPartyFileCoordinate, Set<ResolvedLicenseDTO>> mapThirdPartyFileCoordinateToResolvedLicenseDTO(
      List<ThirdPartyFileCoordinate> thirdPartyFileCoordinates,
      Map<ThirdPartyFileCoordinate, LicenseOverride> componentsToLicenseOverrides,
      Map<ThirdPartyFileCoordinate, Set<ThirdPartyCoordinateLicense>> componentsToCoordinateLicenses)
  {
    Map<ThirdPartyFileCoordinate, Set<ResolvedLicenseDTO>> licenseDTOs = new HashMap<>();
    for (ThirdPartyFileCoordinate thirdPartyFileCoordinate : thirdPartyFileCoordinates) {
      licenseDTOs.put(thirdPartyFileCoordinate, new HashSet<>());
      if (componentsToLicenseOverrides.containsKey(thirdPartyFileCoordinate)) {
        LicenseOverride licenseOverride = componentsToLicenseOverrides.get(thirdPartyFileCoordinate);
        licenseDTOs.get(thirdPartyFileCoordinate).addAll(licenseOverride.getLicenseIds().stream()
            .map(licenseId -> new ResolvedLicenseDTO(licenseId, null, null, licenseOverride.getStatus()))
            .collect(Collectors.toSet()));
      }
      else if (componentsToCoordinateLicenses.containsKey(thirdPartyFileCoordinate)) {
        licenseDTOs.get(thirdPartyFileCoordinate).addAll(componentsToCoordinateLicenses.get(thirdPartyFileCoordinate)
            .stream().map(thirdPartyCoordinateLicense -> new
                ResolvedLicenseDTO(thirdPartyCoordinateLicense.getLicenseId(), thirdPartyCoordinateLicense.getName(),
                thirdPartyCoordinateLicense.getUrl(), null)).collect(Collectors.toSet()));
      }
    }
    return licenseDTOs;
  }

  private Map<ThirdPartyFileCoordinate, Set<ThirdPartyCoordinateLicense>> findThirdPartyFileCoordinatesWithoutOverrides(
      Map<String, ThirdPartyFileCoordinate> thirdPartyFileCoordinateById,
      Set<String> componentIdsWithoutLicenseOverrides)
  {
    if (CollectionUtils.isEmpty(componentIdsWithoutLicenseOverrides)) {
      return Collections.emptyMap();
    }
    Map<ThirdPartyFileCoordinate, Set<ThirdPartyCoordinateLicense>> thirdPartyFileCoordinatesToLicenses
        = new HashMap<>();
    List<ThirdPartyCoordinateLicense> thirdPartyCoordinateLicenses =
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateIds(componentIdsWithoutLicenseOverrides);
    for (ThirdPartyCoordinateLicense thirdPartyCoordinateLicense : thirdPartyCoordinateLicenses) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate =
          thirdPartyFileCoordinateById.get(thirdPartyCoordinateLicense.getFileCoordinateId());
      if (thirdPartyFileCoordinatesToLicenses.containsKey(thirdPartyFileCoordinate)) {
        thirdPartyFileCoordinatesToLicenses.get(thirdPartyFileCoordinate).add(thirdPartyCoordinateLicense);
      }
      else {
        Set<ThirdPartyCoordinateLicense> licenses = new HashSet<>();
        licenses.add(thirdPartyCoordinateLicense);
        thirdPartyFileCoordinatesToLicenses.put(thirdPartyFileCoordinate, licenses);
      }
    }
    return thirdPartyFileCoordinatesToLicenses;
  }

  private Map<ThirdPartyFileCoordinate, LicenseOverride> findThirdPartyFileCoordinatesWithOverrides(
      Application sbomApplication,
      List<Pair<ThirdPartyFileCoordinate, ComponentIdentifier>> componentIdentifiers,
      Set<String> componentIdsWithoutLicenseOverrides)
  {
    Map<ThirdPartyFileCoordinate, LicenseOverride> thirdPartyFileCoordinateToLicenseOverrides = new HashMap<>();
    for (Pair<ThirdPartyFileCoordinate, ComponentIdentifier> pair : componentIdentifiers) {
      LicenseOverride licenseOverride =
          licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(sbomApplication, pair.getRight());
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = pair.getLeft();
      if (licenseOverride != null && LICENSE_OVERRIDE_STATUSES.contains(licenseOverride.getStatus())) {
        thirdPartyFileCoordinateToLicenseOverrides.put(thirdPartyFileCoordinate, licenseOverride);
      }
      else {
        componentIdsWithoutLicenseOverrides.add(thirdPartyFileCoordinate.getId());
      }
    }
    return thirdPartyFileCoordinateToLicenseOverrides;
  }
}
