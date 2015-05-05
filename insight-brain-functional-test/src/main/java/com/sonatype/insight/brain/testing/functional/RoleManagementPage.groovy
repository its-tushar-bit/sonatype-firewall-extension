/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

class RoleManagementPage
    extends BasePage
{
  static url = "assets/index.html#/roles"

  static at = { roleItems.size() > 0 }

  static content = {
    roleItems(wait: true) { $('.role-item') }
    roleName { index -> roleItems.getAt(index).find('.role-summary > h3').text() }
  }
}
