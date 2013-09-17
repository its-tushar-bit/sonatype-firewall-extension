/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import com.sonatype.insight.brain.model.NameHelper;

import org.junit.Assert;
import org.junit.Test;

public class LdapConfigurationTest
{
  @Test
  public void testCopyConstructor() {
    String id = "id";
    String name = "name";
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

    LdapConfiguration orig = new LdapConfiguration();
    orig.setId(id);
    orig.setName(name);
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

    LdapConfiguration copy = new LdapConfiguration(orig);

    Assert.assertEquals(id, copy.getId());
    Assert.assertEquals(name, copy.getName());
    Assert.assertEquals(NameHelper.normalize(name), copy.getNameLowercaseNoWhitespace());
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
