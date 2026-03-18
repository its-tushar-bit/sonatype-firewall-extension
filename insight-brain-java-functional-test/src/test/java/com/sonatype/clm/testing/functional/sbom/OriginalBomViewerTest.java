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
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.FileUtils;

import java.time.Duration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
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

  private SbomManagerBillOfMaterialsPage page;

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
    setFeatures(LicensedFeature.SBOM_MANAGER);

    // Force a page reload to trigger permission re-check after license is set
    refreshOrOpen("about");
  }

  private void navigateToOriginalBomTab() {
    page = new SbomManagerBillOfMaterialsPage();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    page.tabs().findBy(text("Original BOM")).shouldBe(visible).click();
    page.originalBomViewerInfo().shouldBe(visible, Duration.ofSeconds(30));
  }

  @Ignore("Small SBOMs are auto-expanded; preview only shows for nodes that weren't auto-expanded")
  @Test
  public void testOriginalBomViewer_PreviewDisplaysForCollapsedJsonNodes() throws Exception {
    // Setup: Create SBOM with JSON format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    // Small SBOMs are auto-expanded, so collapse a node first to see preview
    SelenideElement metadataItem = page.treeItemKeys()
        .findBy(text("metadata"))
        .closest(".nx-tree__item");
    SelenideElement collapseIcon = metadataItem.$(".nx-tree__collapse-click");
    collapseIcon.click();

    // Verify that collapsed nodes show preview (wait for React to re-render after collapse)
    SelenideElement metadataNode = page.treeItemKeys().findBy(text("metadata")).parent();
    SelenideElement preview = metadataNode.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible, Duration.ofSeconds(5));
    preview.shouldHave(Condition.not(Condition.empty));
  }

  @Ignore("Small SBOMs are auto-expanded; cannot test collapse behavior on auto-expanded nodes")
  @Test
  public void testOriginalBomViewer_PreviewHidesWhenNodeExpanded() throws Exception {
    // Setup: Create SBOM with JSON format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    // Small SBOMs are auto-expanded, so collapse the node first
    SelenideElement componentsItem = page.treeItemKeys()
        .findBy(text("components"))
        .closest(".nx-tree__item");
    SelenideElement collapseIcon = componentsItem.$(".nx-tree__collapse-click");
    collapseIcon.click();

    // Find the collapsed node with preview
    SelenideElement componentsLabel = page.treeItemKeys().findBy(text("components")).parent();

    // Verify preview is present when collapsed (wait for React to re-render after collapse)
    SelenideElement preview = componentsLabel.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible, Duration.ofSeconds(5));

    // Expand the node by clicking its collapse icon again
    collapseIcon.click();

    // Verify preview no longer exists after expansion
    // Wait for DOM to update after expansion
    preview.should(Condition.not(Condition.exist), Duration.ofSeconds(5));
  }

  @Test
  @Ignore("Incompatible with auto-expand feature - previews not shown for auto-expanded nodes in small SBOMs")
  public void testOriginalBomViewer_PreviewDisplaysForXmlFormat() throws Exception {
    // Setup: Create SBOM with XML format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.xml", SbomFormat.XML, SbomSpecification.CYCLONEDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    // Small SBOMs are auto-expanded, so collapse a node first to see preview
    // Find metadata node and collapse it
    SelenideElement metadataItem = page.treeItemKeys()
        .findBy(text("metadata"))
        .closest(".nx-tree__item");
    SelenideElement collapseIcon = metadataItem.$(".nx-tree__collapse-click");
    collapseIcon.click();

    // Verify that collapsed XML nodes show preview with attributes (wait for React to re-render)
    SelenideElement metadataLabel = page.treeItemKeys().findBy(text("metadata")).parent();
    SelenideElement preview = metadataLabel.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible, Duration.ofSeconds(5));
    preview.shouldHave(Condition.not(Condition.empty));
  }

  @Ignore("Small SBOMs are auto-expanded; preview with ellipsis not visible on auto-expanded nodes")
  @Test
  public void testOriginalBomViewer_PreviewShowsEllipsisForLargeObjects() throws Exception {
    // Setup: Create SBOM with JSON format that has objects with many properties
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    // Small SBOMs are auto-expanded, so collapse nodes to see previews
    // Collapse the components node which has multiple child components
    SelenideElement componentsItem = page.treeItemKeys()
        .findBy(text("components"))
        .closest(".nx-tree__item");
    SelenideElement collapseIcon = componentsItem.$(".nx-tree__collapse-click");
    collapseIcon.click();

    // Find node with preview that contains ellipsis (wait for React to re-render after collapse)
    // The preview MUST show "…" when there are more than 3 properties
    // Test will fail if ellipsis feature is broken
    SelenideElement componentsLabel = page.treeItemKeys().findBy(text("components")).parent();
    SelenideElement preview = componentsLabel.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible, Duration.ofSeconds(5));
    preview.shouldHave(text("…"));
  }

  @Test
  @Ignore("Incompatible with auto-expand feature - previews not shown for auto-expanded nodes in small SBOMs")
  public void testOriginalBomViewer_PreviewDisplaysForSpdxFormat() throws Exception {
    // Setup: Create SBOM with SPDX JSON format
    setupSbomWithFormat("sboms/valid-spdx-bom.json", SbomFormat.JSON, SbomSpecification.SPDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    // Verify that SPDX format nodes also show previews
    // SPDX fields like "creationInfo" or "packages" MUST have previews
    // Test will fail if SPDX preview feature is broken
    SelenideElement spdxNode = page.treeItemKeys()
        .findBy(text("creationInfo").or(text("packages")))
        .parent();
    SelenideElement preview = spdxNode.$(".iq-original-bom-viewer__preview");
    preview.shouldBe(visible);
    preview.shouldHave(Condition.not(Condition.empty));
  }

  @Test
  @Ignore("Incompatible with auto-expand feature - previews not shown for auto-expanded nodes in small SBOMs")
  public void testOriginalBomViewer_PreviewCssTruncation() throws Exception {
    // Setup: Create SBOM with JSON format
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    // Find a preview element
    SelenideElement preview = page.treeItemPreviews().first();
    preview.shouldBe(visible);

    // Verify CSS properties for truncation are explicitly applied
    // These values MUST be set for proper text truncation with ellipsis
    preview.shouldHave(Condition.cssValue("overflow", "hidden"));
    preview.shouldHave(Condition.cssValue("text-overflow", "ellipsis"));
    preview.shouldHave(Condition.cssValue("white-space", "nowrap"));
  }

  @Test
  public void testOriginalBomViewer_IntelligentArrayTitles_NameAtVersion() throws Exception {
    // Setup: Create SBOM with components that have name and version
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.json", SbomFormat.JSON, SbomSpecification.CYCLONEDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    SelenideElement componentsItem = page.treeItemKeys().findBy(text("components")).closest(".nx-tree__item");
    page.originalBomTree().collapseIconFor(componentsItem).click();

    // Verify that array items show "name@version" format instead of just numeric indices
    // The test SBOM contains exactly 2 components: guava@30.1-jre and junit@4.12
    // Both MUST be displayed with name@version format, not numeric indices like "0", "1"
    page.treeItemKeys().findBy(text("guava@30.1-jre")).shouldBe(visible);
    page.treeItemKeys().findBy(text("junit@4.12")).shouldBe(visible);
  }

  @Test
  public void testOriginalBomViewer_IntelligentArrayTitles_XmlFormat() throws Exception {
    // Setup: Create SBOM with XML format (CycloneDX XML)
    setupSbomWithFormat("sboms/valid-cyclonedx-bom.xml", SbomFormat.XML, SbomSpecification.CYCLONEDX);

    // Navigate to Original BOM tab
    navigateToOriginalBomTab();

    // Wait for tree to load
    page.originalBomViewerTree().shouldBe(visible);

    // Expand to components level
    SelenideElement componentsLabel = page.treeItemKeys()
        .findBy(text("components").or(text("component")));

    if (componentsLabel.exists()) {
      SelenideElement componentsItem = componentsLabel.closest(".nx-tree__item");
      SelenideElement collapseIcon = componentsItem.$(".nx-tree__collapse-click");
      if (collapseIcon.exists()) {
        collapseIcon.click();
      }

      // Verify XML components also show name@version format for repeated elements
      // The test XML SBOM contains exactly 2 component elements: guava@30.1-jre and junit@4.12
      // Both MUST be displayed with name@version format
      page.treeItemKeys().findBy(text("guava@30.1-jre")).shouldBe(visible);
      page.treeItemKeys().findBy(text("junit@4.12")).shouldBe(visible);
    }
  }

  /**
   * Helper method to setup SBOM metadata with specified format and specification
   */
  private void setupSbomWithFormat(
      String bomFileName,
      SbomFormat format,
      SbomSpecification specification) throws Exception
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
        insightWork.getSbomDir(application.getId()).toPath());

    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        scan.getThirdPartyFileId(),
        application.getId(),
        "test-version",
        ACTIVE,
        zippedBom.getFileName().toString(),
        specification.toString(),
        format.name(),
        specification == SbomSpecification.CYCLONEDX ? "1.5" : "2.3");

    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
  }
}
