/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
    input { $('input[ng-model^="vm.component"]') }
    regex { $('#isRegex') }
    add { $('form button') }
    rows(required: false) { $('tr').moduleList(ProprietaryTableRow) }
    buttons(required: false) { $('#proprietaryButtons').module(ButtonsModule) }
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
