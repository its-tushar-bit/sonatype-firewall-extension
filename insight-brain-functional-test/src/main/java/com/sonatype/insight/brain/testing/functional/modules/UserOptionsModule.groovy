/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import com.sonatype.insight.brain.testing.functional.ReportViolationsPage

import geb.Module
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement

/**
 * @since 1.8
 */
class UserOptionsModule
    extends Module
{
  static base = { $('#user-menu') }

  static content = {
    displayName(wait: true) { $('#user-name') }
    optionsDropdown(wait: true) { $('#user-menu-dropdown-toggle') }
    logout(wait: true, to: ReportViolationsPage) { $('#logout') }
    openChangePassword(wait: true) { $('#change-password') }
  }

  private void dropdownClick() {
    optionsDropdown.click()
  }

  void logoutClick() {
    dropdownClick()
    def body = browser.page.$('body').firstElement();
    waitFor { logout.displayed }
    logout.click()
    /*
     * NOTE: Logout triggers navigation to a new page. Any page content that is accessed directly after the click event
     * is at danger of becoming stale during use once the browser starts loading the new page. To avoid this trouble,
     * we wait until the browser has started loading the new page as witnessed by a previous element becoming stale.
     */
    waitFor { isStale(body) }
  }

  void changePasswordClick() {
    dropdownClick()
    waitFor { openChangePassword.displayed }
    openChangePassword.click()
  }

  private boolean isStale(WebElement element) {
    try {
      element.isDisplayed()
      return false
    }
    catch (StaleElementReferenceException e) {
      return true
    }
  }
}
