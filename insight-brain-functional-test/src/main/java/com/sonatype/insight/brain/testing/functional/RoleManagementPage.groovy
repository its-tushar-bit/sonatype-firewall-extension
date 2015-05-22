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
    pageTitle { $('h1.page-title') }
    builtinRoles(wait: true) { moduleList RoleSummary, $('#builtin-roles .role-item') }
    customRoles(required: false) { moduleList RoleSummary, $('#custom-roles .role-item') }
    createRole(required:true) { $('#create-role') }
  }
}

class RoleSummary extends Module {
  static content = {
    name { $('.role-summary h3') }
    description { $('.role-summary div') }
  }
}
