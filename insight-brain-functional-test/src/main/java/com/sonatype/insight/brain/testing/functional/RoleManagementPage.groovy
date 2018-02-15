/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Module
import geb.module.FormElement

class RoleManagementPage
extends BasePage {
  static url = "assets/index.html#/roles"

  static at = { builtinRoles.size() > 0 }

  static content = {
    pageTitle { $('.iq-tile-header__title h2') }
    builtinRoles(wait: true) { $('#builtin-roles .iq-list__item').moduleList(RoleSummary) }
    customRoles(required: false) { $('#custom-roles .role-name-list-item').moduleList(RoleSummary) }
    createRole(required:true) { $('#create-role').module(FormElement) }
  }
}

class RoleSummary extends Module {
  static content = {
    name { $('.role-name') }
    description { $('.iq-list__subtext') }
  }
}
