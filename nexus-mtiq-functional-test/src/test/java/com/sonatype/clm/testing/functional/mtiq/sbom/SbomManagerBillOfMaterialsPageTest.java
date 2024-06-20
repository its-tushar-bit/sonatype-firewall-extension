/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SbomManagerBillOfMaterialsPageTest
    extends AbstractMtiqFunctionalTest
{
  private final SbomManagerBillOfMaterialsPage sbomManagerBillOfMaterialsPage = new SbomManagerBillOfMaterialsPage();

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Organization organization;

  private Application application;

  private ThirdPartySbomMetadata sbomMetadata;

  @Before
  public void init() {
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());

    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
      scannedFile.getId(),
      application.getId(),
      "test-version",
      "ACTIVE",
      scannedFile.getFilename(),
      SbomSpecification.CYCLONEDX.name(),
      SbomFormat.XML.name(),
      "0.0"
    );
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testBillOfMaterial_PageHeader() {
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    sbomManagerBillOfMaterialsPage.title().shouldHave(text("Test Application")).shouldBe(visible);
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'XXX");
    sbomManagerBillOfMaterialsPage.importedDate()
        .shouldBe(visible)
        .shouldHave(text(dateFormat.format(sbomMetadata.getCreatedAt()).replace("Z", "+00:00")));
  }

  @Test
  public void testBillOfMaterial_SbomManagerDisabled() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    sbomManagerBillOfMaterialsPage.title().shouldNotBe(visible);
    sbomManagerBillOfMaterialsPage.errorAlert().shouldBe(visible);
  }
}
