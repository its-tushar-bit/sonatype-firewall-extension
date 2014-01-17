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
class ApplicationEvaluationModule extends Module
{
  static content = {
    //this content is all in the popup dialog
    dialog(required: false) { $('#evaluate-bundle-dialog') }
    file(required: false) { $('#bundleFile') }
    application(required: false) { $('#bundleApplication') }
    stage(required: false) { $('#bundleStage') }
    status(required: false) { $('#evaluate-bundle-status') }
    viewReport(required: false) { dialog.find('a', text: 'View Report') }
    close(required: false) { dialog.find('button', text: 'Close') }
    upload(required: false) { dialog.find('button', text: 'Upload') }
    cancel(required: false) { dialog.find('button', text: 'Cancel') }
  }
  
  def getSelectedApplicationOption() {
    return application.find('option', value:application.value()).text()
  }
}
