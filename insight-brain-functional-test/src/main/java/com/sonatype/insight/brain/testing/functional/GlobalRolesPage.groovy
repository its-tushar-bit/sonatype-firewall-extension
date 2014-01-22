/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.RoleMappingModule


class GlobalRolesPage
    extends BasePage
{
  static url = "assets/index.html#/management/security/global"

  static at = { $('#security').displayed }

  static content = {
    mapping(wait: true) { module RoleMappingModule }
  }
}
