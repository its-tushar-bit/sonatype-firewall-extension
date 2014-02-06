/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * Describe a modal that offers a Yes/No choice.
 * @since 1.8
 */
class ModalModule
    extends Module
{
  def title
  def confirmText = 'Delete'
  def cancelText = 'Cancel'
  def okText = 'OK'

  def static content = {
    modals(required: false) { $('div.modal') }
    modal { modals.has('h3', text: title) }
    buttons { module ButtonsModule, modal }
    confirm { buttons.button(confirmText) }
    cancel { buttons.button(cancelText) }
    ok(required: false) { buttons.button(okText) }
    text { modal.find('p').text() }
  }
}
