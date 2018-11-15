/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    String systemPassword = "systemPassword";
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

    assertEquals(id, copy.getId());
    assertEquals(protocol, copy.getProtocol());
    assertEquals(hostname, copy.getHostname());
    assertEquals(port, copy.getPort());
    assertEquals(searchBase, copy.getSearchBase());
    assertEquals(authenticationMethod, copy.getAuthenticationMethod());
    assertEquals(saslRealm, copy.getSaslRealm());
    assertEquals(systemUsername, copy.getSystemUsername());
    assertEquals(systemPassword, copy.getSystemPassword());
    assertEquals(connectionTimeout, copy.getConnectionTimeout());
    assertEquals(retryDelay, copy.getRetryDelay());
  }
}
