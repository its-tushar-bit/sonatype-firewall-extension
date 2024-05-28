/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.OriginalSources;
import com.sonatype.clm.testing.functional.pages.EditOriginalSourcesModal;
import com.sonatype.clm.testing.functional.pages.EditOriginalSourcesModal.StatusDropdownMenu;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EditOriginalSourcesTest
    extends AbstractFunctionalTest
{
  private ComponentObligationDAO componentObligationDAO;

  private Application app;

  private ComponentIdentifier componentId;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    componentObligationDAO = lookup(ComponentObligationDAO.class);
  }

  private void init(String hash, ComponentIdentifier componentIdentifier, String testFileSuffix) {
    componentId = componentIdentifier;
    app = tempEntity.newApplicationWithParent(EditOriginalSourcesTest.class.getSimpleName() + testFileSuffix,
        "app" + testFileSuffix, "org" + testFileSuffix);
    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, componentId);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");

    try {
      testCLMServer.getHdsServer()
          .respondWith(IOUtils
              .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                  StandardCharsets.UTF_8))
          .atUri("/rest/license/metadata");
      testCLMServer.getHdsServer()
          .respondWith(IOUtils.toString(
              this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse" + testFileSuffix + ".json"),
              StandardCharsets.UTF_8))
          .atUri("/rest/legal/comment");
      testCLMServer.getHdsServer()
          .respondWith("[]")
          .atUri("/rest/legal/file");
      testCLMServer.getHdsServer()
          .respondWith(IOUtils.toString(
              this.getClass().getResourceAsStream("/legal/legalSourceLinkHdsResponse.json"),
              StandardCharsets.UTF_8))
          .atUri("/rest/legal/source-link");

      testCLMServer.getHdsServer()
          .respondWith(IOUtils.toString(
              this.getClass().getResourceAsStream("/legal/componentDetails" + testFileSuffix + ".json"),
              StandardCharsets.UTF_8))
          .atUri("rest/ci/componentDetails");
      testCLMServer.getHdsServer()
          .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetailsList.json"),
              StandardCharsets.UTF_8))
          .atUri("rest/ci/componentDetails/list");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  public void testOriginalSourcesValueByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doTestOriginalSourcesValue();
    eyesWatcher.eyesCheck("Component legal edit original sources modal");
  }

  @Test
  public void testOriginalSourcesValueByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    Repository repository = tempEntity.newRepository();
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId, repository.getId()));
    doTestOriginalSourcesValue();
  }

  private void doTestOriginalSourcesValue() {
    OriginalSources originalSources = ComponentLegalOverviewPage.originalSources();
    assertThat(originalSources.at(0)).isNotNull();
    assertThat(originalSources.at(0).value()).isEqualTo("http://link1.com");
    assertThat(originalSources.at(1)).isNotNull();
    assertThat(originalSources.at(1).value()).isEqualTo("https://link2.com");
    assertThat(originalSources.at(2)).isNotNull();
    assertThat(originalSources.at(2).value()).isEqualTo("link3");

    assertThat(ComponentLegalOverviewPage.originalSources().all().shouldHave(CollectionCondition.size(3)));
  }

  @Test
  public void modifyOriginalSourcesValueByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyOriginalSourcesValue();
  }

  @Test
  public void modifyOriginalSourcesValueByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    Repository repository = tempEntity.newRepository();
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId, repository.getId()));
    doModifyOriginalSourcesValue();
  }

  private void doModifyOriginalSourcesValue() {
    ComponentLegalOverviewPage.editOriginalSourcesButton().scrollTo().shouldBe(Condition.visible).click();

    EditOriginalSourcesModal modal = new EditOriginalSourcesModal();
    modal.should(Condition.appear);

    modal.originalSourceInputAt(1).setValue("UPDATED ORIGINAL SOURCE");

    modal.save().shouldNotHave(Condition.cssClass("disabled"));

    modal.save().click();

    modal.should(Condition.disappear);

    OriginalSources originalSources = ComponentLegalOverviewPage.originalSources();
    assertThat(originalSources.at(0)).isNotNull();
    assertThat(originalSources.at(0).value()).isEqualTo("http://link1.com");
    assertThat(originalSources.at(1)).isNotNull();
    assertThat(originalSources.at(1).value()).isEqualTo("link3");
    assertThat(originalSources.at(2)).isNotNull();
    assertThat(originalSources.at(2).value()).isEqualTo("UPDATED ORIGINAL SOURCE");

    assertThat(ComponentLegalOverviewPage.originalSources().all().shouldHave(CollectionCondition.size(3)));
  }

  @Test
  public void modifyOriginalSourceStateByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyOriginalSourceState();
  }

  @Test
  public void modifyOriginalSourceStateByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    Repository repository = tempEntity.newRepository();
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId, repository.getId()));
    doModifyOriginalSourceState();
  }

  private void doModifyOriginalSourceState() {
    ComponentLegalOverviewPage.editOriginalSourcesButton().scrollTo().shouldBe(Condition.visible).click();

    EditOriginalSourcesModal modal = new EditOriginalSourcesModal();
    modal.should(Condition.appear);

    modal.originalSourceStatusCheckboxAt(0).shouldBe(Condition.checked);
    modal.originalSourceStatusToggleAt(0).click();

    modal.originalSourceStatusCheckboxAt(0).shouldNotBe(Condition.checked);
    modal.save().shouldNotHave(Condition.cssClass("disabled")).click();

    modal.should(Condition.disappear);

    OriginalSources originalSources = ComponentLegalOverviewPage.originalSources();
    assertThat(originalSources.at(0).value()).isEqualTo("https://link2.com");
    assertThat(originalSources.at(1).value()).isEqualTo("link3");

    assertThat(ComponentLegalOverviewPage.originalSources().all().shouldHave(CollectionCondition.size(2)));
  }

  @Test
  public void addOriginalSourceByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doAddOriginalSource();
  }

  @Test
  public void addOriginalSourceByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    Repository repository = tempEntity.newRepository();
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId, repository.getId()));
    doAddOriginalSource();
  }

  private void doAddOriginalSource() {
    ComponentLegalOverviewPage.editOriginalSourcesButton().scrollTo().shouldBe(Condition.visible).click();

    EditOriginalSourcesModal modal = new EditOriginalSourcesModal();
    modal.should(Condition.appear);

    modal.originalSourceInputs().shouldHave(CollectionCondition.size(3));

    modal.addSourceButton().click();

    modal.originalSourceInputs().shouldHave(CollectionCondition.size(4));

    modal.originalSourceInputAt(3).shouldHave(Condition.value("")).setValue("NEW SOURCE");
    modal.save().shouldNotHave(Condition.cssClass("disabled")).click();

    modal.should(Condition.disappear);

    OriginalSources originalSources = ComponentLegalOverviewPage.originalSources();
    assertThat(originalSources.at(0)).isNotNull();
    assertThat(originalSources.at(0).value()).isEqualTo("http://link1.com");
    assertThat(originalSources.at(1)).isNotNull();
    assertThat(originalSources.at(1).value()).isEqualTo("https://link2.com");
    assertThat(originalSources.at(2)).isNotNull();
    assertThat(originalSources.at(2).value()).isEqualTo("link3");
    assertThat(originalSources.at(3).value()).isEqualTo("NEW SOURCE");
  }

  @Test
  public void modifyOriginalSourceAndCancelByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyOriginalSourceAndCancel();
  }

  @Test
  public void modifyOriginalSourceAndCancelByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    Repository repository = tempEntity.newRepository();
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId, repository.getId()));
    doModifyOriginalSourceAndCancel();
  }

  public void doModifyOriginalSourceAndCancel() {
    ComponentLegalOverviewPage.editOriginalSourcesButton().scrollTo().shouldBe(Condition.visible).click();

    EditOriginalSourcesModal modal = new EditOriginalSourcesModal();
    modal.should(Condition.appear);

    modal.originalSourceInputs().shouldHave(CollectionCondition.size(3));
    modal.originalSourceInputAt(1).setValue("UPDATED SOURCE");
    modal.addSourceButton().click();
    modal.originalSourceInputs().shouldHave(CollectionCondition.size(4));
    modal.originalSourceInputAt(3).shouldHave(Condition.value("")).setValue("NEW SOURCE");
    modal.cancel().shouldNotHave(Condition.cssClass("disabled")).click();

    modal.should(Condition.disappear);

    OriginalSources originalSources = ComponentLegalOverviewPage.originalSources();
    assertThat(originalSources.at(0)).isNotNull();
    assertThat(originalSources.at(0).value()).isEqualTo("http://link1.com");
    assertThat(originalSources.at(1)).isNotNull();
    assertThat(originalSources.at(1).value()).isEqualTo("https://link2.com");
    assertThat(originalSources.at(2)).isNotNull();
    assertThat(originalSources.at(2).value()).isEqualTo("link3");

    assertThat(ComponentLegalOverviewPage.originalSources().all().shouldHave(CollectionCondition.size(3)));
  }

  @Test
  public void modifyObligationStatusByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyObligationStatus();
  }

  @Test
  public void modifyObligationStatusByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    Repository repository = tempEntity.newRepository();
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId, repository.getId()));
    doModifyObligationStatus();
  }

  private void doModifyObligationStatus() {
    ComponentLegalOverviewPage.editOriginalSourcesButton().scrollTo().shouldBe(Condition.visible).click();

    assertThat(componentObligationDAO.getByOwnerIdAndComponentIdentifierAndObligationName(
        Organization.ROOT_ORGANIZATION_ID, componentId,
        "Required Disclosure of Original Source Code with Distribution")).isNull();

    EditOriginalSourcesModal modal = new EditOriginalSourcesModal();
    modal.should(Condition.appear);

    modal.statusDropdown().shouldBe(Condition.visible);
    modal.statusDropdown().shouldBe(enabled);
    modal.statusDropdown().selectedStatus().shouldHave(text("Unreviewed"));

    // when we pull down the list
    modal.statusDropdown().click();
    StatusDropdownMenu menu = modal.statusDropdown().dropdownMenu();

    // then the status list is complete, except for the selected status.
    assertThat(String.join(",", menu.options().texts())).isEqualTo("Fulfilled,Flagged,Not Applicable");

    // Changing the status
    modal.statusDropdownItems().find(Condition.exactText("Flagged")).click();

    // should enable the save button
    modal.save().shouldNotHave(Condition.cssClass("disabled")).click();
    modal.should(Condition.disappear);

    // The status in the DB should change
    assertThat(componentObligationDAO.getByOwnerIdAndComponentIdentifierAndObligationName(
        Organization.ROOT_ORGANIZATION_ID, componentId, "Required Disclosure of Original Source Code with Distribution")
        .getStatus()).isEqualTo(ObligationStatus.FLAGGED);
  }
}
