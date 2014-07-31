/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

import com.sonatype.insight.brain.testing.functional.BasePage
import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule

import geb.Module

/**
 * @since 1.11
 */
class ProprietaryComponentsPage
    extends BasePage 
{
  static url = 'assets/index.html#/proprietarycomponents'

  static at = { input.displayed }

  static content = {
    pageTitle { $('h1.page-title', text: 'Proprietary Components') }
    form { $('form[name="neditor"]') }
    input { $('form[name="neditor"] input[type="text"]') }
    add { $('form[name="neditor"] button') }
    rows(required: false) { moduleList ProprietaryTableRow, $('tr') }
    buttons(required: false) { module ButtonsModule, $('#proprietaryButtons') }
    error(required: false) { $('#proprietaryError .alert-message') }
  }
}

class ProprietaryTableRow
    extends Module
{
  static content = {
    cell(required: false) { int i -> $('td', i) }
    value { cell(0).text() }
    isRegex { cell(1).text().contains('regex') }
    delete { cell(1).find('button').click() }
  }
}


