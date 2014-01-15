/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * Describe a modal that offers a Yes/No choice.
 * @since 1.8
 */
class ModalModule extends Module
{
  def title
  def confirmText = 'Delete'
  def cancelText = 'Cancel'
  def okText = 'OK'

  def static content = {
    modals(required: false) { $('div.modal') }
    modal { modals.has('h3', text: title) }
    confirm { modal.find('button', text: confirmText ) }
    cancel { modal.find('button', text: cancelText) }
    ok(required: false) { modal.find('button', text: okText) }
  }
}
