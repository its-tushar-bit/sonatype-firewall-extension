/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.clm.dto.model.ProprietaryConfig
import com.sonatype.insight.brain.dataaccess.ProprietaryConfigDAO
import com.sonatype.insight.brain.testing.functional.configuration.ProprietaryComponentsPage
import spock.lang.Stepwise

/**
 * @since 1.11
 */
@Stepwise
class ProprietaryComponentSpec
    extends BaseSpec
{

  private static final ProprietaryConfig CONFIG = new ProprietaryConfig(packages: ['com.sonatype'],
      regexes: ['.*data\\.zip'])

  def setupSpec() {
    ProprietaryConfigDAO proprietaryConfigDAO = new ProprietaryConfigDAO(
        new File(serviceRule.configuration.sonatypeWork, 'data'))
    proprietaryConfigDAO.update(CONFIG)
  }

  def "Should be able to load and view existing configuration"() {
    when: 'first viewing the page'
      loginAsAdminVia(ProprietaryComponentsPage)

    then: 'we see already stored values'
      !error.displayed
      buttons.button('Reset').disabled
      buttons.save.disabled
      rows.size() == 2
      rows[0].value == CONFIG.packages[0]
      !rows[0].isRegex
      rows[1].value == CONFIG.regexes[0]
      rows[1].isRegex
  }

  def "Can add a new proprietary package to the list"() {
    when: 'adding a new package'
      form.currentEntry = 'org.sonatype'
      add.click()

    then: 'it is added to the end of the package list'
      !buttons.button('Reset').disabled
      !buttons.save.disabled
      rows.size() == 3
      rows[1].value == 'org.sonatype'
      !rows[1].isRegex
  }

  def "Can add a new proprietary regex to the list"() {
    when: 'adding a new regex'
      form.currentEntry = '.*sonatype.*'
      form.isRegex = true
      add.click()

    then: 'it is added to the end of the regex list'
      rows.size() == 4
      rows[3].value == '.*sonatype.*'
      rows[3].isRegex
  }

  def "Can save new entries"() {
    when: 'we save the new entries'
      buttons.save.click()

    then: 'the data is pushed to the server and the buttons disable'
      rows.size() == 4
      buttons.button('Reset').disabled
      buttons.save.disabled
  }

  def "Already specified packages result in an error"() {
    when: 'we add a package already stored'
      form.currentEntry = CONFIG.packages[0]
      add.click()

    then: 'an error is shown and the Reset button is enabled'
      error.text() == 'Package already specified'
      !buttons.button('Reset').disabled
      buttons.save.disabled
  }

  def "We can clear an error by resetting the form"() {
    when: 'we clear an existing error'
      buttons.button('Reset').click()

    then: 'the error is removed and buttons are disabled'
      !error.displayed
      buttons.save.disabled
      buttons.button('Reset').disabled
  }

  def "Already specified regexes result in an error"() {
    when: 'we add a package already stored'
      form.currentEntry = CONFIG.regexes[0]
      form.isRegex = true
      add.click()

    then: 'an error is shown and the Reset button is enabled'
      error.text() == 'Regex already specified'
      !buttons.button('Reset').disabled
      buttons.save.disabled
  }

  def "Packages with wildcards should be rejected"() {
    setup: 'clear existing error(already explicitly tested)'
      buttons.button('Reset').click()

    when: 'we add a package containing a wildcard'
      form.currentEntry = 'com.**'
      add.click()

    then: 'an error is shown and the Reset button is enabled'
      error.text().matches(~/Wildcards.*/)
      !buttons.button('Reset').disabled
      buttons.save.disabled
  }

  def "Packages which have a bad prefix should be rejected"() {
    setup: 'clear existing error(already explicitly tested)'
      buttons.button('Reset').click()

    when: 'we add a package containing a wildcard'
      form.currentEntry = '.'
      add.click()

    then: 'an error is shown and the Reset button is enabled'
      error.text().matches(~/Invalid package prefix.*/)
      !buttons.button('Reset').disabled
      buttons.save.disabled
  }

  def "Unparseable regexes result in an error"() {
    setup: 'clear existing error(already explicitly tested)'
      buttons.button('Reset').click()

    when: 'we add a regex that cannot be parsed and try to save it'
      form.currentEntry = '*'
      form.isRegex = true
      add.click()
      buttons.save.click()

    then: 'the server returns an error'
      waitFor { error.displayed }
      error.text().startsWith('Dangling meta character')
      report('regex error message')

    cleanup: 'clear existing error'
      buttons.button('Reset').click()
  }
}
