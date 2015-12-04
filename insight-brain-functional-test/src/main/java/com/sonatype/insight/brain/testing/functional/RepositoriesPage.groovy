/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ContextTabsModule

class RepositoriesPage
    extends OwnerManagementPage
{
  static url = 'assets/index.html#/management/repositories/security'
  static at = { $('#repositories-editor').displayed }

  static content = {
    repositoriesName(required: false) { $('.inline-editor.head .read-only') }

    tabs { module ContextTabsModule }
  }
}
