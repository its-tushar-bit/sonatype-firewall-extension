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
    appliedTagList(required:false) { $('span', 'ng-repeat': startsWith('tag in appliedTags')).find('span') }
    appliedTagEmptyText(required:false) { $('em', 'ng-show': startsWith('appliedTags.length') ) }
    availableTagList(required:false) { $('span', 'ng-repeat': startsWith('tag in availableTags')).find('span') }
    availableTag(required:false) { name -> availableTagList.filter(text: name) }
    availableTagEmptyText(required:false) { $('em', 'ng-show': startsWith('availableTags.length') ) }

    tagFilterInput(required:false) { $('input', 'ng-model': 'tagSearch') }
  }
}
