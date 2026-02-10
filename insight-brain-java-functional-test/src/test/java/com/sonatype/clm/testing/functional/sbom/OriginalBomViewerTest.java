/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.FileUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;

/**
 * Functional tests for the Original BOM Viewer preview functionality.
 * Tests verify that preview text displays correctly for collapsed nodes in the tree view
 * and that it disappears when nodes are expanded.
 */
public class OriginalBomViewerTest
    extends AbstractFunctionalTest
{
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Organization organization;

  private Application application;

  private ThirdPartySbomMetadata sbomMetadata;

  private final InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws Exception {
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());
  }

  @Before
  public void setLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
  }

  @Test
  public void testOriginalBomViewer_PreviewDisplaysForCollapsedJsonNodes() throws Exception {
    // Setup: Create SBOM with JSON format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to bill of materials page
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    // Click on "Original BOM" tab
    $$(".nx-tab").findBy(text("Original BOM")).shouldBe(visible).click();

    // Wait for tree to load
    SelenideElement tree = $(".iq-original-bom-viewer__tree");
    tree.shouldBe(visible);

    // Verify that collapsed nodes show preview
    SelenideElement metadataNode = $$("span.iq-original-bom-viewer__key").findBy(text("metadata")).parent();
    SelenideElement preview = metadataNode.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible);
    preview.shouldHave(Condition.not(Condition.empty));
  }

  @Test
  public void testOriginalBomViewer_PreviewHidesWhenNodeExpanded() throws Exception {
    // Setup: Create SBOM with JSON format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to bill of materials page
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    // Click on "Original BOM" tab
    $$(".nx-tab").findBy(text("Original BOM")).shouldBe(visible).click();

    // Wait for tree to load
    $(".iq-original-bom-viewer__tree").shouldBe(visible);

    // Find a collapsed node with preview (components is collapsed in test data)
    SelenideElement componentsLabel = $$("span.iq-original-bom-viewer__key").findBy(text("components")).parent();

    // Verify preview is present when collapsed (search only in label, not nested children)
    SelenideElement preview = componentsLabel.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible);

    // Expand the node by clicking its collapse icon
    SelenideElement componentsItem = $$("span.iq-original-bom-viewer__key").findBy(text("components"))
        .closest(".nx-tree__item");
    SelenideElement collapseIcon = componentsItem.$(".nx-tree__collapse-click");
    collapseIcon.click();

    // Verify preview no longer exists after expansion
    preview.should(Condition.not(Condition.exist));
  }

  @Test
  public void testOriginalBomViewer_PreviewDisplaysForXmlFormat() throws Exception {
    // Setup: Create SBOM with XML format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.xml", SbomFormat.XML, SbomSpecification.CYCLONEDX);

    // Navigate to bill of materials page
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    // Click on "Original BOM" tab
    $$(".nx-tab").findBy(text("Original BOM")).shouldBe(visible).click();

    // Wait for tree to load
    $(".iq-original-bom-viewer__tree").shouldBe(visible);

    // Verify that collapsed XML nodes show preview with attributes
    // Look for any collapsed node with a preview (XML nodes with attributes/children show previews)
    SelenideElement xmlPreview = $$(".iq-original-bom-viewer__preview").first();
    xmlPreview.shouldBe(visible);
    xmlPreview.shouldHave(Condition.not(Condition.empty));
  }

  @Test
  public void testOriginalBomViewer_PreviewShowsEllipsisForLargeObjects() throws Exception {
    // Setup: Create SBOM with JSON format that has objects with many properties
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to bill of materials page
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    // Click on "Original BOM" tab
    $$(".nx-tab").findBy(text("Original BOM")).shouldBe(visible).click();

    // Wait for tree to load
    $(".iq-original-bom-viewer__tree").shouldBe(visible);

    // Find node with preview that contains ellipsis
    // The preview MUST show "…" when there are more than 3 properties
    // Test will fail if ellipsis feature is broken
    SelenideElement previewWithEllipsis = $$(".iq-original-bom-viewer__preview").findBy(text("…"));
    previewWithEllipsis.shouldBe(visible);
    previewWithEllipsis.shouldHave(text("…"));
  }

  @Test
  public void testOriginalBomViewer_PreviewDisplaysForSpdxFormat() throws Exception {
    // Setup: Create SBOM with SPDX JSON format
    setupSbomWithFormat("sboms/valid-spdx-bom.json", SbomFormat.JSON, SbomSpecification.SPDX);

    // Navigate to bill of materials page
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    // Click on "Original BOM" tab
    $$(".nx-tab").findBy(text("Original BOM")).shouldBe(visible).click();

    // Wait for tree to load
    $(".iq-original-bom-viewer__tree").shouldBe(visible);

    // Verify that SPDX format nodes also show previews
    // SPDX fields like "creationInfo" or "packages" MUST have previews
    // Test will fail if SPDX preview feature is broken
    SelenideElement spdxNode = $$("span.iq-original-bom-viewer__key")
        .findBy(text("creationInfo").or(text("packages"))).parent();
    SelenideElement preview = spdxNode.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible);
    preview.shouldHave(Condition.not(Condition.empty));
  }

  @Test
  public void testOriginalBomViewer_PreviewCssTruncation() throws Exception {
    // Setup: Create SBOM with JSON format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to bill of materials page
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    // Click on "Original BOM" tab
    $$(".nx-tab").findBy(text("Original BOM")).shouldBe(visible).click();

    // Wait for tree to load
    $(".iq-original-bom-viewer__tree").shouldBe(visible);

    // Find a preview element
    SelenideElement preview = $$(".iq-original-bom-viewer__preview").first();
    preview.shouldBe(visible);

    // Verify CSS properties for truncation are explicitly applied
    // These values MUST be set for proper text truncation with ellipsis
    preview.shouldHave(Condition.cssValue("overflow", "hidden"));
    preview.shouldHave(Condition.cssValue("text-overflow", "ellipsis"));
    preview.shouldHave(Condition.cssValue("white-space", "nowrap"));
  }

  /**
   * Helper method to setup SBOM metadata with specified format and specification
   */
  private void setupSbomWithFormat(String bomFileName, SbomFormat format, SbomSpecification specification)
      throws Exception
  {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(scannedFile);

    // Create report file for the scan
    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/OriginalBomViewerTest", tempDir), reportFile);

    // Create policy evaluation
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    Path zippedBom = mockOriginalSbom(
        OriginalBomViewerTest.class,
        bomFileName,
        insightWork.getSbomDir(application.getId()).toPath()
    );

    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        scan.getThirdPartyFileId(),
        application.getId(),
        "test-version",
        ACTIVE,
        zippedBom.getFileName().toString(),
        specification.toString(),
        format.name(),
        specification == SbomSpecification.CYCLONEDX ? "1.5" : "2.3"
    );

    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
  }
}
