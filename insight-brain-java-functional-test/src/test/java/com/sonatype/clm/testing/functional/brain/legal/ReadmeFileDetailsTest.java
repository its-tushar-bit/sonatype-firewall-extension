/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.ComponentLicenseFileDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentLicenseFileDetailsPage.LicenseFileOverview;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ReadmeFileDetailsTest
    extends AbstractFunctionalTest
{
  private Application app;

  private Organization rootOrg;

  ComponentIdentifier componentId;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    app = tempEntity.newApplicationWithParent(ReadmeFileDetailsTest.class.getSimpleName(), "app", "org");
    componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474",
        componentId);

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalFileHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");

    refreshOrOpen(ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(
        app.getPublicId(), "033e7a20b23ea284d474", 0));
  }

  @Test
  public void testReadmeFileDisplayedByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.LicenseFiles licenseFiles = ComponentLegalOverviewPage.licenseFiles();

    // Verify README file is displayed with correct path and content
    // Should have at least 3 files: 2 LICENSE files + 1 README
    licenseFiles.all().shouldHave(sizeGreaterThan(2));

    // Verify README file is in the list
    licenseFiles.shouldHave(text("README.md"));
    licenseFiles.shouldHave(text("Spring Boot Actuator"));
    licenseFiles.shouldHave(text("Copyright 2020 Spring Contributors"));
  }

  @Test
  public void testReadmeFileDisplayedByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId,
        tempEntity.newRepository().getId()));
    ComponentLegalOverviewPage.LicenseFiles licenseFiles = ComponentLegalOverviewPage.licenseFiles();

    // Verify README file is displayed
    licenseFiles.all().shouldHave(sizeGreaterThan(2));

    // Check that README.md path and content are visible in license files
    licenseFiles.shouldHave(text("README.md"));
    licenseFiles.shouldHave(text("Spring Boot Actuator"));
  }

  @Test
  public void testAddVerifyReadmeByHash() {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));

    String content = "# Custom README\n\nCopyright 2024 Custom Contributors";
    LicenseFileOverview licenseFileOverview = doTestAddVerifyReadme(content);

    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    licenseFileOverview.shouldHave(text(content));
  }

  @Test
  public void testAddVerifyReadmeByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));

    String content = "# Project README\n\nCopyright 2024 Project Team";
    LicenseFileOverview licenseFileOverview = doTestAddVerifyReadme(content);

    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    licenseFileOverview.shouldHave(text(content));
  }

  private LicenseFileOverview doTestAddVerifyReadme(String content) {
    LicenseFileOverview licenseFileOverview = ComponentLicenseFileDetailsPage.licenseFileOverview();

    licenseFileOverview.shouldBe(visible);

    ComponentLegalFile componentReadmeFile =
        tempEntity.newComponentLegalFile(componentId, rootOrg.getId(), LegalFileType.README, "readmeContentHash");
    tempEntity.newLegalFileOverride("readmeContentHash", "hash", content,
        ComponentLegalPartStatus.ENABLED, componentReadmeFile.getId());

    return licenseFileOverview;
  }
}
