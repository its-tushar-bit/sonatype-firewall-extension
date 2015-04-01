/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.viewdetails

/**
 * @since 1.12
 */
class NexusViewDetailsPage
    extends AbstractViewDetailsPage
{
  static url = 'assets/version-graph/rm/nexus/viewdetails.html'

  static at = { sectionHeaders.size > 2 }

}
