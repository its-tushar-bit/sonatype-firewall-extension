/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.ProprietaryConfigInheritedList;
import com.sonatype.clm.testing.functional.elements.ProprietaryConfigInheritedTile;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.ProprietaryComponentMatcher.MatcherType.PACKAGE;
import static com.sonatype.clm.testing.functional.elements.ProprietaryComponentMatcher.MatcherType.REGEX;
import static com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage.DUPLICATE_PACKAGE_MESSAGE;
import static com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage.DUPLICATE_REGEX_MATCHER_MESSAGE;
import static com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage.INVALID_PACKAGE_MESSAGE;
import static com.sonatype.clm.testing.functional.pages.ProprietaryConfigEditorPage.WILDCARD_PACKAGE_MESSAGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractProprietaryConfigEditorTest extends AbstractFunctionalTest
{
  private Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private ProprietaryConfigDAO proprietaryConfigDAO = new ProprietaryConfigDAO();

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    tempEntity.newProprietaryConfig(currentOwner.getParentOwnerId(), asList("com.inherited"), asList(".*test\\.zip"));
    tempEntity.newProprietaryConfig(currentOwner.getId(), asList("com.local"), asList());

    this.currentOwner = currentOwner;

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testProprietaryComponentMatchers() {
    OwnerSummaryPage.proprietaryComponentMatchers().shouldHave(text("1 local, 2 inherited"));

    OwnerSummaryPage.proprietaryComponentMatchers().click();
    waitUntilUrl(ProprietaryConfigEditorPage.url(currentOwner));

    Dropdown typeDropDown = ProprietaryConfigEditorPage.typeDropdown();
    typeDropDown.selectedItem().shouldHave(text(PACKAGE.name)).click();
    typeDropDown.listItems().shouldHave(texts("Package", "Regular Expression"));
    typeDropDown.selectedItem().click();

    ProprietaryConfigEditorPage.matcherValue().shouldBe(empty);
    ProprietaryConfigEditorPage.addButtton().shouldHave(DISABLED);
    ProprietaryConfigEditorPage.updateButton().shouldHave(DISABLED);
    eyesWatcher.eyesCheck();

    assertInheritanceSection();

    // make sure toggling dropdown clears input
    ProprietaryConfigEditorPage.matcherValue().val("com.package");
    typeDropDown.selectedItem().click();
    typeDropDown.listItem(REGEX.dropdownIndex).shouldHave(text(REGEX.name)).click();
    ProprietaryConfigEditorPage.matcherValue().val("com.regex");
    typeDropDown.selectedItem().click();
    typeDropDown.listItem(PACKAGE.dropdownIndex).shouldHave(text(PACKAGE.name)).click();
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldNotExist();

    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("com.local"));
    ProprietaryConfigEditorPage.matcherValue().val("com.local");
    ProprietaryConfigEditorPage.addButtton().shouldHave(DISABLED);
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldShowError(DUPLICATE_PACKAGE_MESSAGE);

    // check warning clears
    ProprietaryConfigEditorPage.matcherValue().val("foo");
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldNotExist();
    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("com.local"));

    // check the various validations
    ProprietaryConfigEditorPage.matcherValue().val("!");
    ProprietaryConfigEditorPage.addButtton().shouldHave(DISABLED);
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue())
        .shouldShowError(INVALID_PACKAGE_MESSAGE);

    ProprietaryConfigEditorPage.matcherValue().val("*.sonatype");
    ProprietaryConfigEditorPage.addButtton().shouldHave(DISABLED);
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldShowError(WILDCARD_PACKAGE_MESSAGE);

    typeDropDown.selectedItem().click();
    typeDropDown.listItem(REGEX.dropdownIndex).shouldHave(text(REGEX.name)).click();

    // make sure the value is cleared
    ProprietaryConfigEditorPage.matcherValue().shouldBe(empty);
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldNotExist();

    ProprietaryConfigEditorPage.matcherValue().val("com.sonatype.*");
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldNotExist();
    ProprietaryConfigEditorPage.addButtton().shouldNotHave(DISABLED).click();
    ProprietaryConfigEditorPage.addButtton().shouldHave(DISABLED);
    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("com.local", "com.sonatype.* (regex)"));

    ProprietaryConfigEditorPage.updateButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    
    ProprietaryConfig config = proprietaryConfigDAO.getByOwnerId(currentOwner.getId());
    assertThat(config.getPackages()).containsExactly("com.local");
    assertThat(config.getRegexes()).containsExactly("com.sonatype.*");

    ProprietaryConfigEditorPage.updateButton().shouldHave(DISABLED);

    // check validation for regex
    ProprietaryConfigEditorPage.matcherValue().val("com.sonatype.*");
    ProprietaryConfigEditorPage.addButtton().shouldHave(DISABLED);
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldShowError(DUPLICATE_REGEX_MATCHER_MESSAGE);

    // check warning clears
    ProprietaryConfigEditorPage.matcherValue().val("");
    PopoverViolations.on(ProprietaryConfigEditorPage.matcherValue()).shouldNotExist();
    
    // now remove them
    ProprietaryConfigEditorPage.localMatcher(PACKAGE, "com.local").deleteButton().click();
    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("com.sonatype.* (regex)"));

    ProprietaryConfigEditorPage.localMatcher(REGEX, "com.sonatype.*").deleteButton().click();
    ProprietaryConfigEditorPage.localMatchers().shouldHave(texts("No local matchers configured"));

    ProprietaryConfigEditorPage.addButtton().shouldHave(DISABLED);
    ProprietaryConfigEditorPage.updateButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    config = proprietaryConfigDAO.getByOwnerId(currentOwner.getId());
    assertThat(config.getPackages()).isEmpty();
    assertThat(config.getRegexes()).isEmpty();
  }

  private void assertInheritanceSection() {
    List<Owner> parentOwners = new ArrayList<>();
    for (Owner owner : new OwnerDAO().walkHierarchy(currentOwner.getParentOwnerId())) {
      parentOwners.add(owner);
    }

    ProprietaryConfigInheritedTile configTile = new ProprietaryConfigInheritedTile();
    configTile.proprietaryConfigInheritedLists().shouldHaveSize(1);

    ProprietaryConfigInheritedList list = configTile.proprietaryConfigInheritedList(0);

    list.ownerName().shouldBe(visible)
        .shouldHave(ProprietaryConfigInheritedTile.inheritedText(parentOwners.get(0).getName()));
    list.inheritedMatchers().shouldHave(texts("com.inherited", ".*test\\.zip (regex)"));
    list.inheritedMatcher(REGEX, ".*test\\.zip (regex)").deleteButton().shouldNot(exist);
    list.inheritedMatcher(PACKAGE, "com.inherited").deleteButton().shouldNot(exist);
  }
}
