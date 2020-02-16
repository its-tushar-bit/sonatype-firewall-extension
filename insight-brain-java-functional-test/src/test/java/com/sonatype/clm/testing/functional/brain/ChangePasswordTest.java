/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ChangePasswordModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ChangePasswordTest
    extends AbstractFunctionalTest
{
  @Test
  public void testChangePassword() {
    User user = tempEntity.newUser("testchangepass", "John", "Doe", "john@doe.com");
    refreshOrOpen(ReportListPage.url());
    login(user.getUsername(), user.getPassword());

    MainHeader.userMenu().dropdownToggle().shouldBe(visible).click();
    MainHeader.userMenu().changePassword().should(appear).click();

    ChangePasswordModal modal = new ChangePasswordModal();
    modal.should(appear);
    modal.ok().shouldBe(disabled);

    modal.oldPassword().setValue("unsecret");
    modal.ok().shouldBe(disabled);

    modal.newPassword().setValue("newsecret");
    modal.ok().shouldBe(disabled);

    modal.newPasswordValidate().setValue("newsecretdoesntmatch");
    popoverViolations(modal.newPasswordValidate()).should(appear).shouldHave(text("Passwords must match!"));
    modal.ok().shouldBe(disabled);
    eyesWatcher.eyesCheck();

    modal.newPasswordValidate().setValue("newsecret");
    popoverViolations(modal.newPasswordValidate()).shouldNot(exist);
    modal.ok().shouldBe(enabled).click();

    modal.invalidCredentialsError().should(appear).shouldNotBe(empty);

    modal.oldPassword().setValue(user.getPassword());
    modal.ok().shouldBe(enabled).click();

    modal.should(disappear);

    logout();
    login(user.getUsername(), "newsecret");
    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldHave(text("John Doe"));
  }
}
