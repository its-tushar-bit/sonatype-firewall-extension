/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.pages.LabelEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Color.dark_red;
import static com.sonatype.insight.brain.model.Color.light_green;
import static org.assertj.core.api.Assertions.assertThat;

public class LabelEditorTest
    extends AbstractFunctionalTest
{
  private static final String LABEL_NAME = "a label";

  private LabelDAO labelDAO;

  private Application app;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void init() {
    labelDAO = lookup(LabelDAO.class);

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
    LabelEditorPage.nxColorPicker().color("kiwi").click();
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
    LabelEditorPage.labelNameDiv().shouldHave(cssClass("pristine"));
    LabelEditorPage.labelName().shouldBe(visible).shouldHave(value("original name"));
    LabelEditorPage.descriptionDiv().shouldHave(cssClass("pristine"));
    LabelEditorPage.description().shouldBe(visible).shouldHave(value("original description"));
    LabelEditorPage.nxColorPicker().shouldBe(visible).color("kiwi").shouldHave(cssClass("selected"));
    // when
    LabelEditorPage.labelName().val("updated name");
    LabelEditorPage.description().val("updated description");
    LabelEditorPage.nxColorPicker().color("red").click();
    eyesWatcher.eyesCheck();
    LabelEditorPage.saveButton().shouldBe(enabled).shouldNotHave(cssClass("disabled")).click();
    // then
    LabelEditorPage.title().shouldHave(text("Edit"));
    LabelEditorPage.labelName().shouldBe(visible).shouldHave(value("updated name"));
    LabelEditorPage.description().shouldBe(visible).shouldHave(value("updated description"));
    LabelEditorPage.nxColorPicker().shouldBe(visible).color("red").shouldHave(cssClass("selected"));

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
    NxDeleteModal labelEditorDeleteModal = LabelEditorPage.getDeleteModal();
    labelEditorDeleteModal.shouldBe(visible);
    labelEditorDeleteModal.header().shouldHave(text("Delete Label"));
    labelEditorDeleteModal.alertContent().shouldHave(text(label.getLabel()));
    // when
    labelEditorDeleteModal.closeButton().click();
    // then
    labelEditorDeleteModal.shouldBe(hidden);
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
    NxDeleteModal labelEditorDeleteModal = LabelEditorPage.getDeleteModal();
    labelEditorDeleteModal.submitButton().shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    labelEditorDeleteModal.shouldBe(hidden);

    String createLabelUrl = LabelEditorPage.urlToCreate(OwnerType.ORGANIZATION, app.getOrganizationId());
    waitUntilUrl(createLabelUrl);

    label = labelDAO.getById(label.getId());
    assertThat(label).isNull();
  }

  private void testLabelCreate_testInputValidation() {
    // when invalid name
    LabelEditorPage.labelName().val("$$$");
    // then error on name, disabled save
    LabelEditorPage.labelInvalidMessage().shouldBe(visible).shouldHave(text("Use valid characters"));
    // when valid name, but no color
    LabelEditorPage.labelName().val("valid name");
    LabelEditorPage.descriptionInvalidMessage().shouldNotBe(visible);
    LabelEditorPage.saveButton().shouldBe(enabled).shouldNotHave(cssClass("disabled"));
    // when select color
    LabelEditorPage.nxColorPicker().color("kiwi").click();
    // then save enabled
    LabelEditorPage.saveButton().shouldBe(enabled).shouldNotHave(cssClass("disabled"));
    // when invalid description - too long
    LabelEditorPage.description().val(StringUtils.repeat("a", 256));
    // then error on description and disabled save
    LabelEditorPage.descriptionInvalidMessage()
        .shouldBe(visible)
        .shouldHave(text("Please enter less than 255 characters"));
    // when valid description
    LabelEditorPage.description().val("Description");
    LabelEditorPage.descriptionInvalidMessage().shouldNotBe(visible);
    LabelEditorPage.saveButton().shouldBe(enabled).shouldNotHave(cssClass("disabled"));
  }

  private void assertInitialStateIsCorrect() {
    LabelEditorPage.title().shouldHave(text("New"));
    LabelEditorPage.labelNameDiv().shouldBe(visible, empty).shouldHave(cssClass("pristine"));
    LabelEditorPage.descriptionDiv().shouldBe(visible, empty).shouldHave(cssClass("pristine"));
    LabelEditorPage.nxColorPicker().shouldBe(visible).selectedColor().shouldNot(exist);
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
