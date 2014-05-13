/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

import com.sonatype.insight.brain.testing.functional.modules.ButtonsModule
import geb.Module

/**
 * @since 1.11
 */
class ProprietaryComponentsPage
    extends ConfigurationPage
{
  static url = "${ConfigurationPage.url}/proprietarycomponents"

  static at = { pageTitle.displayed }

  static content = {
    pageTitle { $('h1.page-title', text: 'Proprietary Components') }
    form { $('form[name="neditor"]') }
    add { form.find('button') }
    rows(required: false) { moduleList ProprietaryTableRow, $('tr') }
    buttons(required: false) { module ButtonsModule, $('#proprietaryButtons') }
    error(required: false) { $('#proprietaryError') }
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


