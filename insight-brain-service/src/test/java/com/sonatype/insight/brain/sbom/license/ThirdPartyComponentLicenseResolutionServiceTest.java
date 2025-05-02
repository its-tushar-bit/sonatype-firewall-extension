/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.uuid;
import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyComponentLicenseResolutionServiceTest
    extends AbstractComponentTest
{
  @Inject
  ThirdPartyComponentLicenseResolutionService service;

  private Application app;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent(org);
  }

  @Test
  public void testResolveLicense_NoLicenseOverride() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
            "test-bom.json");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate  =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), "pkg:generic/component@v1");
    ThirdPartyCoordinateLicense thirdPartyCoordinateLicense =
        tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "TEST-LICENSE",
            "TEST-LICENSE", "www.example.com");
    Map<ThirdPartyFileCoordinate, Set<ResolvedLicenseDTO>> result =
        service.resolveLicenses(app, List.of(thirdPartyFileCoordinate));
    assertThat(result).hasSize(1);
    assertThat(result.get(thirdPartyFileCoordinate)).isNotNull();
    assertThat(result.get(thirdPartyFileCoordinate).size()).isEqualTo(1);
    assertThat(result.get(thirdPartyFileCoordinate).contains(new ResolvedLicenseDTO(
        thirdPartyCoordinateLicense.getLicenseId(), thirdPartyCoordinateLicense.getName(),
        thirdPartyCoordinateLicense.getUrl(), null))).isTrue();
  }

  @Test
  public void testResolveLicense_LicenseOverrideNoThirdPartyLicense() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
            "test-bom.json");
    ComponentIdentifier componentIdentifier =
        new PackageUrlIdentifier("pkg:generic/component@v1").toComponentIdentifier();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate  =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), "pkg:generic/component@v1");
    LicenseOverride licenseOverride =
        tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.SELECTED, "MIT");
    Map<ThirdPartyFileCoordinate, Set<ResolvedLicenseDTO>> result =
        service.resolveLicenses(app, List.of(thirdPartyFileCoordinate));
    assertThat(result).hasSize(1);
    assertThat(result.get(thirdPartyFileCoordinate)).isNotNull();
    assertThat(result.get(thirdPartyFileCoordinate).size()).isEqualTo(1);
    assertThat(result.get(thirdPartyFileCoordinate))
        .hasSameElementsAs(licenseOverrideToResolvedLicenseDTO(licenseOverride));
  }

  @Test
  public void testResolveLicense_LicenseOverrideWithThirdPartyLicense() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
            "test-bom.json");
    ComponentIdentifier componentIdentifier =
        new PackageUrlIdentifier("pkg:generic/component@v1").toComponentIdentifier();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate  =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), "pkg:generic/component@v1");
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "TEST-LICENSE",
        "TEST-LICENSE", "www.example.com");
    LicenseOverride licenseOverride =
        tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.SELECTED, "MIT");
    Map<ThirdPartyFileCoordinate, Set<ResolvedLicenseDTO>> result =
        service.resolveLicenses(app, List.of(thirdPartyFileCoordinate));
    assertThat(result).hasSize(1);
    assertThat(result.get(thirdPartyFileCoordinate)).isNotNull();
    assertThat(result.get(thirdPartyFileCoordinate).size()).isEqualTo(1);
    assertThat(result.get(thirdPartyFileCoordinate))
        .hasSameElementsAs(licenseOverrideToResolvedLicenseDTO(licenseOverride));
  }

  private  Set<ResolvedLicenseDTO> licenseOverrideToResolvedLicenseDTO(LicenseOverride licenseOverride) {
    return licenseOverride.getLicenseIds().stream().map(licenseId -> new ResolvedLicenseDTO(licenseId, null, null,
        licenseOverride.getStatus())).collect(Collectors.toSet());
  }
}
