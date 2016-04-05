/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.CategoryTile.CategoryTileOrgContext;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList.TileSimpleListElement;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class OrganizationSummaryViewTest
    extends AbstractSummaryViewTest
{

  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init(organization);
  }

  @Override
  @Test
  public void testReportLinks() {
    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldBe(empty);
  }

  @Test
  public void testLTGTile_NoLocal() {
    int hierarchySize = getHierarchySize(organization.getId());

    LicenseThreatGroupTile ltgTile = new LicenseThreatGroupTile();
    ltgTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(organization.getName()));
    ltgTile.newButton().shouldBe(visible, enabled);

    ltgTile.ltgLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      ThreatGroupTileSimpleList list = ltgTile.ltgList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible).shouldHave(text("No local threat groups defined"));
        list.elements().shouldBe(empty);
      }
      else {
        list.ownerName().shouldBe(visible);
        list.emptyDescriptor().shouldNotBe(visible);
        list.elements().shouldHaveSize(LicenseThreatGroupDAO.DEFAULT_LICENSE_THREAT_GROUP_COUNT);
      }
    }
  }

  @Override
  @Test
  public void testApplicationCategoryTile() {
    testApplicationCategoryTile_Empty();
    testApplicationCategoryTile_WithApplicableCategories();
  }

  @Test
  public void testMoveApplicationLink() {
    ActionDropDown.actionButton().click();
    ActionDropDown.moveApplication().shouldNotBe(visible);
  }

  private void testApplicationCategoryTile_Empty() {
    final int hierarchySize = getHierarchySize(organization.getId());
    CategoryTile categoryTile = new CategoryTileOrgContext();
    categoryTile.subHeader().shouldBe(visible).shouldHave(categoryTile.subHeaderText(organization.getName()));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(categoryTile.buttonText());

    categoryTile.categoryLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      TileSimpleList list = categoryTile.categoryList(i);
      list.elements().shouldBe(empty);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible).shouldHave(categoryTile.emptyListDescriptorText());
      }
      else {
        list.subsectionHeader().shouldNot(exist);
        list.emptyDescriptor().shouldNot(exist);
      }
    }
  }

  private void testApplicationCategoryTile_WithApplicableCategories() {
    List<List<Tag>> ownerTags = new ArrayList<>();
    List<Owner> owners = new ArrayList<>();

    for (Owner owner : new OwnerDAO().walkHierarchy(organization.getId())) {
      List<Tag> tags = new ArrayList<>();
      owners.add(owner);

      if (owner.getId() != null) {
        tags.add(tempEntity.newTag(owner.getId(), owner.getName() + " Test Tag 1", Color.dark_purple));
        tags.add(tempEntity.newTag(owner.getId(), owner.getName() + " Test Tag 2", Color.dark_blue));

        ownerTags.add(tags);
      }
    }

    refresh();

    final int hierarchySize = owners.size();
    CategoryTile categoryTile = new CategoryTileOrgContext();
    assertThat(ownerTags.size(), equalTo(owners.size()));
    categoryTile.categoryLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      TileSimpleList list = categoryTile.categoryList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
      }
      else {
        list.subsectionHeader().shouldBe(visible).shouldHave(CategoryTile.inheritedText(owners.get(i).getName()));
      }

      list.elements().shouldHaveSize(ownerTags.get(i).size());

      for (int j = 0; j < ownerTags.get(i).size(); j++) {
        TileSimpleListElement actualCategory = list.element(j);
        Tag expectedCategory = ownerTags.get(i).get(j);

        if (i == 0) {
          actualCategory.root.shouldBe(TileSimpleListElement.CLICKABLE);
          actualCategory.chevron().shouldBe(visible);
        }
        else {
          actualCategory.root.shouldNotBe(TileSimpleListElement.CLICKABLE);
          actualCategory.chevron().shouldNot(exist);
        }

        if (expectedCategory.getDescription() == null) {
          actualCategory.description().shouldNot(exist);
        }
        else {
          actualCategory.description().shouldBe(visible).shouldHave(text(expectedCategory.getDescription()));
        }

        actualCategory.icon().shouldBe(visible).shouldHave(cssClass(expectedCategory.getColor().toValue()));
        actualCategory.name().shouldBe(visible).shouldHave(text(expectedCategory.getName()));
      }
    }
  }
}
