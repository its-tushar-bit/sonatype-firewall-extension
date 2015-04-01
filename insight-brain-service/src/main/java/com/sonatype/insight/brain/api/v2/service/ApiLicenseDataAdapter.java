/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v1.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiLicenseDataDTO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.component.Component;

/**
 * @since 1.13.0
 */
@Named
public class ApiLicenseDataAdapter
{
  private final MultiLicenseDAO multiLicenseDAO;

  @Inject
  public ApiLicenseDataAdapter(final MultiLicenseDAO multiLicenseDAO) {
    this.multiLicenseDAO = multiLicenseDAO;
  }

  public ApiLicenseDataDTO convertToDTO(final Component component) {
    ApiLicenseDataDTO licenseDataDTO = new ApiLicenseDataDTO();
    licenseDataDTO.status = component.getLicenseOverrideStatus().getName();
    convertLicenses(licenseDataDTO.declaredLicenses, component.getDeclaredLicenseIds());
    convertLicenses(licenseDataDTO.observedLicenses, component.getObservedLicenseIds());
    convertLicenses(licenseDataDTO.overriddenLicenses, component.getLicenseOverrideIds());

    return licenseDataDTO;
  }

  private void convertLicenses(final List<ApiLicenseDTO> licenses, final Collection<String> licenseIds) {
    for (String licenseId : licenseIds) {
      ApiLicenseDTO license = new ApiLicenseDTO();
      license.licenseId = licenseId;
      license.licenseName = multiLicenseDAO.getByIdNotNull(licenseId).getShortDisplayName();
      licenses.add(license);
    }
  }
}
