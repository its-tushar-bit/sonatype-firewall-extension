/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import org.junit.jupiter.api.Test;

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
    boolean referralIgnored = !new LdapConnection().isReferralIgnored();

    LdapConnection originalLdapConnection = new LdapConnection();
    originalLdapConnection.setId(id);
    originalLdapConnection.setProtocol(protocol);
    originalLdapConnection.setHostname(hostname);
    originalLdapConnection.setPort(port);
    originalLdapConnection.setSearchBase(searchBase);
    originalLdapConnection.setReferralIgnored(referralIgnored);
    originalLdapConnection.setAuthenticationMethod(authenticationMethod);
    originalLdapConnection.setSaslRealm(saslRealm);
    originalLdapConnection.setSystemUsername(systemUsername);
    originalLdapConnection.setSystemPassword(systemPassword);
    originalLdapConnection.setConnectionTimeout(connectionTimeout);
    originalLdapConnection.setRetryDelay(retryDelay);

    LdapConnection copyLdapConnection = new LdapConnection(originalLdapConnection);

    assertThat(copyLdapConnection.getId()).isEqualTo(id);
    assertThat(copyLdapConnection.getProtocol()).isEqualTo(protocol);
    assertThat(copyLdapConnection.getHostname()).isEqualTo(hostname);
    assertThat(copyLdapConnection.getPort()).isEqualTo(port);
    assertThat(copyLdapConnection.getSearchBase()).isEqualTo(searchBase);
    assertThat(copyLdapConnection.isReferralIgnored()).isEqualTo(referralIgnored);
    assertThat(copyLdapConnection.getAuthenticationMethod()).isEqualTo(authenticationMethod);
    assertThat(copyLdapConnection.getSaslRealm()).isEqualTo(saslRealm);
    assertThat(copyLdapConnection.getSystemUsername()).isEqualTo(systemUsername);
    assertThat(copyLdapConnection.getSystemPassword()).isEqualTo(systemPassword);
    assertThat(copyLdapConnection.getConnectionTimeout()).isEqualTo(connectionTimeout);
    assertThat(copyLdapConnection.getRetryDelay()).isEqualTo(retryDelay);
  }
}
