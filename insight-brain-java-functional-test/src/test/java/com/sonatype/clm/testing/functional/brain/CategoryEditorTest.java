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
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.CategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.CategoryEditorPage.DeleteErrorModal;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;

import org.apache.commons.lang3.StringUtils;
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

public class CategoryEditorTest
    extends AbstractFunctionalTest
{
  private static final String CATEGORY_NAME = "a category";

  private TagDAO tagDAO = new TagDAO();

  private ApplicationTagDAO appTagDao = new ApplicationTagDAO();

  private Organization org;

  private Tag category;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization("CategoryEditorTest Organization");
    category = tempEntity.newTag(org.getId(), "original name", "original description", light_green);
    refreshOrOpen(OwnerSummaryPage.url(org));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(org.getName()));
  }

  @Test
  public void testCreateCategory() {
    OwnerSummaryPage.categoryTile().addCategoryButton().click();
    assertInitialStateIsCorrect();
    CategoryEditorPage.categoryName().val("$$$"); // invalid characters
    PopoverViolations.on(CategoryEditorPage.categoryName()).shouldShowInvalidCharactersError();
    CategoryEditorPage.saveButton().shouldHave(DISABLED);

    CategoryEditorPage.categoryName().val(CATEGORY_NAME); // description and color are mandatory as well
    // TODO check color 'field' validation error after CLM-5436
    PopoverViolations.on(CategoryEditorPage.categoryName()).shouldNotExist();
    CategoryEditorPage.saveButton().shouldHave(DISABLED);

    CategoryEditorPage.description().val(StringUtils.repeat("Long text ", 26)); // too long
    PopoverViolations.on(CategoryEditorPage.description()).shouldShowMaxLengthError();

    // take focus off the input to prevent blinking cursor
    SidebarNavigation.productVersion().click();
    eyesWatcher.eyesCheck();
    CategoryEditorPage.saveButton().shouldHave(DISABLED);

    CategoryEditorPage.description().val("Description");
    PopoverViolations.on(CategoryEditorPage.description()).shouldNotExist();
    CategoryEditorPage.saveButton().shouldHave(DISABLED); // color still missing

    CategoryEditorPage.colorPicker().color(light_green).click();
    CategoryEditorPage.saveButton().shouldBe(enabled).shouldNotHave(DISABLED).click();

    assertInitialStateIsCorrect(); // form reset

    Tag category = getCategoryByName(org.getId(), CATEGORY_NAME);
    assertThat(category).isNotNull();
    assertThat(category.getOrganizationId()).isEqualTo(org.getId());
    assertThat(category.getName()).isEqualTo(CATEGORY_NAME);
    assertThat(category.getDescription()).isEqualTo("Description");
    assertThat(category.getColor()).isEqualTo(light_green);
  }

  @Test
  public void testEditCategory() {
    OwnerSummaryPage.categoryTile().localCategory(category.getName()).click();
    CategoryEditorPage.title().shouldHave(text("Edit"));
    CategoryEditorPage.categoryName().shouldBe(visible).shouldHave(CLM.PRISTINE)
        .shouldHave(value("original name"));
    CategoryEditorPage.description().shouldBe(visible).shouldHave(CLM.PRISTINE)
        .shouldHave(value("original description"));
    CategoryEditorPage.colorPicker().shouldBe(visible).color(light_green).shouldBe(CLM.SELECTED);
    CategoryEditorPage.saveButton().shouldHave(DISABLED);
    // when
    CategoryEditorPage.categoryName().val("updated name");
    CategoryEditorPage.description().val("updated description");
    CategoryEditorPage.colorPicker().color(dark_red).click();
    CategoryEditorPage.saveButton().shouldBe(enabled).shouldNotHave(DISABLED).click();
    // then
    CategoryEditorPage.title().shouldHave(text("Edit"));
    CategoryEditorPage.categoryName().shouldBe(visible).shouldHave(value("updated name"));
    CategoryEditorPage.description().shouldBe(visible).shouldHave(value("updated description"));
    CategoryEditorPage.colorPicker().shouldBe(visible).color(dark_red).shouldBe(CLM.SELECTED);
    CategoryEditorPage.saveButton().shouldHave(DISABLED);

    category = getCategoryByName(org.getId(), "updated name");
    assertThat(category).isNotNull();
    assertThat(category.getOrganizationId()).isEqualTo(org.getId());
    assertThat(category.getName()).isEqualTo("updated name");
    assertThat(category.getDescription()).isEqualTo("updated description");
    assertThat(category.getColor()).isEqualTo(dark_red);
  }

  @Test
  public void testDeleteCategory() {
    // given
    refreshOrOpen(CategoryEditorPage.urlToEdit(org.getId(), category.getId()));
    // when
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    // then
    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Application Category"));
    DeleteModal.body().shouldHave(CategoryEditorPage.deleteWarningText());
    // when
    DeleteModal.cancelButton().click();
    // then
    DeleteModal.root().shouldBe(hidden);
    CategoryEditorPage.categoryName().shouldHave(value(category.getName()));
    category = tagDAO.getById(category.getId());
    assertThat(category).isNotNull();
    // when
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    // then
    DeleteModal.continueButton().shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    waitUntilUrl(CategoryEditorPage.urlToCreate(org.getId()));

    category = tagDAO.getById(category.getId());
    assertThat(category).isNull();
  }

  @Test
  public void testDeleteCategoryAssociatedToAnApp() {
    // given
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(app.getId(), category.getId());

    refreshOrOpen(CategoryEditorPage.urlToEdit(org.getId(), category.getId()));
    refresh();
    // when
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    // then
    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Application Category"));
    DeleteModal.body().shouldHave(CategoryEditorPage.deleteWarningText(app.getName()));
    DeleteModal.continueButton().shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    waitUntilUrl(CategoryEditorPage.urlToCreate(org.getId()));

    assertThat(tagDAO.getById(category.getId())).isNull();
    assertThat(appTagDao.getByApplicationIdAndTagId(app.getId(), category.getId())).isNull();
  }

  @Test
  public void testDeleteCategoryAssociatedToPolicy() {
    Tag category = tempEntity.newTag(org.getId());
    Policy policy1 = tempEntity.newPolicy();
    Policy policy2 = tempEntity.newPolicy();
    tempEntity.newPolicyTag(policy1.getId(), category.getId());
    tempEntity.newPolicyTag(policy2.getId(), category.getId());

    refreshOrOpen(CategoryEditorPage.urlToEdit(org.getId(), category.getId()));
    refresh();
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    DeleteErrorModal.root().shouldBe(visible);
    DeleteErrorModal.message()
        .shouldHave(DeleteErrorModal.associatedPoliciesText(policy1.getName(), policy2.getName()));
    DeleteErrorModal.closeButton().shouldBe(visible).click();
    DeleteErrorModal.root().shouldBe(hidden);
  }

  private void assertInitialStateIsCorrect() {
    CategoryEditorPage.title().shouldHave(text("New"));
    CategoryEditorPage.categoryName().shouldBe(visible, empty).shouldHave(CLM.PRISTINE);
    CategoryEditorPage.description().shouldBe(visible, empty).shouldHave(CLM.PRISTINE);
    CategoryEditorPage.colorPicker().shouldBe(visible).selectedColor().shouldNot(exist);
    CategoryEditorPage.saveButton().shouldHave(DISABLED);
  }

  private Tag getCategoryByName(String organizationId, String categoryName) {
    for (Tag tag : tagDAO.getByOrganizationId(organizationId)) {
      if (categoryName.equals(tag.getName())) {
        return tag;
      }
    }
    return null;
  }
}
