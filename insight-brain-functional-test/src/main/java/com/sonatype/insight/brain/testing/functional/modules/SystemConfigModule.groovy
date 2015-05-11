/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import com.sonatype.insight.brain.testing.functional.utils.BrowserInfo
import geb.Module

class SystemConfigModule
    extends Module
{
  static base = { $('#system-configuration-menu') }

  static content = {
    dropdown {
      def navigator = $('#system-configuration-menu-dropdown-toggle')
      BrowserInfo.chrome ? navigator.parent() : navigator
    }
    manageUsers(required: false) { $('#system-configuration-users') }
    manageRoles(required: false) { $('#system-configuration-roles') }
    manageAdministrators(required: false) { $('#system-configuration-administrators') }
    manageProductLicense(required: false) { $('#system-configuration-product-license') }
    manageProprietary(required: false) { $('#system-configuration-proprietary') }
    manageLdap(required: false) { $('#system-configuration-ldap') }
  }
}
