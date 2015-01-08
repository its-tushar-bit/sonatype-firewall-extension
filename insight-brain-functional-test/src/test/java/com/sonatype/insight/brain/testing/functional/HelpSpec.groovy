/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import spock.lang.Stepwise

@Stepwise
class HelpSpec
    extends BaseSpec
{

  def setupSpec() {
    loginAsAdminVia()
  }

  def "Links to external pages are presented in the UI"() {
    when: 'We are on any page'
      // noop, just need to confirm the elements are onscreen since ChromeDriver will not allow clicking of the
      // dropdown(element is not visible)

    then: 'We are presented with the links to the external documentation'
      helpLinks.dropdown.displayed
      helpLinks.documentation.@href == 'http://links.sonatype.com/products/clm/doc/' + getMajorMinorVersion()
      helpLinks.documentation.@target == '_blank'

    and: 'links to create a new support request'
      helpLinks.support.@href == 'http://links.sonatype.com/products/clm/support'
      helpLinks.support.@target == '_blank'
  }

  String getMajorMinorVersion() {
      Properties props = new Properties()
      props.load(getClass().getResourceAsStream("/version.properties"))

      String[] version = props.get("version").split("\\.")
      return version[0] + "." + version[1]
  }
}
