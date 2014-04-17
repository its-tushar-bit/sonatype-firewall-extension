/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

class ProductLicensePage
    extends ConfigurationPage
{
  static url = "${ConfigurationPage.url}/productlicense"

  static at = { $('#license').displayed }

  static content = {
    expiry(required: false) { $('#license-expiry') }
    fingerprint(required: false) { $('#license-fingerprint') }
  }
}
