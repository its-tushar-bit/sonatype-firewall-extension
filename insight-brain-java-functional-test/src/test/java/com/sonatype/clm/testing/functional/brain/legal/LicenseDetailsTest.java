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
import com.sonatype.clm.testing.functional.pages.ComponentLicensesDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentLicensesDetailsPage.ComponentLicenseOverview;
import com.sonatype.clm.testing.functional.pages.ComponentLicensesDetailsPage.LicenseList;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;

public class LicenseDetailsTest
    extends AbstractFunctionalTest
{
  private Application app;

  private final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    app = tempEntity.newApplicationWithParent(LicenseDetailsTest.class.getSimpleName(), "app", "org");
    final OwnerComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474", componentId);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");

    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetails.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetailsList.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails/list");
  }

  @Test
  public void testComponentLicenseOverview_ByHash() {
    refreshOrOpen(
        ComponentLicensesDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestComponentLicenseOverview();
  }

  @Test
  public void testComponentLicenseOverview_ByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentLicensesDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestComponentLicenseOverview();
  }

  private void doTestComponentLicenseOverview() {
    final ComponentLicenseOverview overview = ComponentLicensesDetailsPage.componentLicenseOverview();

    overview.getDeclaredLicense().shouldHave(text("Apache-2.0"));
    overview.getObservedLicense().shouldHave(text("GPL-2.0"));
    overview.getEffectiveLicense().shouldHave(text("Apache-2.0, GPL-2.0"));
  }

  @Test
  public void testSelectDifferentLicense_ByHash() {
    refreshOrOpen(
        ComponentLicensesDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestSelectDifferentLicense();
  }

  private void doTestSelectDifferentLicense() {
    final LicenseList licenseList = ComponentLicensesDetailsPage.licenseList();

    licenseList.licenseItem(1).shouldHave(text("Apache-2.0"));
    licenseList.licenseItem(2).shouldHave(text("GPL-2.0"));
  }
}
