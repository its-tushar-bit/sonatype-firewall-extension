/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.8
 */
class TagModule
    extends Module
{
  static content = {
    newTagButton { $('button', 'ng-click': 'createNew()') }

    tagList(required:false) { $('span', 'ng-repeat': startsWith('tag in tags')) }
    tag(required:false) { index -> tagList(index).find('span') }
    delete { tag -> tag.find('i', title: startsWith('Delete')).click() }
    appliedMarker { tag -> tag.find('.appliedTagCount') }

    //form controls(only visible while editing)
    tagEditor(required:false) { $('form', name: 'tagEditor') }
    name(required:false)  { tagEditor.name() }
    description(required:false)  { tagEditor.description() }
    color(requied:false) { name -> tagEditor.find('.' + name + 'Label') }
    buttons { module ButtonsModule }

    //client validation error messaging
    nameValidations(required: false) { module ValidationModule, name.parent() }

    //server error messaging
    serverAlerts { $('div', 'clm-alerts': 'alerts') }
    cancelServerAlert { serverAlerts.find('button') }
    editAlerts(required:false) { $('div', 'clm-alerts': 'editorAlerts') }
    cancelEditAlert { editAlerts.find('button') }
  }

  def createNewTag(name = 'New Tag', description = 'Tag description', color = 'black') {
    waitFor { newTagButton.displayed }
    newTagButton.click()
    this.name = name
    this.description = description
    this.color(color).click()
  }
}
