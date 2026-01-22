/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

@Named
public class ApiLicenseDataAdapter
{
  private final MultiLicenseDAO multiLicenseDAO;

  @Inject
  public ApiLicenseDataAdapter(MultiLicenseDAO multiLicenseDAO) {
    this.multiLicenseDAO = multiLicenseDAO;
  }

  public ApiLicenseDataDTO convertToDTO(final Component component) {
    ApiLicenseDataDTO licenseDataDTO = new ApiLicenseDataDTO();
    convert(component, licenseDataDTO);

    return licenseDataDTO;
  }

  /**
   * @since 1.16.0
   */
  public ApiLicenseDataDTO convertToDTO(ComponentEvaluationDataList.ComponentEvaluationData componentDetailsFromHds) {
    ApiLicenseDataDTO licenseDataDTO = new ApiLicenseDataDTO();
    convert(componentDetailsFromHds, licenseDataDTO);

    return licenseDataDTO;
  }

  public ApiLicenseDataDTOV2 convertToDTOV2(final Component component) {
    ApiLicenseDataDTOV2 licenseDataDTO = new ApiLicenseDataDTOV2();
    convert(component, licenseDataDTO);

    for (LicenseThreatGroup ltg : component.getLicenseThreatGroups()) {
      licenseDataDTO.effectiveLicenseThreats.add(convert(ltg));
    }

    return licenseDataDTO;
  }

  public ApiLicenseThreatDTOV2 convert(LicenseThreatGroup ltg) {
    ApiLicenseThreatDTOV2 threat = new ApiLicenseThreatDTOV2();
    threat.licenseThreatGroupLevel = ltg.getThreatLevel();
    threat.licenseThreatGroupName = ltg.getName();

    if (ltg.getThreatLevel() > 7) {
      threat.licenseThreatGroupCategory = "critical";
    }
    else if (ltg.getThreatLevel() > 3) {
      threat.licenseThreatGroupCategory = "severe";
    }
    else if (ltg.getThreatLevel() > 0) {
      threat.licenseThreatGroupCategory = "moderate";
    }
    else {
      threat.licenseThreatGroupCategory = "no-threat";
    }
    return threat;
  }

  private String getLicenseNameById(final String licenseId) {
    return multiLicenseDAO.getByIdNotNull(licenseId).getShortDisplayName();
  }

  private void convert(final Component component, final ApiLicenseDataDTO licenseDataDTO) {
    // For display purposes, multi-license names should always be used as they make the distinction
    // between AND and OR clear. Using only the individual license names loses that distinction and customers
    // tend to assume AND is meant even though for multi-licenses it's generally (always?) OR.
    // CLM-16638
    Set<String> declaredLicenses = component.getDeclaredMultiLicenseIds();
    Set<String> observedLicenses = component.getObservedMultiLicenseIds();

    licenseDataDTO.status = component.getLicenseOverrideStatus().getName();

    convertLicenses(licenseDataDTO.declaredLicenses, declaredLicenses);
    convertLicenses(licenseDataDTO.observedLicenses, observedLicenses);
    convertLicenses(licenseDataDTO.overriddenLicenses, component.getLicenseOverrideIds());
    convertLicenses(licenseDataDTO.effectiveLicenses, ComponentDetailsLoader.calculateEffectiveLicenses(
        declaredLicenses, observedLicenses, component.getLicenseOverrideIds()));
  }

  private void convert(ComponentEvaluationDataList.ComponentEvaluationData componentDetailsFromHds,
                       ApiLicenseDataDTO licenseDataDTO)
  {
    licenseDataDTO.status = LicenseOverrideStatus.OPEN.getName();
    convertLicenses(licenseDataDTO.declaredLicenses, componentDetailsFromHds.declaredLicenses);
    convertLicenses(licenseDataDTO.observedLicenses, componentDetailsFromHds.observedLicenses);
    convertLicenses(licenseDataDTO.effectiveLicenses, ComponentDetailsLoader.calculateEffectiveLicenses(
        getLicenseIds(componentDetailsFromHds.declaredLicenses),
        getLicenseIds(componentDetailsFromHds.observedLicenses)));
  }

  private void convertLicenses(final List<ApiLicenseDTO> licenses, final Collection<String> licenseIds) {
    for (String licenseId : licenseIds) {
      ApiLicenseDTO license = new ApiLicenseDTO();
      license.licenseId = licenseId;
      license.licenseName = getLicenseNameById(licenseId);
      licenses.add(license);
    }
  }

  private void convertLicenses(List<ApiLicenseDTO> licenseDTOs, Set<License> licenses) {
    for (License license : licenses) {
      ApiLicenseDTO licenseDTO = new ApiLicenseDTO();
      licenseDTO.licenseId = license.getLicenseId();
      licenseDTO.licenseName = getLicenseNameById(licenseDTO.licenseId);
      licenseDTOs.add(licenseDTO);
    }
  }

  private static Set<String> getLicenseIds(Set<License> licenses) {
    return licenses.stream().map(License::getLicenseId).collect(Collectors.toSet());
  }
}
