/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.IqAssociationEditor;
import com.sonatype.clm.testing.functional.pages.ApplicationCategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.NxColor;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.AssociationEditor.MULTI_COLUMN;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Color.dark_blue;
import static com.sonatype.insight.brain.model.Color.light_green;

public class ApplicationCategoryEditorTest
    extends AbstractFunctionalTest
{
  private static final String CATEGORY_NAME = "Test Cat";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private Application application;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    // note the ȧ being used to force a character to be encoded
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    refreshOrOpen(OwnerSummaryPage.url(application));
  }

  @Test
  public void testNoCategories() {
    refreshOrOpen(ApplicationCategoryEditorPage.urlToEdit(application));

    ErrorBox errorBox = ApplicationCategoryEditorPage.errorBox();
    errorBox.shouldBe(visible);
    errorBox.shouldHave(ApplicationCategoryEditorPage.NO_CATEGORIES_DEFINED);
  }

  @Test
  public void testCategorySave_SingleColumn() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();

    Tag category1 = tempEntity.newTag(application.getOrganizationId(), CATEGORY_NAME + "_1", dark_blue);
    Tag category2 = tempEntity.newTag(application.getOrganizationId(), CATEGORY_NAME + "_2", light_green);

    refreshOrOpen(OwnerSummaryPage.url(application));

    categoryTile.newButton().click();

    eyesWatcher.eyesCheck();

    ApplicationCategoryEditorPage.title().shouldHave(ApplicationCategoryEditorPage.titleText());
    ApplicationCategoryEditorPage.subtitle()
        .text()
        .equals(ApplicationCategoryEditorPage.subtitleText(YE_OLE_APPLICATION));
    ApplicationCategoryEditorPage.associationEditor().shouldBe(visible);
    ApplicationCategoryEditorPage.associationEditor().rows().shouldHave(size(2));
    ApplicationCategoryEditorPage.associationEditor().shouldNotBe(MULTI_COLUMN);

    IqAssociationEditor.AssociationEditorElement category1Item =
        ApplicationCategoryEditorPage.associationEditor().item(0);
    category1Item.checkBox().shouldBe(visible).shouldNotBe(selected);
    category1Item.description().shouldBe(visible).shouldHave(text(category1.getName()));
    String nxColorClass = "nx-selectable-color--" +
        NxColor.getNxColorFromColor(category1.getColor()).toString();
    category1Item.icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));

    IqAssociationEditor.AssociationEditorElement category2Item =
        ApplicationCategoryEditorPage.associationEditor().item(1);
    category2Item.checkBox().shouldBe(visible).shouldNotBe(selected);
    category2Item.description().shouldBe(visible).shouldHave(text(category2.getName()));
    nxColorClass = "nx-selectable-color--" +
        NxColor.getNxColorFromColor(category2.getColor()).toString();
    category2Item.icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));

    // just pick one to click
    category1Item.checkBox().click();

    ApplicationCategoryEditorPage.updateButton().shouldBe(enabled).shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    // Refresh page to ensure values are propagated to server
    refreshOrOpen(ApplicationCategoryEditorPage.urlToEdit(application));
    category1Item = ApplicationCategoryEditorPage.associationEditor().item(0);
    category2Item = ApplicationCategoryEditorPage.associationEditor().item(1);

    category1Item.checkBox().shouldBe(selected);
    category1Item.description().shouldBe(visible).shouldHave(text(category1.getName()));
    category2Item.checkBox().shouldNotBe(selected);
    category2Item.description().shouldBe(visible).shouldHave(text(category2.getName()));
  }

  @Test
  public void testCategorySave_TwoColumns() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    List<Tag> categories = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      categories.add(tempEntity.newTag(application.getOrganizationId(), CATEGORY_NAME + "_" + i,
          (i % 2) == 0 ? dark_blue : light_green));
    }

    refreshOrOpen(OwnerSummaryPage.url(application));

    categoryTile.newButton().click();

    eyesWatcher.eyesCheck();

    // use the categories on the first row
    Tag category1 = categories.get(0);
    Tag category6 = categories.get(5);

    ApplicationCategoryEditorPage.title().shouldHave(ApplicationCategoryEditorPage.titleText());
    ApplicationCategoryEditorPage.subtitle()
        .text()
        .equals(ApplicationCategoryEditorPage.subtitleText(YE_OLE_APPLICATION));
    ApplicationCategoryEditorPage.associationEditor().shouldBe(visible);
    ApplicationCategoryEditorPage.associationEditor().rows().shouldHave(size(10));
    ApplicationCategoryEditorPage.associationEditor().shouldBe(MULTI_COLUMN);

    for (int i = 0; i < 10; i++) {
      IqAssociationEditor.AssociationEditorElement item = ApplicationCategoryEditorPage.associationEditor().item(i);
      item.checkBox().shouldBe(visible).shouldNotBe(selected);
      item.description().shouldBe(visible).shouldHave(text(categories.get(i).getName()));
      String nxColorClass = "nx-selectable-color--" +
          NxColor.getNxColorFromColor(categories.get(i).getColor()).toString();
      item.icon().shouldHave(cssClass(nxColorClass));
    }

    // select the items in the first row
    IqAssociationEditor.AssociationEditorElement category1Item =
        ApplicationCategoryEditorPage.associationEditor().item(0);
    IqAssociationEditor.AssociationEditorElement category6Item =
        ApplicationCategoryEditorPage.associationEditor().item(5);
    category1Item.checkBox().shouldBe(visible).click();
    category6Item.checkBox().shouldBe(visible).click();

    ApplicationCategoryEditorPage.updateButton().shouldBe(enabled).shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    // Refresh page to ensure values are propagated to server
    refreshOrOpen(ApplicationCategoryEditorPage.urlToEdit(application));
    category1Item = ApplicationCategoryEditorPage.associationEditor().item(0);
    category6Item = ApplicationCategoryEditorPage.associationEditor().item(5);

    category1Item.checkBox().shouldBe(selected);
    category1Item.description().shouldBe(visible).shouldHave(text(category1.getName()));
    category6Item.checkBox().shouldBe(selected);
    category6Item.description().shouldBe(visible).shouldHave(text(category6.getName()));

    // make sure the remaining items aren't selected and haven't been applied
    for (int i = 1; i < 5; i++) {
      IqAssociationEditor.AssociationEditorElement firstItem =
          ApplicationCategoryEditorPage.associationEditor().item(i);
      IqAssociationEditor.AssociationEditorElement secondItem =
          ApplicationCategoryEditorPage.associationEditor().item(i + 5);
      firstItem.checkBox().shouldNotBe(selected);
      secondItem.checkBox().shouldNotBe(selected);
    }
  }
}
