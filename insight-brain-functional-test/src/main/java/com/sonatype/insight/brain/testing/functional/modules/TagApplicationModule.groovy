/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
    appliedTagList(required:false) { $('span', 'ng-repeat': startsWith('tag in appliedTags')) }
    availableTagList(required:false) { $('span', 'ng-repeat': startsWith('tag in availableTags')) }

    tagFilterInput(required:false) { $('input', 'ng-model': 'tagSearch') }
  }
}
