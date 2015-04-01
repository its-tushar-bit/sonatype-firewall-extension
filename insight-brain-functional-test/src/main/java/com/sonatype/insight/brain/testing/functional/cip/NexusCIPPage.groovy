/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.cip

/**
 * The embedded CIP as seen by Nexus.
 * @since 1.12
 */
class NexusCIPPage
    extends AbstractCIPPage
{
  static url = 'assets/version-graph/rm/nexus/index.html#/'

  static content = {
    selectAnAppText(required: false) { $('#select-application') }
    appSelect { $('#selectApp') }
    options { appSelect.find('option')*.text().tail() }
  }
}
