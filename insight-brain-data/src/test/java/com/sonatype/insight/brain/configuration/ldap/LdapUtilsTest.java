/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class LdapUtilsTest
{

  @Test
  public void testEscapeLdapUrl() {
    // Blank or null URL
    String escapedUrl = LdapUtils.escapeLdapUrl("");
    assertThat(escapedUrl, is(""));
    escapedUrl = LdapUtils.escapeLdapUrl(null);
    assertThat(escapedUrl, nullValue());

    // Ensure that all unsafe characters are escaped to their Hex US ASCII values.
    escapedUrl = LdapUtils.escapeLdapUrl(" <>\"#%{}|\\^~[]`");
    assertThat(escapedUrl, is("%20%3C%3E%22%23%25%7B%7D%7C%5C%5E%7E%5B%5D%60"));

    // Ensure that reserved characters (?, !,',',(,),=) remain un-escaped.
    escapedUrl = LdapUtils.escapeLdapUrl("ldap://host.com:6666/!o=The White Album,c=US??sub?(cn=Rocky Raccoon)");
    assertThat(escapedUrl, is("ldap://host.com:6666/!o=The%20White%20Album,c=US??sub?(cn=Rocky%20Raccoon)"));

    // Ensure that the bindname extension escapes commas.
    escapedUrl = LdapUtils.escapeLdapUrl("ldap:///??sub??!bindname=cn=Apple Shop,co=Seed");
    assertThat(escapedUrl, is("ldap:///??sub??!bindname=cn=Apple%20Shop%2Cco=Seed"));
  }
}
