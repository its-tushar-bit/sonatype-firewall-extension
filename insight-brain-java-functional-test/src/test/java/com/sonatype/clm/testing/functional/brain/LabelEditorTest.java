/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.LabelEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Color.dark_red;
import static com.sonatype.insight.brain.model.Color.light_green;
import static org.assertj.core.api.Assertions.assertThat;

public class LabelEditorTest
    extends AbstractFunctionalTest
{
  private static final String LABEL_NAME = "a label";

  private LabelDAO labelDAO = new LabelDAO();

  private Application app;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void init() {
    app = tempEntity.newApplicationWithParent("test_app", "LabelEditorTest app");
    refreshOrOpen(OwnerSummaryPage.url(app));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(app.getName()));
  }

  @After
  public void cleanup() {
    Label label = getLabelByName(app.getId(), LABEL_NAME);
    if (label != null) {
      labelDAO.delete(label);
    }
  }

  @Test
  public void testLabelCreate() {
    // given
    OwnerSummaryPage.labelTile().addLabelButton().click();
    assertInitialStateIsCorrect();
    testLabelCreate_testInputValidation();
    // when
    LabelEditorPage.labelName().val(LABEL_NAME);
    LabelEditorPage.description().val("a description");
    LabelEditorPage.colorPicker().color(light_green).click();
    LabelEditorPage.saveButton().click();
    // then
    assertInitialStateIsCorrect();
    Label label = getLabelByName(app.getId(), LABEL_NAME);
    assertThat(label).isNotNull();
    assertThat(label.getOwnerId()).isEqualTo(app.getId());
    assertThat(label.getLabel()).isEqualTo(LABEL_NAME);
    assertThat(label.getDescription()).isEqualTo("a description");
    assertThat(label.getColor()).isEqualTo(light_green);
  }

  @Test
  public void testLabelEdit() {
    // given
    Label label = tempEntity.newLabel(app.getOrganizationId(), "original name", "original description", light_green);
    refreshOrOpen(OwnerSummaryPage.url(OwnerType.ORGANIZATION, app.getOrganizationId()));
    OwnerSummaryPage.labelTile().localLabel(label.getLabel()).click();
    LabelEditorPage.title().shouldHave(text("Edit"));
    LabelEditorPage.labelName().shouldBe(visible).shouldHave(CLM.PRISTINE).shouldHave(value("original name"));
    LabelEditorPage.description().shouldBe(visible).shouldHave(CLM.PRISTINE)
        .shouldHave(value("original description"));
    LabelEditorPage.colorPicker().shouldBe(visible).color(light_green).shouldBe(CLM.SELECTED);
    LabelEditorPage.saveButton().shouldHave(DISABLED);
    // when
    LabelEditorPage.labelName().val("updated name");
    LabelEditorPage.description().val("updated description");
    LabelEditorPage.colorPicker().color(dark_red).click();
    eyesWatcher.eyesCheck();
    LabelEditorPage.saveButton().shouldBe(enabled).shouldNotHave(DISABLED).click();
    // then
    LabelEditorPage.title().shouldHave(text("Edit"));
    LabelEditorPage.labelName().shouldBe(visible).shouldHave(value("updated name"));
    LabelEditorPage.description().shouldBe(visible).shouldHave(value("updated description"));
    LabelEditorPage.colorPicker().shouldBe(visible).color(dark_red).shouldBe(CLM.SELECTED);
    LabelEditorPage.saveButton().shouldHave(DISABLED);

    label = getLabelByName(app.getOrganizationId(), "updated name");
    assertThat(label).isNotNull();
    assertThat(label.getOwnerId()).isEqualTo(app.getOrganizationId());
    assertThat(label.getLabel()).isEqualTo("updated name");
    assertThat(label.getDescription()).isEqualTo("updated description");
    assertThat(label.getColor()).isEqualTo(dark_red);
  }

  @Test
  public void testDeleteLabelModal() {
    // given
    Label label = tempEntity.newLabel(app.getId());
    refreshOrOpen(LabelEditorPage.urlToEdit(app, label.getId()));
    // when
    LabelEditorPage.deleteButton().shouldBe(visible).click();
    // then
    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Label"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(label.getLabel()));
    // when
    DeleteModal.cancelButton().click();
    // then
    DeleteModal.root().shouldBe(hidden);
    LabelEditorPage.labelName().shouldHave(value(label.getLabel()));
    label = labelDAO.getById(label.getId());
    assertThat(label).isNotNull();
  }

  @Test
  public void testDeleteLabel() {
    // given
    Label label = tempEntity.newLabel(app.getOrganizationId());
    refreshOrOpen(LabelEditorPage.urlToEdit(OwnerType.ORGANIZATION, app.getOrganizationId(), label.getId()));
    // when
    LabelEditorPage.deleteButton().shouldBe(visible).click();
    DeleteModal.continueButton().shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    String createLabelUrl = LabelEditorPage.urlToCreate(OwnerType.ORGANIZATION, app.getOrganizationId());
    waitUntilUrl(createLabelUrl);

    label = labelDAO.getById(label.getId());
    assertThat(label).isNull();
  }

  private void testLabelCreate_testInputValidation() {
    // when invalid name
    LabelEditorPage.labelName().val("$$$");
    // then error on name, disabled save
    popoverViolations(LabelEditorPage.labelName()).shouldBe(visible).shouldHave(text("Use valid characters"));
    LabelEditorPage.saveButton().shouldHave(DISABLED);
    // when valid name, but no color
    LabelEditorPage.labelName().val("valid name");
    // then error on name is gone, but save still disabled. TODO check color 'field' validation error after CLM-5436
    popoverViolations(LabelEditorPage.labelName()).shouldNot(exist);
    LabelEditorPage.saveButton().shouldHave(DISABLED);
    // when select color
    LabelEditorPage.colorPicker().color(light_green).click();
    // then save enabled
    LabelEditorPage.saveButton().shouldBe(enabled).shouldNotHave(DISABLED);
    // when invalid description - too long
    LabelEditorPage.description().val(StringUtils.repeat("a", 256));
    // then error on description and disabled save
    popoverViolations(LabelEditorPage.description()).shouldBe(visible).shouldHave(text("Maximum length"));
    LabelEditorPage.saveButton().shouldHave(DISABLED);
    // when valid description
    LabelEditorPage.description().val("Description");
    popoverViolations(LabelEditorPage.description()).shouldNot(exist);
    LabelEditorPage.saveButton().shouldBe(enabled).shouldNotHave(DISABLED);
  }

  private void assertInitialStateIsCorrect() {
    LabelEditorPage.title().shouldHave(text("New"));
    LabelEditorPage.labelName().shouldBe(visible, empty).shouldHave(CLM.PRISTINE);
    LabelEditorPage.description().shouldBe(visible, empty).shouldHave(CLM.PRISTINE);
    LabelEditorPage.colorPicker().shouldBe(visible).selectedColor().shouldNot(exist);
    LabelEditorPage.saveButton().shouldHave(DISABLED);
  }

  private Label getLabelByName(String ownerId, String labelName) {
    for (Label label : labelDAO.getByOwnerId(ownerId)) {
      if (labelName.equals(label.getLabel())) {
        return label;
      }
    }
    return null;
  }
}
