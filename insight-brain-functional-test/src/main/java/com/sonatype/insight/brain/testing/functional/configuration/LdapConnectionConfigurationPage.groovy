/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

/**
 * @since 1.7
 */
class LdapConnectionConfigurationPage
    extends LdapConfigurationPage
{
  static at = { hostname.displayed }

  static content = {
    // connection details editor
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
    testConnection { $('.ldap-button-group button.btn-success') }
    reset { $('.ldap-button-group div button:first-child') }
    save { $('.ldap-button-group button.btn-primary') }
  }
}
