/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.ProprietaryConfigInheritedList;
import com.sonatype.clm.testing.functional.elements.ProprietaryConfigInheritedTile;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;

import com.codeborne.selenide.ElementsCollection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.ProprietaryComponentMatcher.MatcherType.PACKAGE;
import static com.sonatype.clm.testing.functional.elements.ProprietaryComponentMatcher.MatcherType.REGEX;
import static com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage.BEGINNING_OR_ENDING_PERIOD_MESSAGE;
import static com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage.DUPLICATE_VALUE_MESSAGE;
import static com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage.INVALID_PACKAGE_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractProprietaryConfigEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private ProprietaryConfigDAO proprietaryConfigDAO;

  private OwnerDAO ownerDAO;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    proprietaryConfigDAO = lookup(ProprietaryConfigDAO.class);
    ownerDAO = lookup(OwnerDAO.class);
  }

  protected void init(Owner currentOwner) {
    tempEntity.newProprietaryConfig(currentOwner.getParentOwnerId(), Collections.singletonList("com.inherited"),
        Collections.singletonList(".*test\\.zip"));
    tempEntity.newProprietaryConfig(currentOwner.getId(), Collections.singletonList("com.local"),
        Collections.emptyList());

    this.currentOwner = currentOwner;

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testProprietaryComponentMatchers() {
    OwnerSummaryPage.proprietaryComponentMatchers().shouldHave(text("1 local, 2 inherited"));

    OwnerSummaryPage.proprietaryComponentMatchers().click();
    waitUntilUrl(ProprietaryConfigEditorPage.url(currentOwner));

    NxFormSelect typeDropDown = ProprietaryConfigEditorPage.typeDropdown();
    typeDropDown.listItem(PACKAGE.ordinal()).shouldHave(text(PACKAGE.name));
    typeDropDown.listItems().shouldHave(texts("Package", "Regular Expression"));
    typeDropDown.chooseOption(new Option(PACKAGE.ordinal(), PACKAGE.name));

    ProprietaryConfigEditorPage.matcherValue().shouldBe(empty);
    ProprietaryConfigEditorPage.addButton().shouldHave(attribute("disabled"));

    // take focus off of the select to prevent the select options displayed
    SidebarNavigation.container().click();

    assertInheritanceSection();

    // make sure toggling dropdown clears input
    ProprietaryConfigEditorPage.matcherValue().val("com.package");
    typeDropDown.listItem(REGEX.ordinal()).shouldHave(text(REGEX.name));
    typeDropDown.chooseOption(new Option(REGEX.ordinal(), REGEX.name));
    ProprietaryConfigEditorPage.matcherValue().val("com.regex");

    typeDropDown.chooseOption(new Option(PACKAGE.ordinal(), PACKAGE.name));
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldNotBe(visible);

    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("com.local"));
    ProprietaryConfigEditorPage.matcherValue().val("com.local");
    ProprietaryConfigEditorPage.addButton().shouldHave(attribute("disabled"));
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldHave(text(DUPLICATE_VALUE_MESSAGE));

    // check for beginning and ending periods
    ProprietaryConfigEditorPage.matcherValue().val(".foo");
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldBe(visible);
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldHave(text(BEGINNING_OR_ENDING_PERIOD_MESSAGE));

    ProprietaryConfigEditorPage.matcherValue().val("foo.");
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldBe(visible);
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldHave(text(BEGINNING_OR_ENDING_PERIOD_MESSAGE));

    // check warning clears
    ProprietaryConfigEditorPage.matcherValue().val("foo");
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldNotBe(visible);
    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("com.local"));

    // check the various validations
    ProprietaryConfigEditorPage.matcherValue().val("/");
    ProprietaryConfigEditorPage.addButton().shouldHave(attribute("disabled"));
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldBe(visible).shouldHave(text(INVALID_PACKAGE_MESSAGE));

    typeDropDown.listItem(REGEX.ordinal()).shouldHave(text(REGEX.name));
    typeDropDown.chooseOption(new Option(REGEX.ordinal(), REGEX.name));

    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldNotBe(visible);

    ProprietaryConfigEditorPage.matcherValue().val("com.sonatype.*");
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldNotBe(visible);
    ProprietaryConfigEditorPage.addButton().shouldNotHave(DISABLED).click();
    ProprietaryConfigEditorPage.addButton().shouldHave(attribute("disabled"));

    ElementsCollection localMatchers = ProprietaryConfigEditorPage.localMatchers();
    localMatchers.get(0).shouldHave(text("com.local"));
    localMatchers.get(0).shouldHave(text("Package"));
    localMatchers.get(1).shouldHave(text("com.sonatype.*"));
    localMatchers.get(1).shouldHave(text("RegEx"));

    ProprietaryConfigEditorPage.updateButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    ProprietaryConfig config = proprietaryConfigDAO.getByOwnerId(currentOwner.getId());
    assertThat(config.getPackages()).containsExactly("com.local");
    assertThat(config.getRegexes()).containsExactly("com.sonatype.*");

    // check validation for regex
    typeDropDown.chooseOption(new Option(REGEX.ordinal(), REGEX.name));
    ProprietaryConfigEditorPage.matcherValue().val("com.sonatype.*");
    ProprietaryConfigEditorPage.addButton().shouldHave(attribute("disabled"));
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldHave(text(DUPLICATE_VALUE_MESSAGE));

    // check warning clears
    ProprietaryConfigEditorPage.matcherValue().val("");
    ProprietaryConfigEditorPage.matcherInvalidMessage().shouldBe(visible);

    // now remove them
    ProprietaryConfigEditorPage.localMatcher(PACKAGE, "com.local").deleteButton().click();
    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("com.sonatype.* Regex"));

    ProprietaryConfigEditorPage.localMatcher(REGEX, "com.sonatype.*").deleteButton().click();
    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("No matchers configured"));

    ProprietaryConfigEditorPage.addButton().shouldHave(attribute("disabled"));
    ProprietaryConfigEditorPage.updateButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    config = proprietaryConfigDAO.getByOwnerId(currentOwner.getId());
    assertThat(config.getPackages()).isEmpty();
    assertThat(config.getRegexes()).isEmpty();
  }

  private void assertInheritanceSection() {
    List<Owner> parentOwners = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(currentOwner.getParentOwnerId())) {
      parentOwners.add(owner);
    }

    ProprietaryConfigInheritedTile configTile = new ProprietaryConfigInheritedTile();
    configTile.proprietaryConfigInheritedLists().shouldHave(size(1));

    ProprietaryConfigInheritedList list = configTile.proprietaryConfigInheritedList(0);

    list.ownerName()
        .shouldBe(visible)
        .shouldHave(ProprietaryConfigInheritedTile.inheritedText(parentOwners.get(0).getName()));
    list.inheritedMatchers().get(0).shouldHave(text(".*test\\.zip"));
    list.inheritedMatchers().get(0).shouldHave(text("RegEx"));
    list.inheritedMatchers().get(1).shouldHave(text("com.inherited"));
    list.inheritedMatchers().get(1).shouldHave(text("Package"));

    list.inheritedMatcher(REGEX, ".*test\\.zip").deleteButton().shouldNot(exist);
    list.inheritedMatcher(PACKAGE, "com.inherited").deleteButton().shouldNot(exist);
  }
}
