/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

import geb.Page

/**
 * @since 1.7
 */
class ConfigurationPage
    extends Page
{
  static url = 'assets/index.html#/management/configuration'

  static content = {
    productLicense { $('a', text: 'Product License') }
    proprietaryPackages { $('a', text: 'Proprietary Packages') }
    ldap { $('a', text: 'LDAP') }
  }
}
