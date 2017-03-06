/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import geb.Module

class RoleManagementPage
extends BasePage {
  static url = "assets/index.html#/roles"

  static at = { builtinRoles.size() > 0 }

  static content = {
    pageTitle { $('.iq-tile-header__title h2') }
    builtinRoles(wait: true) { moduleList RoleSummary, $('#builtin-roles .iq-action-list__item') }
    customRoles(required: false) { moduleList RoleSummary, $('#custom-roles .iq-action-list__item') }
    createRole(required:true) { $('#create-role') }
  }
}

class RoleSummary extends Module {
  static content = {
    name { $('.iq-action-list__text') }
    description { $('.iq-action-list__subtext') }
  }
}
