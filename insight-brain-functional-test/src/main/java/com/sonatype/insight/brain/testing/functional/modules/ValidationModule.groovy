/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

import static com.sonatype.insight.brain.testing.functional.utils.ValidationConstants.*

/**
 * @since 1.7
 */
class ValidationModule
    extends Module
{

  def static content = {
    //parameterized matchers to find content
    div(required: false) { text -> $('div', text: text) }
    divStartsWith(required: false) { text -> $('div', text: startsWith(text)) }

    //validation elements
    required(required: false) { div(REQUIRED) }
    alphaNumeric(required: false) { div(ALPHA_NUMERIC) }
    noSpaces(required: false) { divStartsWith(NO_SPACES) }
    invalidEmail(required: false) { div(INVALID_EMAIL) }
    pattern(required: false) { div(PATTERN) }
    passwordMatches(required: false) { div(PASSWORDS_MUST_MATCH) }

    allValidations(requred: false) { [required, alphaNumeric, noSpaces, invalidEmail, pattern, passwordMatches] }

    //returns true if no validations are displayed
    errorFree(required: false) { !allValidations.any { it?.displayed == true } }
  }
}
