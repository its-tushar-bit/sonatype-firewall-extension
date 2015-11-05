/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView.OwnerDetailTreeViewGroup;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView.OwnerDetailTreeViewGroup.OwnerDetailTreeViewItem;
import com.sonatype.clm.testing.functional.pages.CategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.LabelEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerDetailsEditingPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.tag.Tag;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.back;
import static com.codeborne.selenide.Selenide.open;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

public abstract class AbstractOwnerDetailsEditingTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  private Label label;

  private Tag category;

  @BeforeClass
  public static void boot() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  public void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    label = tempEntity.newLabel(currentOwner.getId());

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      category = tempEntity.newTag(currentOwner.getId());
    }

    open(OwnerDetailsEditingPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
  }

  @Test
  public void testOwnerTreeViewDetails() {
    OwnerDetailTreeView.header().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    assertThat(OwnerDetailTreeView.headerHref(), containsString(OwnerSummaryPage.url(currentOwner.getType().toString(),
        currentOwner.getPublicId())));

    testRouting_ApplicationCategories(OwnerDetailTreeView.applicationCategoryGroup());
    testRouting_Policies(OwnerDetailTreeView.policyGroup());
    testRouting_ComponentLabels(OwnerDetailTreeView.componentLabelGroup());
    testRouting_LicenseThreatGroups(OwnerDetailTreeView.LTGGroup());
    testRouting_Access(OwnerDetailTreeView.accessGroup());
  }

  private void testRouting_ApplicationCategories(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_COLLAPSE_CLASS);

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      detailGroup.items().shouldHaveSize(3);
      detailGroup.item(1).root().shouldBe(visible).click();
      detailGroup.item(1).root().shouldHave(OwnerDetailTreeViewItem.SELECTED_CLASS);
      assertThat(WebDriverRunner.url(),
          endsWith(CategoryEditorPage.urlToCreate(currentOwner.getPublicId())));

      back();

      detailGroup.item(2).root().shouldBe(visible).shouldHave(text(category.getName())).click();
      detailGroup.item(2).root().shouldHave(OwnerDetailTreeViewItem.SELECTED_CLASS);
      detailGroup.item(2).icon().shouldBe(visible).shouldHave(cssClass(category.getColor().toString()));
      assertThat(WebDriverRunner.url(),
          endsWith(CategoryEditorPage.urlToEdit(currentOwner.getPublicId(), category.getId())));

      back();
    }

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
  }

  private void testRouting_Policies(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_COLLAPSE_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
  }

  private void testRouting_ComponentLabels(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_COLLAPSE_CLASS);

    detailGroup.items().shouldHaveSize(3);
    detailGroup.item(1).root().shouldBe(visible).click();
    detailGroup.item(1).root().shouldHave(OwnerDetailTreeViewItem.SELECTED_CLASS);
    assertThat(WebDriverRunner.url(),
        endsWith(LabelEditorPage.urlToCreate(currentOwner.getType().toString(), currentOwner.getPublicId())));

    back();

    detailGroup.item(2).root().shouldBe(visible).shouldHave(text(label.getLabel())).click();
    detailGroup.item(2).root().shouldHave(OwnerDetailTreeViewItem.SELECTED_CLASS);
    detailGroup.item(2).icon().shouldBe(visible).shouldHave(cssClass(label.getColor().toString()));
    assertThat(WebDriverRunner.url(), endsWith(LabelEditorPage.urlToEdit(currentOwner.getType().toString(),
        currentOwner.getPublicId(), label.getId())));

    back();

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
  }

  private void testRouting_LicenseThreatGroups(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_COLLAPSE_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
  }

  private void testRouting_Access(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_COLLAPSE_CLASS);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldHave(OwnerDetailTreeViewGroup.TWISTY_EXPAND_CLASS);
  }
}
