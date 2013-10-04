/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

/**
 * @since 1.7
 */
class LDAPConnectionConfigurationPage
    extends LDAPConfigurationPage
{
  static at = { isActiveTab(connectionTab) }

  static content = {
    // connection details editor
    ldapConnectionEditor { $('#ldapConnectionEditor') }
    hostname { $('#hostname') }
    protocol { $('#protocol') }
    port { $('#port') }
    searchBase { $('#searchBase') }
    authenticationMethod { $('#authenticationMethod') }
    saslRealm { $('#saslRealm') }
    systemUsername { $('#systemUsername') }
    systemPassword { $('#systemPassword') }
    connectionTimeout { $('#connectionTimeout') }
    retryDelay { $('#retryDelay') }

    requiredFields { [hostname, searchBase] }
    //controls

    reset { $('.ldap-button-group button', text: 'Reset') }
    save { $('.ldap-button-group button', text: 'Save') }
  }
}
