/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class LdapUtils
{
  private static final String[] UNSAFE_LDAP_URL_CHARACTERS = new String[]{" ", "<", ">", "\"", "#", "%", "{", "}",
    "|", "\\", "^", "~", "[", "]", "`"};

  /** Hex values, encoding defined by IETF RFC 1738. */
  private static final String[] ESCAPED_LDAP_URL_CHARACTERS = new String[]{"%20", "%3C", "%3E", "%22", "%23", "%25",
    "%7B", "%7D", "%7C", "%5C", "%5E", "%7E", "%5B", "%5D", "%60"};

  private static final String[] UNSAFE_LDAP_QUERY_CHARACTERS = new String[]{"\\", "(", ")", "*", "\u0000"};

  private static final String[] ESCAPED_LDAP_QUERY_CHARACTERS = new String[]{"\\5c", "\\28", "\\29", "\\2a", "\\00"};

  private static final String[] UNSAFE_LDAP_QUERY_CHARACTERS_WITHOUT_ASTERISK = new String[]{"\\", "(", ")", "\u0000"};

  private static final String[] ESCAPED_LDAP_QUERY_CHARACTERS_WITHOUT_ASTERISK = new String[]{"\\5c", "\\28", "\\29",
    "\\00"};

  private static final String ESCAPED_COMMA = "%2C";

  private static final String BINDNAME_URL_EXTENSION = "bindname";

  /**
   * <p>
   * Escapes an LDAP URL following the IETF <a href="http://tools.ietf.org/html/rfc2255">RFC 2255</a> which allows for
   * question marks, commas, equal signs, parenthesis and many other characters otherwise escaped in a standard web URL
   * <a href="http://tools.ietf.org/html/rfc1738">RFC 1738</a>.
   * </p>
   *
   * @param url The URL or URL fragment that should be escaped.
   * @return A URL that conforms to IETF RFC 2255.
   */
  public static String escapeLdapUrl(String url) {
    if (StringUtils.isBlank(url)) {
      return url;
    }

    String escapedUrl = StringUtils.replaceEach(url, UNSAFE_LDAP_URL_CHARACTERS, ESCAPED_LDAP_URL_CHARACTERS);

    int bindNameIndex = escapedUrl.toLowerCase(Locale.ENGLISH).indexOf(BINDNAME_URL_EXTENSION);
    if (bindNameIndex > -1) {
      escapedUrl = escapedUrl.substring(0, bindNameIndex)
          + escapedUrl.substring(bindNameIndex).replace(",", ESCAPED_COMMA);
    }

    return escapedUrl;
  }

  /**
   * <p>
   * Escapes an LDAP query attribute following the IETF <a href="http://tools.ietf.org/html/rfc2254">RFC 2254</a> which
   * demands that '*' '(' ')' 'NUL' all be escaped before being passed into the query.
   * </p>
   *
   * @param attribute The query attribute that should be escaped
   * @return a query attribute that conforms to IETF RFC 2254
   */
  public static String escapeLdapQueryAttribute(String attribute) {
    return escapeLdapQueryAttribute(attribute, false);
  }

  /**
   * <p>
   * Escapes an LDAP query attribute following the IETF <a href="http://tools.ietf.org/html/rfc2254">RFC 2254</a> which
   * demands that '*' '(' ')' 'NUL' all be escaped before being passed into the query.
   * </p>
   *
   * @param attribute The query attribute that should be escaped
   * @param allowAsterisk if true, will allow the '*' characters in the query parameter
   * @return a query attribute that conforms to IETF RFC 2254
   */
  public static String escapeLdapQueryAttribute(String attribute, boolean allowAsterisk) {
    if (allowAsterisk) {
      return StringUtils.replaceEach(attribute, UNSAFE_LDAP_QUERY_CHARACTERS_WITHOUT_ASTERISK,
          ESCAPED_LDAP_QUERY_CHARACTERS_WITHOUT_ASTERISK);
    }
    else {
      return StringUtils.replaceEach(attribute, UNSAFE_LDAP_QUERY_CHARACTERS, ESCAPED_LDAP_QUERY_CHARACTERS);
    }
  }
}
