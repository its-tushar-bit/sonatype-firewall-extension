/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.9
 */
class TagApplicationModule
    extends Module
{
  static content = {
    appliedTagList(required:false) { $('#tags .row-fluid .span6:first-child .clm-tag') }
    appliedTagEmptyText(required:false) { $('#tags .row-fluid .span6:first-child em') }
    availableTagList(required:false) { $('#tags .row-fluid .span6:last-child .clm-tag') }
    availableTag(required:false) { name -> availableTagList.filter(text: name) }
    availableTagEmptyText(required:false) { $('#tags .row-fluid .span6:last-child em') }

    tagFilterInput(required:false) { $('#tags input') }
  }
}
