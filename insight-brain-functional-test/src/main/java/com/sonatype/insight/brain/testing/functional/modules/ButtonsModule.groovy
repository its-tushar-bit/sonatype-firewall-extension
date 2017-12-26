/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module
import geb.module.FormElement

/**
 * @since 1.8
 */
class ButtonsModule
    extends Module
{
  static content = {
    button(required: false) { text -> $('button', text: text).module(FormElement) }
    save(required: false) { button('Save') }
    cancel(required: false) { button('Cancel') }
  }
}
