/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import org.junit.Assert;
import org.junit.Test;

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

    Assert.assertEquals(id, copy.getId());
    Assert.assertEquals(protocol, copy.getProtocol());
    Assert.assertEquals(hostname, copy.getHostname());
    Assert.assertEquals(port, copy.getPort());
    Assert.assertEquals(searchBase, copy.getSearchBase());
    Assert.assertEquals(authenticationMethod, copy.getAuthenticationMethod());
    Assert.assertEquals(saslRealm, copy.getSaslRealm());
    Assert.assertEquals(systemUsername, copy.getSystemUsername());
    Assert.assertEquals(systemPassword, copy.getSystemPassword());
    Assert.assertEquals(connectionTimeout, copy.getConnectionTimeout());
    Assert.assertEquals(retryDelay, copy.getRetryDelay());
  }
}
