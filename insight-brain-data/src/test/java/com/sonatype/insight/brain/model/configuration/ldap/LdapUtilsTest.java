/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapUtilsTest
{
  @Test
  public void testEscapeLdapUrl() {
    // Blank or null URL
    String escapedUrl = LdapUtils.escapeLdapUrl("");
    assertThat(escapedUrl).isEqualTo("");
    escapedUrl = LdapUtils.escapeLdapUrl(null);
    assertThat(escapedUrl).isNull();

    // Ensure that all unsafe characters are escaped to their Hex US ASCII values.
    escapedUrl = LdapUtils.escapeLdapUrl(" <>\"#%{}|\\^~[]`");
    assertThat(escapedUrl).isEqualTo("%20%3C%3E%22%23%25%7B%7D%7C%5C%5E%7E%5B%5D%60");

    // Ensure that reserved characters (?, !,',',(,),=) remain un-escaped.
    escapedUrl = LdapUtils.escapeLdapUrl("ldap://host.com:6666/!o=The White Album,c=US??sub?(cn=Rocky Raccoon)");
    assertThat(escapedUrl).isEqualTo("ldap://host.com:6666/!o=The%20White%20Album,c=US??sub?(cn=Rocky%20Raccoon)");

    // Ensure that the bindname extension escapes commas.
    escapedUrl = LdapUtils.escapeLdapUrl("ldap:///??sub??!bindname=cn=Apple Shop,co=Seed");
    assertThat(escapedUrl).isEqualTo("ldap:///??sub??!bindname=cn=Apple%20Shop%2Cco=Seed");
  }

  @Test
  public void testEscapeQueryAttribute() {
    // validate each escaped character
    assertThat(LdapUtils.escapeLdapQueryAttribute("\\()*" + '\u0000')).isEqualTo("\\5c\\28\\29\\2a\\00");
    // as well as the string initially reported
    assertThat(LdapUtils.escapeLdapQueryAttribute("*)(uid=*))(|(uid=*"))
        .isEqualTo("\\2a\\29\\28uid=\\2a\\29\\29\\28|\\28uid=\\2a");
  }

  @Test
  public void testEscapeQueryAttribute_allowWildcard() {
    // validate each escaped character
    assertThat(LdapUtils.escapeLdapQueryAttribute("\\()*" + '\u0000', true)).isEqualTo("\\5c\\28\\29*\\00");
    // as well as the string initially reported
    assertThat(LdapUtils.escapeLdapQueryAttribute("*)(uid=*))(|(uid=*", true))
        .isEqualTo("*\\29\\28uid=*\\29\\29\\28|\\28uid=*");
  }
}
