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
class LabelModule
    extends Module
{
  static content = {
    newLabelButton { $('button[ng-click="createNew()"]') }

    labelList(required:false) { $('span', 'ng-repeat': startsWith('label in applicableLabel.labels')) }
    label(required:false) { index -> labelList[index].find('span') }
    delete { tag -> tag.find('i[title]').click() }

    //form controls(only visible while editing)
    labelEditor(required:false) { $('form[name="labelEditor"]') }
    name(required:false)  { labelEditor.label() }
    description(required:false)  { labelEditor.description() }
    color(requied:false) { name -> labelEditor.find('.' + name + 'Label') }
    buttons { module ButtonsModule, labelEditor }

    //client validation error messaging
    nameValidations(required: false) { module ValidationModule, name.parent() }
    errorFree { nameValidations.errorFree && serverAlerts.children().size() == 0 }

    //server error messaging
    serverAlerts { $('div[clm-alerts="alerts"]') }
    cancelServerAlert { $('div[clm-alerts="alerts"] button') }
    editAlerts(required:false) { $('div[clm-alerts="editorAlerts"]') }
    cancelEditAlert(required:false) { $('div[clm-alerts="editorAlerts"] button') }
  }

  def createNewLabel(name = 'NewLabel', description = 'Label description', color = 'black') {
    waitFor { newLabelButton.displayed }
    newLabelButton.click()
    this.name = name
    this.description = description
    this.color(color).click()
  }
}
