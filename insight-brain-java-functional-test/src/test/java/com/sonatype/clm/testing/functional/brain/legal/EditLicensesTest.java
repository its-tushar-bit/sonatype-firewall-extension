/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.EditLicensesModal;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class EditLicensesTest
    extends AbstractFunctionalTest
{
  private Application app;

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  private void init(String hash, ComponentIdentifier componentIdentifier, String testFileSuffix) throws IOException {
    app = tempEntity.newApplicationWithParent(EditLicensesTest.class.getSimpleName(), "app", "org");

    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, componentIdentifier);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse" + testFileSuffix + ".json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");

    testCLMServer.getHdsServer()
        .respondWith(
            IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetails" + testFileSuffix + ".json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetailsList.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails/list");
  }

  @After
  public void after() {
    licenseOverrideDAO.getByOwnerId(ROOT_ORGANIZATION_ID).forEach(licenseOverrideDAO::delete);
  }

  @Test
  public void testEditLicenseByHash() throws IOException {
    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    init("033e7a20b23ea284d474", componentId, "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    doTestEditLicense(app);
  }

  @Test
  public void testEditLicenseByComponentIdentifier() throws IOException {
    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar");
    init("02744a3ac66344569f0b", componentId, "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId));
    doTestEditLicense(organizationDAO.getById(ROOT_ORGANIZATION_ID));
  }

  private void doTestEditLicense(Owner owner) {
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal licensesModal = new EditLicensesModal();
    licensesModal.should(Condition.appear);
    assertThat(licensesModal.header().getText()).isEqualTo("Edit Licenses");
    assertThat(licensesModal.statusDropdown().getText()).isEqualTo("Open");
    assertThat(licensesModal.commentTextInput().getText()).isEmpty();
    assertOption(licensesModal.scopeDropdown().getSelectedOption(), owner);

    licensesModal.save().hover();
    Tooltip.get().shouldBe(Condition.visible).shouldBe(Condition.exactText("There are no changes to save."));
    licensesModal.save().shouldHave(Condition.cssClass("disabled"));
    licensesModal.statusDropdown().click();

    licensesModal.statusOpenOption().shouldBe(Condition.visible);
    licensesModal.statusAcknowledgedOption().shouldBe(Condition.visible);
    licensesModal.statusSelectedOption().shouldBe(Condition.visible);
    licensesModal.statusOverrriddenOption().shouldBe(Condition.visible);
    licensesModal.statusConfirmedption().shouldBe(Condition.visible);

    licensesModal.statusSelectedOption().click();

    licensesModal.save().hover();
    Tooltip.get().shouldNotBe(Condition.visible);
    licensesModal.save().shouldNotHave(Condition.cssClass("disabled"));

    licensesModal.getCheckboxAt(0).shouldBe(Condition.visible);
    licensesModal.getCheckboxAt(1).shouldBe(Condition.visible).click();
    licensesModal.getCheckboxAt(1).shouldBe(Condition.selected);

    licensesModal.scopeDropdown().selectOptionByValue("ROOT_ORGANIZATION_ID");
    eyesWatcher.eyesCheck();

    licensesModal.save().click();
    licensesModal.shouldNotBe(Condition.visible);
  }

  private void assertOption(SelenideElement option, Owner owner) {
    option.shouldHave(Condition.value(owner.getId()));
    option.shouldHave(Condition.exactText(getOptionText(owner)));
  }

  private String getOptionText(Owner owner) {
    return StringUtils.capitalise(owner.getType().toString()) + " - " + owner.getName();
  }
}
