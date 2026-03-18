/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.license;

import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ThirdPartyComponentLicenseResolutionServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ThirdPartyComponentLicenseResolutionService service;

  @Inject
  private TestProductLicense productLicense;

  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testResolveLicenseOverridesOrThirdPartyLicenses_NoLicenseOverride() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
        "test-bom.json");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), "pkg:generic/component@v1");
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "TEST-LICENSE", "TEST-LICENSE",
        "www.example.com");

    Set<ResolvedLicenseDTO> result =
        service.resolveLicenseOverridesOrThirdPartyLicenses(app.getId(), thirdPartyFileCoordinate);

    assertThat(result).hasSize(1);
    assertThat(result).extracting(ResolvedLicenseDTO::licenseId)
        .containsExactly("TEST-LICENSE");
  }

  @Test
  public void testResolveLicenseOverridesOrThirdPartyLicenses_LicenseOverrideNoThirdPartyLicense() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
        "test-bom.json");
    ComponentIdentifier componentIdentifier =
        new PackageUrlIdentifier("pkg:generic/component@v1").toComponentIdentifier();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), "pkg:generic/component@v1");
    tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.SELECTED, "MIT");

    Set<ResolvedLicenseDTO> result =
        service.resolveLicenseOverridesOrThirdPartyLicenses(app.getId(), thirdPartyFileCoordinate);

    assertThat(result).hasSize(1)
        .extracting(ResolvedLicenseDTO::licenseId)
        .containsExactly("MIT");
  }

  @Test
  public void testGetLicenseOverrides_whenExist() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
        "test-bom.json");
    String purl = "pkg:generic/component@v1";
    ComponentIdentifier cid =
        new PackageUrlIdentifier(purl).toComponentIdentifier();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), purl);
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "Apache-2.0", "Apache-2.0", "www.example.com");
    tempEntity.newLicenseOverride(app.getId(), cid, LicenseOverrideStatus.SELECTED, Set.of("MIT", "Aladdin"));

    Set<ResolvedLicenseDTO> result = service.getLicenseOverrides(app.getId(), purl);

    assertThat(result).hasSize(2)
        .extracting(ResolvedLicenseDTO::licenseId)
        .containsExactlyInAnyOrder("MIT", "Aladdin");
  }

  @Test
  public void testGetLicenseOverrides_whenNotExist() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
        "test-bom.json");
    String purl = "pkg:generic/component@v1";
    ThirdPartyFileCoordinate tpfc =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component", "v1",
            uuid().substring(0, 20), purl);
    tempEntity.newThirdPartyCoordinateLicense(tpfc, "Apache-2.0", "Apache-2.0", "www.example.com");

    assertThat(service.getLicenseOverrides(app.getId(), purl)).isEmpty();
  }

  @Test
  public void testGetLicenseOverrides_invalidPurl() {
    String purl = "pkg:scrambled";
    assertThat(service.getLicenseOverrides(app.getId(), purl)).isEmpty();
  }

  @Test
  public void testGetLicenseOverrides_nullPurl() {
    assertThat(service.getLicenseOverrides(app.getId(), null)).isEmpty();
  }

  @Test
  public void testResolveLicense_LicenseOverrideWithThirdPartyLicense_ForSbomProduct() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    testResolveLicenseOverridesOrThirdPartyLicenses_LicenseOverrideWithProduct("TEST-LICENSE");
  }

  @Test
  public void testResolveLicense_LicenseOverrideWithThirdPartyLicense_ForLifecycleProduct() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    testResolveLicenseOverridesOrThirdPartyLicenses_LicenseOverrideWithProduct("Aladdin", "MIT");
  }

  @Test
  public void testResolveLicenseOverridesOrThirdPartyLicenses_ForSbomAndALPProduct() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);
    testResolveLicenseOverridesOrThirdPartyLicenses_LicenseOverrideWithProduct("Aladdin", "MIT");
  }

  private void testResolveLicenseOverridesOrThirdPartyLicenses_LicenseOverrideWithProduct(String... expected) {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
        "test-bom.json");
    ComponentIdentifier componentIdentifier =
        new PackageUrlIdentifier("pkg:generic/component@v1").toComponentIdentifier();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), "pkg:generic/component@v1");
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "TEST-LICENSE",
        "TEST-LICENSE", "www.example.com");
    tempEntity.newLicenseOverride(app.getId(),
        componentIdentifier, LicenseOverrideStatus.SELECTED, Set.of("MIT", "Aladdin"));
    Set<ResolvedLicenseDTO> result =
        service.resolveLicenseOverridesOrThirdPartyLicenses(app.getId(), thirdPartyFileCoordinate);
    assertThat(result).hasSize(expected.length).extracting("licenseId").containsExactlyInAnyOrder(expected);
  }

  @Test
  public void testResolveLicenseOverridesOrThirdPartyLicenses_InvalidAppId() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "SBOM", "generic", "component",
            "v1", uuid().substring(0, 20), "pkg:generic/component@v1");
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.resolveLicenseOverridesOrThirdPartyLicenses("unknownAppId", thirdPartyFileCoordinate);
    }).withMessageContaining("Application with ID unknownAppId does not exist.");
  }
}
