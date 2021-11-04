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
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.CopyrightStatements;
import com.sonatype.clm.testing.functional.pages.EditCopyrightsModal;
import com.sonatype.clm.testing.functional.pages.EditCopyrightsModal.StatusDropdownMenu;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import org.apache.commons.io.IOUtils;
import org.apache.mina.core.RuntimeIoException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EditCopyrightsTest
    extends AbstractFunctionalTest
{
  private Application app;

  private ComponentIdentifier componentId;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  private void init(String hash, ComponentIdentifier componentIdentifier, String testFileSuffix) {
    componentId = componentIdentifier;
    app = tempEntity.newApplicationWithParent(EditCopyrightsTest.class.getSimpleName() + testFileSuffix,
        "app" + testFileSuffix, "org" + testFileSuffix);
    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, componentId);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");
    tempEntity.newComponentObligation(componentId, app.getId(), "Inclusion of Copyright", null, ObligationStatus.OPEN,
        "NA");

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
              this.getClass().getResourceAsStream("/legal/componentDetails" + testFileSuffix + ".json"),
              StandardCharsets.UTF_8))
          .atUri("rest/ci/componentDetails");
      testCLMServer.getHdsServer()
          .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetailsList.json"),
              StandardCharsets.UTF_8))
          .atUri("rest/ci/componentDetails/list");
    }
    catch (IOException e) {
      throw new RuntimeIoException(e);
    }
  }

  @Test
  public void testCopyrightsValueByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doTestCopyrightsValue();
    eyesWatcher.eyesCheck("Component legal edit copyrights modal");
  }

  @Test
  public void testCopyrightsValueByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId));
    doTestCopyrightsValue();
  }

  private void doTestCopyrightsValue() {
    CopyrightStatements copyrightStatements = ComponentLegalOverviewPage.copyrightStatements();
    assertThat(copyrightStatements.at(0)).isNotNull();
    assertThat(copyrightStatements.at(0).value()).isEqualTo("Copyright SomeDeveloper 2017");
    assertThat(copyrightStatements.at(1)).isNotNull();
    assertThat(copyrightStatements.at(1).value())
        .isEqualTo("Copyright SomeDeveloper 2018-2019 All Right reserved");
    assertThat(copyrightStatements.at(2)).isNotNull();
    assertThat(copyrightStatements.at(2).value())
        .isEqualTo("Copyright SomeDeveloper 2019-2020");

    assertThat(ComponentLegalOverviewPage.copyrightStatements().all().size()).isEqualTo(3);
    eyesWatcher.eyesCheck("Component legal edit copyrights modal");
  }

  @Test
  public void modifyCopyrightsValueByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyCopyrightsValue();
  }

  @Test
  public void modifyCopyrightsValueByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId));
    doModifyCopyrightsValue();
  }

  private void doModifyCopyrightsValue() {
    ComponentLegalOverviewPage.editCopyrightButton().shouldBe(Condition.visible).click();

    EditCopyrightsModal modal = new EditCopyrightsModal();
    modal.should(Condition.appear);
    modal.save().shouldHave(Condition.cssClass("disabled"));

    modal.copyrightInputAt(1).setValue("UPDATED COPYRIGHT");

    modal.save().shouldNotHave(Condition.cssClass("disabled"));

    modal.save().click();

    modal.should(Condition.disappear);

    CopyrightStatements copyrightStatements = ComponentLegalOverviewPage.copyrightStatements();
    assertThat(copyrightStatements.at(0).value()).isEqualTo("Copyright SomeDeveloper 2017");
    assertThat(copyrightStatements.at(1).value())
        .isEqualTo("Copyright SomeDeveloper 2019-2020");
    assertThat(copyrightStatements.at(2).value())
        .isEqualTo("UPDATED COPYRIGHT");

    assertThat(ComponentLegalOverviewPage.copyrightStatements().all().size()).isEqualTo(3);
  }

  @Test
  public void modifyCopyrightStateByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyCopyrightState();
  }

  @Test
  public void modifyCopyrightStateByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId));
    doModifyCopyrightState();
  }

  private void doModifyCopyrightState() {
    ComponentLegalOverviewPage.editCopyrightButton().shouldBe(Condition.visible).click();

    EditCopyrightsModal modal = new EditCopyrightsModal();
    modal.should(Condition.appear);
    modal.save().shouldHave(Condition.cssClass("disabled"));

    modal.copyrightStatusCheckboxAt(0).shouldBe(Condition.checked);
    modal.copyrightStatusToggleAt(0).click();

    modal.copyrightStatusCheckboxAt(0).shouldNotBe(Condition.checked);
    modal.save().shouldNotHave(Condition.cssClass("disabled")).click();

    modal.should(Condition.disappear);

    CopyrightStatements copyrightStatements = ComponentLegalOverviewPage.copyrightStatements();
    assertThat(copyrightStatements.at(0).value())
        .isEqualTo("Copyright SomeDeveloper 2018-2019 All Right reserved");
    assertThat(copyrightStatements.at(1).value())
        .isEqualTo("Copyright SomeDeveloper 2019-2020");

    assertThat(ComponentLegalOverviewPage.copyrightStatements().all().size()).isEqualTo(2);
  }

  @Test
  public void addCopyrightByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doAddCopyright();
  }

  @Test
  public void addCopyrightByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId));
    doAddCopyright();
  }

  private void doAddCopyright() {
    ComponentLegalOverviewPage.editCopyrightButton().shouldBe(Condition.visible).click();

    EditCopyrightsModal modal = new EditCopyrightsModal();
    modal.should(Condition.appear);
    modal.save().shouldHave(Condition.cssClass("disabled"));

    modal.copyrightInputs().shouldHave(CollectionCondition.size(3));

    modal.addCopyrightButton().click();

    modal.copyrightInputs().shouldHave(CollectionCondition.size(4));

    modal.save().shouldHave(Condition.cssClass("disabled"));

    modal.copyrightInputAt(3).shouldHave(Condition.value("")).setValue("NEW COPYRIGHT");
    modal.save().shouldNotHave(Condition.cssClass("disabled")).click();

    modal.should(Condition.disappear);

    CopyrightStatements copyrightStatements = ComponentLegalOverviewPage.copyrightStatements();
    assertThat(copyrightStatements.at(0).value()).isEqualTo("Copyright SomeDeveloper 2017");
    assertThat(copyrightStatements.at(1).value())
        .isEqualTo("Copyright SomeDeveloper 2018-2019 All Right reserved");
    assertThat(copyrightStatements.at(2).value())
        .isEqualTo("Copyright SomeDeveloper 2019-2020");
    assertThat(copyrightStatements.at(3).value())
        .isEqualTo("NEW COPYRIGHT");
  }

  @Test
  public void modifyCopyrightAndCancelByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyCopyrightAndCancel();
  }

  @Test
  public void modifyCopyrightAndCancelByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId));
    doModifyCopyrightAndCancel();
  }

  public void doModifyCopyrightAndCancel() {
    ComponentLegalOverviewPage.editCopyrightButton().shouldBe(Condition.visible).click();

    EditCopyrightsModal modal = new EditCopyrightsModal();
    modal.should(Condition.appear);
    modal.save().shouldHave(Condition.cssClass("disabled"));

    modal.copyrightInputs().shouldHave(CollectionCondition.size(3));
    modal.copyrightInputAt(1).setValue("UPDATED COPYRIGHT");
    modal.addCopyrightButton().click();
    modal.copyrightInputs().shouldHave(CollectionCondition.size(4));
    modal.copyrightInputAt(3).shouldHave(Condition.value("")).setValue("NEW COPYRIGHT");
    modal.cancel().shouldNotHave(Condition.cssClass("disabled")).click();

    modal.should(Condition.disappear);

    CopyrightStatements copyrightStatements = ComponentLegalOverviewPage.copyrightStatements();
    assertThat(copyrightStatements.at(0).value()).isEqualTo("Copyright SomeDeveloper 2017");
    assertThat(copyrightStatements.at(1).value())
        .isEqualTo("Copyright SomeDeveloper 2018-2019 All Right reserved");
    assertThat(copyrightStatements.at(2).value())
        .isEqualTo("Copyright SomeDeveloper 2019-2020");

    assertThat(ComponentLegalOverviewPage.copyrightStatements().all().size()).isEqualTo(3);
  }

  @Test
  @Ignore
  public void modifyObligationStatusByHash() {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doModifyObligationStatus();
  }

  @Test
  @Ignore
  public void modifyObligationStatusByComponentIdentifier() throws UnsupportedEncodingException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId));
    doModifyObligationStatus();
  }

  private void doModifyObligationStatus() {
    ComponentLegalOverviewPage.editCopyrightButton().shouldBe(Condition.visible).click();

    assertThat(new ComponentObligationDAO().getByOwnerIdAndComponentIdentifierAndObligationName(
        Organization.ROOT_ORGANIZATION_ID, componentId, "Inclusion of Copyright")).isNull();

    EditCopyrightsModal modal = new EditCopyrightsModal();
    modal.should(Condition.appear);
    modal.save().shouldHave(Condition.cssClass("disabled"));

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
    assertThat(new ComponentObligationDAO().getByOwnerIdAndComponentIdentifierAndObligationName(
        Organization.ROOT_ORGANIZATION_ID, componentId, "Inclusion of Copyright").getStatus())
            .isEqualTo(ObligationStatus.FLAGGED);
  }
}
