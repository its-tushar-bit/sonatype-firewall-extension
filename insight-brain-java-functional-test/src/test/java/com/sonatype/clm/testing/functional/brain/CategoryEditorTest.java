/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.CategoryEditorPage;
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

public class CategoryEditorTest
    extends AbstractFunctionalTest
{
  private static final String CATEGORY_NAME = "a category";

  private TagDAO tagDAO;

  private ApplicationTagDAO appTagDao;

  private Organization org;

  private Tag category;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void init() {
    tagDAO = lookup(TagDAO.class);
    appTagDao = lookup(ApplicationTagDAO.class);

    org = tempEntity.newOrganization("CategoryEditorTest Organization");
    category = tempEntity.newTag(org.getId(), "original name", "original description", light_green);
    refreshOrOpen(OwnerSummaryPage.url(org));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(org.getName()));
  }

  @Test
  public void testCreateCategory() {
    OwnerSummaryPage.categoryTile().addCategoryButton().click();
    assertInitialStateIsCorrect();

    // invalid characters scenario
    CategoryEditorPage.categoryName().val("$$$");
    CategoryEditorPage.categoryInvalidMessage().shouldBe(visible).shouldHave(text("Use valid characters"));

    // name is valid, but description and color are mandatory as well
    CategoryEditorPage.categoryName().val(CATEGORY_NAME);
    CategoryEditorPage.categoryInvalidMessage().shouldNotBe(visible);

    // when invalid description - too long
    CategoryEditorPage.description().val(StringUtils.repeat("a", 256));
    // then error on description and disabled save
    CategoryEditorPage.descriptionInvalidMessage()
        .shouldBe(visible)
        .shouldHave(text("Please enter less than 255 characters"));

    // take focus off the input to prevent blinking cursor
    SidebarNavigation.container().click();
    eyesWatcher.eyesCheck();

    CategoryEditorPage.description().val("Description");
    CategoryEditorPage.descriptionInvalidMessage().shouldNotBe(visible);

    // cause color was picked during initialization
    CategoryEditorPage.saveButton().shouldBe(enabled).shouldNotHave(cssClass("disabled"));

    CategoryEditorPage.nxColorPicker().color("kiwi").click();
    CategoryEditorPage.saveButton().shouldBe(enabled).shouldNotHave(cssClass("disabled")).click();

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
    OwnerSummaryPage.categoryTile().localCategoryLink(category.getName()).click();
    CategoryEditorPage.title().shouldHave(text("Edit"));
    CategoryEditorPage.categoryNameDiv().shouldHave(cssClass("pristine"));
    CategoryEditorPage.categoryName().shouldBe(visible).shouldHave(value("original name"));
    CategoryEditorPage.descriptionDiv().shouldHave(cssClass("pristine"));
    CategoryEditorPage.description().shouldBe(visible).shouldHave(value("original description"));
    CategoryEditorPage.nxColorPicker().shouldBe(visible).color("kiwi").shouldHave(cssClass("selected"));
    // when
    CategoryEditorPage.categoryName().val("updated name");
    CategoryEditorPage.description().val("updated description");
    CategoryEditorPage.nxColorPicker().color("red").click();
    CategoryEditorPage.saveButton().shouldBe(enabled).shouldNotHave(cssClass("disabled")).click();
    // then
    CategoryEditorPage.title().shouldHave(text("Edit"));
    CategoryEditorPage.categoryName().shouldBe(visible).shouldHave(value("updated name"));
    CategoryEditorPage.description().shouldBe(visible).shouldHave(value("updated description"));
    CategoryEditorPage.nxColorPicker().shouldBe(visible).color("red").shouldHave(cssClass("selected"));

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
    NxDeleteModal categoryEditorDeleteModal = CategoryEditorPage.getDeleteModal();
    categoryEditorDeleteModal.shouldBe(visible);
    categoryEditorDeleteModal.header().shouldHave(text("Delete Application Category"));
    // question here
    categoryEditorDeleteModal.alertContent().shouldHave(CategoryEditorPage.deleteWarningText());
    // when
    categoryEditorDeleteModal.closeButton().click();
    // then
    categoryEditorDeleteModal.shouldBe(hidden);
    CategoryEditorPage.categoryName().shouldHave(value(category.getName()));
    category = tagDAO.getById(category.getId());
    assertThat(category).isNotNull();
    // when
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    // then
    categoryEditorDeleteModal.submitButton().shouldHave(text("Continue")).shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    categoryEditorDeleteModal.shouldBe(hidden);

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
    NxDeleteModal categoryEditorDeleteModal = CategoryEditorPage.getDeleteModal();
    categoryEditorDeleteModal.shouldBe(visible);
    categoryEditorDeleteModal.header().shouldHave(text("Delete Application Category"));
    categoryEditorDeleteModal.shouldHave(CategoryEditorPage.deleteWarningText(app.getName()));
    categoryEditorDeleteModal.submitButton().shouldHave(text("Continue")).shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    categoryEditorDeleteModal.shouldBe(hidden);

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
    NxDeleteModal categoryEditorDeleteModal = CategoryEditorPage.getDeleteModal();
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    categoryEditorDeleteModal.shouldBe(visible);
    categoryEditorDeleteModal.alertContent()
        .shouldHave(CategoryEditorPage.associatedPoliciesText(policy1.getName(), policy2.getName()));
    categoryEditorDeleteModal.submitButton().shouldHave(text("Ok")).shouldBe(visible).click();
    categoryEditorDeleteModal.shouldBe(hidden);
  }

  private void assertInitialStateIsCorrect() {
    CategoryEditorPage.title().shouldHave(text("New"));
    CategoryEditorPage.categoryNameDiv().shouldBe(visible, empty).shouldHave(cssClass("pristine"));
    CategoryEditorPage.descriptionDiv().shouldBe(visible, empty).shouldHave(cssClass("pristine"));
    CategoryEditorPage.nxColorPicker().shouldBe(visible).selectedColor().shouldNot(exist);
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
