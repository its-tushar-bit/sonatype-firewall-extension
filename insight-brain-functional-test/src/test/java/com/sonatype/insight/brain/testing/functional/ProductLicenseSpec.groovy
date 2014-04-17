/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.configuration.ProductLicensePage

import spock.lang.Stepwise

@Stepwise
class ProductLicenseSpec
    extends BaseSpec 
{
  def setupSpec() {
    loginAsAdminVia(ProductLicensePage)
  }

  def 'License information gets shown'() {
    expect: 'the expiry date is shown'
      waitFor { expiry.displayed }
      !expiry.text().empty

    and: 'the fingerprint is shown'
      fingerprint.displayed
      !fingerprint.text().empty
  }
}
