/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;

public class ComponentInfoResourceTestUtils
{
  public static ComponentDetails newComponentDetails(
      ComponentIdentifier componentIdentifier,
      MultiLicenseDAO multiLicenseDAO)
  {
    ComponentDetails componentDetails = new ComponentDetails(componentIdentifier);
    componentDetails.setHash("somehash");
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setDeclaredLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO
        .getByIdNotNull("Apache-2.0"))));
    componentDetails
        .setObservedLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("EPL-1.0"))));
    componentDetails
        .setOverriddenLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
    componentDetails
        .setEffectiveLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
    componentDetails.setEffectiveLicenseStatus(LicenseStatus.Overridden);
    SecurityVulnerability sv = new SecurityVulnerability("refid", "source", 1F);
    sv.setStatus(SecurityVulnerabilityOverrideStatus.OPEN.getName());
    componentDetails.setSecurityVulnerabilities(Collections.singletonList(sv));
    componentDetails.setCatalogDate(new Date().getTime());
    componentDetails.setWebsite("http://www.example.com");
    componentDetails.setLicenseThreatLevel(2);
    componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    componentDetails.setIdentificationSourceComment("No comments");
    return componentDetails;
  }

  public static String convertToHdsUrl(String brainUrl) {
    return brainUrl.replaceFirst("(.*/)(rest/[^/]+)/componentDetails(/[^/]+/[^/]+)(.*)", "$2/componentDetails$4")
        .replace("allVersions", "list");
  }

  public static License toLicenseDTO(MultiLicense multiLicense) {
    return new License(multiLicense.getId(), multiLicense.getShortDisplayName());
  }
}
