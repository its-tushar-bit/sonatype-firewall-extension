/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

@Stepwise
class HelpModuleSpec
    extends BaseSpec 
{

  def setupSpec() {
    loginAsAdminVia()
  }

  def "Links to external pages are presented in the UI"() {
    when: 'We click the "help" dropdown'
      helpLinks.dropdown.click()
      waitFor { helpLinks.documentation.displayed }

    then: 'We are presented with the links to the external documentation'
      helpLinks.documentation.@href == 'http://links.sonatype.com/products/clm/doc'
      helpLinks.documentation.@target == '_blank'

    and: 'links to create a new support request'
      helpLinks.support.@href == 'http://links.sonatype.com/products/clm/support'
      helpLinks.support.@target == '_blank'
  }
}
