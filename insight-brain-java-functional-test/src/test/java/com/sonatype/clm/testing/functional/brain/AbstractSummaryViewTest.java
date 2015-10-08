/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList.TileSimpleListElement;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.Label;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public abstract class AbstractSummaryViewTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  @BeforeClass
  public static void boot() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    OwnerSummaryPage.SummaryTile.name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testSummaryTile() {
    OwnerSummaryPage.SummaryTile.name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.SummaryTile.icon().shouldBe(visible);
  }

  @Test
  public void testSummaryTile_missing() {
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), "fakeid"));

    ErrorBox error = OwnerSummaryPage.SummaryTile.error();
    error.root().shouldBe(visible);
    error.message().shouldHave(text("Could not find an " + currentOwner.getType().toString()));
    error.retryButton().shouldBe(visible, enabled);
  }

  @Test
  public void testActionDropDown() {
    ActionDropDown.actionButton().shouldBe(visible);
    ActionDropDown.menu().shouldNotBe(visible);
    ActionDropDown.actionButton().click();
    ActionDropDown.menu().shouldBe(visible);
  }

  @Test
  public void testEditAppOrgNameLink() {
    String shortTypeName = currentOwner.getType().toString().equalsIgnoreCase("application") ? "App" : "Org";
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldHave(text(shortTypeName));
    OwnerEditorDialog.root().shouldNotBe(visible);
    ActionDropDown.editOwner().click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text(currentOwner.getType().toString()));
  }

  @Test
  public void testLabelTile_no_labels() {
    int hierarchySize = getHierarchySize(currentOwner.getId());

    LabelTile labelTile = new LabelTile();
    labelTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(currentOwner.getName()));
    labelTile.newButton().shouldBe(visible, enabled);

    labelTile.labelLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < labelTile.labelLists().size(); i++) {
      TileSimpleList list = labelTile.labelList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible);
      }
      else {
        list.subsectionHeader().shouldNotBe(visible);
      }

      list.elements().shouldBe(empty);
    }
  }

  @Test
  public void testLabelTile_local() {
    int hierarchySize = getHierarchySize(currentOwner.getId());
    List<Label> localLabels = new ArrayList<>();
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 1", Color.black));
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 2", "With Subtitle", Color.blue));

    refreshOrOpen(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));

    LabelTile labelTile = new LabelTile();
    labelTile.labelLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < labelTile.labelLists().size(); i++) {
      TileSimpleList list = labelTile.labelList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldNot(exist);
        list.elements().shouldHaveSize(localLabels.size());

        for (int j = 0; j < list.elements().size(); j++) {
          TileSimpleListElement actualLabel = list.element(j);
          Label expectedLabel = localLabels.get(j);

          if (expectedLabel.getDescription() == null) {
            actualLabel.description().shouldNot(exist);
          }
          else {
            actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
          }

          actualLabel.icon().shouldBe(visible).shouldHave(cssClass(expectedLabel.getColor().toString()));
          actualLabel.name().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
          actualLabel.chevron().shouldBe(visible);
        }
      }
      else {
        list.subsectionHeader().shouldNot(exist);
        list.elements().shouldBe(empty);
      }
    }
  }

  @Test
  public void testLabelTile_inherited() {
    List<List<Label>> inheritedLabels = new ArrayList<>();
    List<Owner> parentOwners = new ArrayList<>();

    for (Owner owner : new OwnerDAO().walkHierarchy(currentOwner.getParentOwnerId())) {
      List<Label> labels = new ArrayList<>();
      parentOwners.add(owner);

      if (owner.getId() != null) {
        labels.add(tempEntity.newLabel(owner.getId(), owner.getId() + " Label 1", Color.black));
        labels.add(tempEntity.newLabel(owner.getId(), owner.getId() + " Label 2", "With Subtitle", Color.blue));

        inheritedLabels.add(labels);
      }
    }

    refreshOrOpen(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));

    LabelTile labelTile = new LabelTile();
    assertThat(inheritedLabels.size(), equalTo(parentOwners.size()));
    labelTile.labelLists().shouldHaveSize(parentOwners.size() + 1);

    for (int i = 0; i < labelTile.labelLists().size(); i++) {
      TileSimpleList list = labelTile.labelList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible);
        list.elements().shouldBe(empty);
      }
      else {
        list.elements().shouldHaveSize(inheritedLabels.get(i - 1).size());
        list.subsectionHeader().shouldBe(visible)
            .shouldHave(LabelTile.inheritedText(parentOwners.get(i - 1).getName()));

        for (int j = 0; j < list.elements().size(); j++) {
          TileSimpleListElement actualLabel = list.element(j);
          Label expectedLabel = inheritedLabels.get(i - 1).get(j);

          if (expectedLabel.getDescription() == null) {
            actualLabel.description().shouldNot(exist);
          }
          else {
            actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
          }

          actualLabel.icon().shouldBe(visible).shouldHave(cssClass(expectedLabel.getColor().toString()));
          actualLabel.name().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
          actualLabel.chevron().shouldNot(exist);
        }
      }
    }
  }

  private int getHierarchySize(String ownerId) {
    int hierarchySize = 0;
    Iterator<Owner> iterator = new OwnerDAO().walkHierarchy(ownerId).iterator();

    for (; iterator.hasNext(); ++hierarchySize) {
      iterator.next();
    }
    return hierarchySize;
  }

  protected abstract void testReportLinks();
}
