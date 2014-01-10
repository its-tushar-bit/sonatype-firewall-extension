/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import com.sonatype.insight.brain.testing.functional.ReportViolationsPage
import geb.Module

/**
 * @since 1.8
 */
class UserOptionsModule extends Module {
  static content = {
    displayName(wait: true) { $('.user-name') }
    optionsDropdown(wait: true) { $('.dashboard-user a.btn') }
    logout (wait: true, to: ReportViolationsPage) { $('a', text: 'Logout') }
    openChangePassword { $('a', text: 'Change Password') }
  }

  void logoutClick() {
    optionsDropdown.click()
    logout.click()
  }

  void changePasswordClick() {
    optionsDropdown.click()
    openChangePassword.click()
  }
}
