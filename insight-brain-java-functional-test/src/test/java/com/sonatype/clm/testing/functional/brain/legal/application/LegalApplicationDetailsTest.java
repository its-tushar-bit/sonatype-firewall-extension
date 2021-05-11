/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal.application;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.functional.pages.LegalApplicationDetailsPage.ComponentTable;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;

public class LegalApplicationDetailsTest
    extends AbstractFunctionalTest
{
  private Application app;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  private void addComponentAndLicenses(
      String groupId,
      String artifactId,
      String version,
      String hash,
      String... licenseIds)
  {
    final ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    final ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
            componentIdentifier);
    Arrays.stream(licenseIds)
        .forEach(licenseId -> tempEntity.newApplicationComponentLicense(applicationComponent.getId(), licenseId));
  }

  @Before
  public void setUp() throws Exception {
    app = tempEntity.newApplicationWithParent(LegalApplicationDetailsPage.class.getSimpleName());

    addComponentAndLicenses("org.package", "component1", "1.0", "hash1", "Apache-2.0");
    addComponentAndLicenses("org.package", "component2", "2.0", "hash2", "BSD-3-Clause");
    addComponentAndLicenses("com.package", "component1", "3.0", "hash3", "BSD-2-Clause");
    tempEntity.newComponentObligation(ComponentIdentifier.createMavenCoordinates("org.package", "component1", "1.0"),
        app.getId(), "Inclusion of Notice", "comment", ObligationStatus.FULFILLED, "hash1");
    tempEntity.newComponentObligation(ComponentIdentifier.createMavenCoordinates("org.package", "component2", "2.0"),
        app.getId(), "Inclusion of Notice", "comment", ObligationStatus.FULFILLED, "hash2");
    tempEntity.newComponentObligation(ComponentIdentifier.createMavenCoordinates("com.package", "component1", "3.0"),
        app.getId(), "Inclusion of Notice", "comment", ObligationStatus.FLAGGED, "hash3");

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
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalFileHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");

    refreshOrOpen(LegalApplicationDetailsPage.urlToApplicationScope(app.getPublicId(), "build"));
  }

  @Test
  public void testApplicationDetails() {
    final SelenideElement title = LegalApplicationDetailsPage.title();

    title.shouldHave(text(app.getName() + " Obligations"));
  }

  @Test
  public void testComponentsTablePresent() {
    final ComponentTable componentTable = LegalApplicationDetailsPage.componentTable();
    componentTable.rows().shouldHave(CollectionCondition.size(3));

    componentTable.componentNames().shouldHave(CollectionCondition.textsInAnyOrder(
        "org.package : component1 : 1.0", "org.package : component2 : 2.0",
        "com.package : component1 : 3.0"));

    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder(
        "Apache-2.0", "BSD-3-Clause", "BSD-2-Clause"));
  }

  @Test
  public void testFilterByComponentName() {
    final ComponentTable componentTable = LegalApplicationDetailsPage.componentTable();

    componentTable.componentNameFilter().setValue("org.");
    componentTable.componentNames().shouldHave(CollectionCondition.textsInAnyOrder(
        "org.package : component1 : 1.0", "org.package : component2 : 2.0"));
    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder("Apache-2.0", "BSD-3-Clause"));

    componentTable.componentNameFilter().setValue("component1");
    componentTable.componentNames().shouldHave(CollectionCondition.textsInAnyOrder(
        "org.package : component1 : 1.0", "com.package : component1 : 3.0"));
    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder("Apache-2.0", "BSD-2-Clause"));

    // setting value to empty doesn't trigger onChange event, so sending a wall of backspaces instead
    componentTable.componentNameFilter().sendKeys("\b\b\b\b\b\b\b\b\b");
    componentTable.componentNames().shouldHave(CollectionCondition.textsInAnyOrder(
        "org.package : component1 : 1.0", "org.package : component2 : 2.0",
        "com.package : component1 : 3.0"));

    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder(
        "Apache-2.0", "BSD-3-Clause", "BSD-2-Clause"));
  }

  @Test
  public void testFilterByLicense() {
    final ComponentTable componentTable = LegalApplicationDetailsPage.componentTable();

    componentTable.licenseFilter().setValue("Apa");
    componentTable.componentNames().shouldHave(CollectionCondition.texts("org.package : component1 : 1.0"));
    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder("Apache-2.0"));

    componentTable.licenseFilter().setValue("BSD");
    componentTable.componentNames().shouldHave(CollectionCondition.textsInAnyOrder(
        "com.package : component1 : 3.0", "org.package : component2 : 2.0"));
    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder("BSD-2-Clause", "BSD-3-Clause"));

    componentTable.licenseFilter().sendKeys("\b\b\b\b");
    componentTable.componentNames().shouldHave(CollectionCondition.textsInAnyOrder(
        "org.package : component1 : 1.0", "org.package : component2 : 2.0",
        "com.package : component1 : 3.0"));

    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder(
        "Apache-2.0", "BSD-3-Clause", "BSD-2-Clause"));
  }

  @Test
  public void testFilterByComponentNameAndLicense() {
    final ComponentTable componentTable = LegalApplicationDetailsPage.componentTable();

    componentTable.componentNameFilter().setValue("org.package");
    componentTable.licenseFilter().setValue("BSD");
    componentTable.componentNames().shouldHave(CollectionCondition.texts("org.package : component2 : 2.0"));
    componentTable.licenses().shouldHave(CollectionCondition.textsInAnyOrder("BSD-3-Clause"));
  }

  @Test
  public void testSortByComponent() {
    final ComponentTable componentTable = LegalApplicationDetailsPage.componentTable();

    componentTable.componentNames().shouldHaveSize(3);
    eyesWatcher.eyesCheck();
    componentTable.sortByComponent().click();
    componentTable.componentNames().get(0).shouldHave(text("com.package : component1 : 3.0"));
    componentTable.componentNames().get(1).shouldHave(text("org.package : component1 : 1.0"));
    componentTable.componentNames().get(2).shouldHave(text("org.package : component2 : 2.0"));
    componentTable.sortByComponent().click();
    componentTable.componentNames().get(0).shouldHave(text("org.package : component2 : 2.0"));
    componentTable.componentNames().get(1).shouldHave(text("org.package : component1 : 1.0"));
    componentTable.componentNames().get(2).shouldHave(text("com.package : component1 : 3.0"));
  }

  @Test
  public void testSortByLicenses() {
    final ComponentTable componentTable = LegalApplicationDetailsPage.componentTable();

    componentTable.componentNames().shouldHaveSize(3);
    componentTable.sortByLicenses().click();
    componentTable.licenses().get(0).shouldHave(text("Apache-2.0"));
    componentTable.licenses().get(1).shouldHave(text("BSD-2-Clause"));
    componentTable.licenses().get(2).shouldHave(text("BSD-3-Clause"));
    componentTable.sortByLicenses().click();
    componentTable.licenses().get(0).shouldHave(text("BSD-3-Clause"));
    componentTable.licenses().get(1).shouldHave(text("BSD-2-Clause"));
    componentTable.licenses().get(2).shouldHave(text("Apache-2.0"));
  }
}
