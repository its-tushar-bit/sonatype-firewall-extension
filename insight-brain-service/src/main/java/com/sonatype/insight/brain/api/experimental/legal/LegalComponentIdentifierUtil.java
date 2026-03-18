/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Set;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

public class LegalComponentIdentifierUtil
{
  private LegalComponentIdentifierUtil() {
  }

  public static ComponentIdentifier removeClassifierAndExtension(ComponentIdentifier componentIdentifier) {
    TreeMap<String, String> coordinates = new TreeMap<>(componentIdentifier.getCoordinates());
    coordinates.remove(ComponentIdentifier.MAVEN_CLASSIFIER);
    coordinates.remove(ComponentIdentifier.MAVEN_EXTENSION);
    return new ComponentIdentifier(componentIdentifier.getFormat(), coordinates);
  }

  public static boolean isComponentAKnownInnerSource(
      Set<String> innerSourcePackageUrls,
      ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier != null) {
      PackageUrlIdentifier versionlessPackageUrl = InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier);
      return versionlessPackageUrl != null && innerSourcePackageUrls.contains(versionlessPackageUrl.getPackageUrl());
    }
    return false;
  }

  public static ApiReportComponentDTOV2 toApiReportComponentDTOV2(
      Component component,
      ApiLicenseDataDTOV2 licenseDataDTOV2)
  {
    ApiReportComponentDTOV2 componentDTO = new ApiReportComponentDTOV2();
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    componentDTO.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    componentDTO.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    componentDTO.hash = component.getHash();
    componentDTO.displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
    componentDTO.proprietary = component.isProprietary();
    componentDTO.thirdParty =
        IdentificationSource.isThirdPartyIdentificationSource(component.getIdentificationSource().getId());
    componentDTO.licenseData = licenseDataDTOV2;
    return componentDTO;
  }
}
