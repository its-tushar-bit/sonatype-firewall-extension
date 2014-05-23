/*
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
    newTagButton { $('#tags .tag-list button') }

    tagList(required:false) { moduleList Tag, $('span[ng-click="editTag(tag)"]') }

    //form controls(only visible while editing)
    tagEditor(required:false) { $('form[name="tagEditor"]') }
    name(required:false)  { $("#tagEditorLabel") }
    description(required:false)  { $('form[name="tagEditor"] textarea') }
    color(required:false) { name -> tagEditor.find('.' + name + 'Label') }
    buttons { module ButtonsModule }

    //client validation error messaging
    nameValidations(required: false) { module ValidationModule, name.parent() }

    //server error messaging
    serverAlerts { $('div[clm-alerts="alerts"]') }
    cancelServerAlert { $('div[clm-alerts="alerts"] button') }
    editAlerts(required:false) { $('div[clm-alerts="editorAlerts"]') }
    cancelEditAlert { $('div[clm-alerts="editorAlerts"] button') }
  }

  def createNewTag(name = 'New Tag', description = 'Tag description', color = 'black') {
    waitFor { newTagButton.displayed }
    newTagButton.click()
    this.name = name
    this.description = description
    this.color(color).click()
  }
}


class Tag
  extends Module
{
  static content = {
    delete { $('i.icon-remove.label-remove') }
    appliedMarker(required: false) { $('.applied-tag-count') }
    body { $('.clm-tag') }
    text { body.text() }
    isColor { String color -> body.classes().contains("${color}Label".toString())}
  }
}