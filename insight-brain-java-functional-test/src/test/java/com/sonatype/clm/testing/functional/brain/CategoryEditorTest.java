/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.pages.CategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

import com.codeborne.selenide.WebDriverRunner;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Color.dark_red;
import static com.sonatype.insight.brain.model.Color.light_green;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class CategoryEditorTest
    extends AbstractFunctionalTest
{
  private static final String CATEGORY_NAME = "a category";

  private TagDAO tagDAO = new TagDAO();

  private Organization org;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization();
    refreshOrOpen(OwnerSummaryPage.url("organization", org.getId()));
  }

  @Test
  public void testCreateCategory() {
    SummaryTile.addCategoryButton().click();
    assertInitialStateIsCorrect();
    CategoryEditorPage.categoryName().val("$$$"); // invalid characters
    PopoverViolations.on(CategoryEditorPage.categoryName()).shouldShowInvalidCharactersError();
    CategoryEditorPage.saveButton().shouldBe(disabled);

    CategoryEditorPage.categoryName().val(CATEGORY_NAME); // description and color are mandatory as well
    // TODO check color 'field' validation error after CLM-5436
    PopoverViolations.on(CategoryEditorPage.categoryName()).shouldNotExist();
    CategoryEditorPage.saveButton().shouldBe(disabled);

    CategoryEditorPage.description().val(StringUtils.repeat("a", 256)); // too long
    PopoverViolations.on(CategoryEditorPage.description()).shouldShowMaxLengthError();
    CategoryEditorPage.saveButton().shouldBe(disabled);

    CategoryEditorPage.description().val("Description");
    PopoverViolations.on(CategoryEditorPage.description()).shouldNotExist();
    CategoryEditorPage.saveButton().shouldBe(disabled); // color still missing

    CategoryEditorPage.colorPicker().color(light_green).click();
    CategoryEditorPage.saveButton().shouldBe(enabled);

    CategoryEditorPage.saveButton().click();
    assertInitialStateIsCorrect(); // form reset

    Tag category = getCategoryByName(org.getId(), CATEGORY_NAME);
    assertNotNull(category);
    assertEquals(org.getId(), category.getOrganizationId());
    assertEquals(CATEGORY_NAME, category.getName());
    assertEquals("Description", category.getDescription());
    assertEquals(light_green, category.getColor());
  }

  @Test
  public void testEditCategory() {
    // given
    Tag category = tempEntity.newTag(org.getId(), "original name", "original description", light_green);
    refreshOrOpen(OwnerSummaryPage.url("organization", org.getId()));
    SummaryTile.localCategory(category.getName()).click();
    CategoryEditorPage.title().shouldHave(text("Edit"));
    CategoryEditorPage.categoryName().shouldBe(visible).shouldHave(cssClass("initial-value")).shouldHave(value("original name"));
    CategoryEditorPage.description().shouldBe(visible).shouldHave(cssClass("initial-value")).shouldHave(value("original description"));
    CategoryEditorPage.colorPicker().root().shouldBe(visible);
    CategoryEditorPage.colorPicker().color(light_green).shouldHave(cssClass("selected"));
    CategoryEditorPage.saveButton().shouldBe(disabled);
    // when
    CategoryEditorPage.categoryName().val("updated name");
    CategoryEditorPage.description().val("updated description");
    CategoryEditorPage.colorPicker().color(dark_red).click();
    CategoryEditorPage.saveButton().shouldBe(enabled).click();
    // then
    CategoryEditorPage.title().shouldHave(text("Edit"));
    CategoryEditorPage.categoryName().shouldBe(visible).shouldHave(value("updated name"));
    CategoryEditorPage.description().shouldBe(visible).shouldHave(value("updated description"));
    CategoryEditorPage.colorPicker().root().shouldBe(visible);
    CategoryEditorPage.colorPicker().color(dark_red).shouldHave(cssClass("selected"));
    CategoryEditorPage.saveButton().shouldBe(disabled);

    category = getCategoryByName(org.getId(), "updated name");
    assertNotNull(category);
    assertEquals(org.getId(), category.getOrganizationId());
    assertEquals("updated name", category.getName());
    assertEquals("updated description", category.getDescription());
    assertEquals(dark_red, category.getColor());
  }

  @Test
  public void testDeleteCategory() {
    // given
    Tag category = tempEntity.newTag(org.getId());
    refreshOrOpen(CategoryEditorPage.urlToEdit(org.getId(), category.getId()));
    // when
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    // then
    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Category"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(category.getName()));
    // when
    DeleteModal.cancelButton().click();
    // then
    DeleteModal.root().shouldNotBe(visible);
    CategoryEditorPage.categoryName().shouldHave(value(category.getName()));
    category = tagDAO.getById(category.getId());
    assertThat(category, is(not(nullValue())));
    // when
    CategoryEditorPage.deleteButton().shouldBe(visible).click();
    // then
    DeleteModal.deleteButton().shouldBe(visible).click();
    // then the modal should be hidden 800 ms after delete REST call is successful
    DeleteModal.root().shouldNotBe(visible);

    assertThat(currentUrl(), containsString(CategoryEditorPage.urlToCreate(org.getId())));

    category = tagDAO.getById(category.getId());
    assertThat(category, is(nullValue()));
  }

  private String currentUrl() {
    return WebDriverRunner.getWebDriver().getCurrentUrl();
  }

  private void assertInitialStateIsCorrect() {
    CategoryEditorPage.title().shouldHave(text("New"));
    CategoryEditorPage.categoryName().shouldBe(visible, empty).shouldHave(cssClass("initial-value"));
    CategoryEditorPage.description().shouldBe(visible, empty).shouldHave(cssClass("initial-value"));
    CategoryEditorPage.colorPicker().root().shouldBe(visible);
    CategoryEditorPage.colorPicker().selectedColor().shouldNot(exist);
    CategoryEditorPage.saveButton().shouldBe(disabled);
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
