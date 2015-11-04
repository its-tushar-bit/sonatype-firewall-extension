/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AssociationEditor.AssociationEditorElement;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.CategoryTile.CategoryTileAppContext;
import com.sonatype.clm.testing.functional.pages.ApplicationCategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.insight.brain.model.Color.blue;
import static com.sonatype.insight.brain.model.Color.light_green;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ApplicationCategoryEditorTest
    extends AbstractFunctionalTest
{
  private static final String CATEGORY_NAME = "Test Cat";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private TagDAO tagDAO = new TagDAO();

  private Application application;

  @BeforeClass
  public static void boot() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName(), YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    refreshOrOpen(OwnerSummaryPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
  }

  @Test
  public void testEmptyCategories() {
    CategoryTile categoryTile = new CategoryTileAppContext();
    
    categoryTile.newButton().click();
    ApplicationCategoryEditorPage.title().shouldHave(text(YE_OLE_APPLICATION));
    ApplicationCategoryEditorPage.associationEditor().root().shouldBe(visible);
    ApplicationCategoryEditorPage.associationEditor().rows().shouldHaveSize(0);
    ApplicationCategoryEditorPage.updateButton().shouldNotBe(enabled);
  }

  @Test
  public void testCategorySave_SingleColumn() {
    CategoryTile categoryTile = new CategoryTileAppContext();

    Tag category1 = tempEntity.newTag(application.getOrganizationId(), CATEGORY_NAME + "_1", blue);
    Tag category2 = tempEntity.newTag(application.getOrganizationId(), CATEGORY_NAME + "_2", light_green);

    refreshOrOpen(OwnerSummaryPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    categoryTile.newButton().click();

    ApplicationCategoryEditorPage.title().shouldHave(text(YE_OLE_APPLICATION));
    ApplicationCategoryEditorPage.associationEditor().root().shouldBe(visible);
    ApplicationCategoryEditorPage.associationEditor().rows().shouldHaveSize(2);
    assertThat(ApplicationCategoryEditorPage.associationEditor().columnCount(), is(equalTo(1)));
    ApplicationCategoryEditorPage.updateButton().shouldBe(enabled);

    AssociationEditorElement category1Item = ApplicationCategoryEditorPage.associationEditor().item(0, 0);
    category1Item.checkBox().shouldBe(visible).shouldNotBe(selected);
    category1Item.description().shouldBe(visible).shouldHave(text(category1.getName()));
    category1Item.icon().shouldBe(visible).shouldHave(cssClass(category1.getColor().toValue()));

    AssociationEditorElement category2Item = ApplicationCategoryEditorPage.associationEditor().item(1, 0);
    category2Item.checkBox().shouldBe(visible).shouldNotBe(selected);
    category2Item.description().shouldBe(visible).shouldHave(text(category2.getName()));
    category2Item.icon().shouldBe(visible).shouldHave(cssClass(category2.getColor().toValue()));

    // just pick one to click
    category1Item.checkBox().click();

    ApplicationCategoryEditorPage.updateButton().click();

    refreshOrOpen(ApplicationCategoryEditorPage.urlToEdit(application.getPublicId()));

    category1Item.checkBox().shouldBe(selected);
    assertThat(getApplicableCategoryByName(application.getId(), category1.getName()), is(not(nullValue())));
    category2Item.checkBox().shouldNotBe(selected);
    assertThat(getApplicableCategoryByName(application.getId(), category2.getName()), is(nullValue()));
  }

  @Test
  public void testCategorySave_TwoColumns() {
    int expectedColumnSize = 2;
    CategoryTile categoryTile = new CategoryTileAppContext();
    List<Tag> categories = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      categories.add(
          tempEntity
              .newTag(application.getOrganizationId(), CATEGORY_NAME + "_" + i, (i % 2) == 0 ? blue : light_green));
    }

    refreshOrOpen(OwnerSummaryPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    categoryTile.newButton().click();

    // use the categories on the first row
    Tag category1 = categories.get(0);
    Tag category6 = categories.get(5);

    ApplicationCategoryEditorPage.title().shouldHave(text(YE_OLE_APPLICATION));
    ApplicationCategoryEditorPage.associationEditor().root().shouldBe(visible);
    ApplicationCategoryEditorPage.associationEditor().rows().shouldHaveSize(5);
    assertThat(ApplicationCategoryEditorPage.associationEditor().columnCount(), is(equalTo(2)));
    ApplicationCategoryEditorPage.updateButton().shouldBe(enabled);

    // row size should be half the number (5) of categories (2 columns)... check the initial state of the items
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < expectedColumnSize; j++) {
        AssociationEditorElement item = ApplicationCategoryEditorPage.associationEditor().item(i, j);
        item.checkBox().shouldBe(visible).shouldNotBe(selected);
        item.description().shouldBe(visible).shouldHave(
            text(categories.get(j == 0 ? i : i + categories.size() / 2).getName()));
        item.icon().shouldHave(cssClass(categories.get(j == 0 ? i : i + categories.size() / 2).getColor().toValue()));
      }
    }

    // select the items in the first row
    AssociationEditorElement category1Item = ApplicationCategoryEditorPage.associationEditor().item(0, 0);
    AssociationEditorElement category6Item = ApplicationCategoryEditorPage.associationEditor().item(0, 1);
    category1Item.checkBox().click();
    category6Item.checkBox().click();

    ApplicationCategoryEditorPage.updateButton().click();

    refreshOrOpen(ApplicationCategoryEditorPage.urlToEdit(application.getPublicId()));

    category1Item.checkBox().shouldBe(selected);
    assertThat(getApplicableCategoryByName(application.getId(), category1.getName()), is(not(nullValue())));
    category6Item.checkBox().shouldBe(selected);
    assertThat(getApplicableCategoryByName(application.getId(), category6.getName()), is(not(nullValue())));

    // make sure the remaining items aren't selected and haven't been applied
    for (int i = 1; i < 5; i++) {
      AssociationEditorElement firstItem = ApplicationCategoryEditorPage.associationEditor().item(i, 0);
      AssociationEditorElement secondItem = ApplicationCategoryEditorPage.associationEditor().item(i, 1);
      firstItem.checkBox().shouldNotBe(selected);
      secondItem.checkBox().shouldNotBe(selected);
      assertThat(getApplicableCategoryByName(application.getId(), categories.get(i).getName()), is(nullValue()));
      assertThat(getApplicableCategoryByName(application.getId(), categories.get(i + 5).getName()), is(nullValue()));
    }
  }

  private Tag getApplicableCategoryByName(String applicationId, String categoryName) {
    for (Tag tag : tagDAO.getByApplicationId(applicationId)) {
      if (categoryName.equals(tag.getName())) {
        return tag;
      }
    }
    return null;
  }

}
