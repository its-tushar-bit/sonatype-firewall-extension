/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.UserDetailsModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class UserDetailsModalTest
    extends AbstractFunctionalTest
{
  @Test
  public void testUserDetails() {
    User user = tempEntity.newUser("jonny", "John", "Smith", "john@smith.com");
    refreshOrOpen(DashboardPage.URL);
    login(user.getUsername(), user.getPassword());
    MainHeader.userMenu().dropdownToggle().shouldBe(visible).click();

    MainHeader.userMenu().userDetails().should(appear).click();

    UserDetailsModal modal = new UserDetailsModal();
    modal.should(appear);
    modal.username().shouldBe(text(user.getUsername()));
    modal.displayName().shouldBe(text(user.calculateDisplayName()));
    modal.groups().shouldBe(text(Group.AUTHENTICATED_USERS_GROUP_ID));
    eyesWatcher.eyesCheck();
    modal.closeButton().shouldBe(enabled).click();
    modal.should(disappear);
  }
}
