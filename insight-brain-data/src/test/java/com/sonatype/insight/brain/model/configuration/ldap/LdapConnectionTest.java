/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapConnectionTest
{
  @Test
  public void testCopyConstructor() {
    String id = "id";
    LdapProtocol protocol = LdapProtocol.LDAPS;
    String hostname = "hostname";
    int port = 389;
    String searchBase = "searchBase";
    LdapAuthenticationMethod authenticationMethod = LdapAuthenticationMethod.DIGESTMD5;
    String saslRealm = "saslRealm";
    String systemUsername = "systemUsername";
    char[] systemPassword = "systemPassword".toCharArray();
    int connectionTimeout = 123;
    int retryDelay = 345;

    LdapConnection orig = new LdapConnection();
    orig.setId(id);
    orig.setProtocol(protocol);
    orig.setHostname(hostname);
    orig.setPort(port);
    orig.setSearchBase(searchBase);
    orig.setAuthenticationMethod(authenticationMethod);
    orig.setSaslRealm(saslRealm);
    orig.setSystemUsername(systemUsername);
    orig.setSystemPassword(systemPassword);
    orig.setConnectionTimeout(connectionTimeout);
    orig.setRetryDelay(retryDelay);

    LdapConnection copy = new LdapConnection(orig);

    assertThat(copy.getId()).isEqualTo(id);
    assertThat(copy.getProtocol()).isEqualTo(protocol);
    assertThat(copy.getHostname()).isEqualTo(hostname);
    assertThat(copy.getPort()).isEqualTo(port);
    assertThat(copy.getSearchBase()).isEqualTo(searchBase);
    assertThat(copy.getAuthenticationMethod()).isEqualTo(authenticationMethod);
    assertThat(copy.getSaslRealm()).isEqualTo(saslRealm);
    assertThat(copy.getSystemUsername()).isEqualTo(systemUsername);
    assertThat(copy.getSystemPassword()).isEqualTo(systemPassword);
    assertThat(copy.getConnectionTimeout()).isEqualTo(connectionTimeout);
    assertThat(copy.getRetryDelay()).isEqualTo(retryDelay);
  }
}
