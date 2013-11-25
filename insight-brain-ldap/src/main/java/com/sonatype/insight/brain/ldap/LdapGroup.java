/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.Objects;

/**
 * Group details populated from LDAP.
 *
 * @since 1.7
 */
public class LdapGroup
{
  private String groupname;

  private String dn;

  public String getGroupname() {
    return groupname;
  }

  public void setGroupname(final String groupname) {
    this.groupname = groupname;
  }

  public String getDn() {
    return dn;
  }

  public void setDn(final String dn) {
    this.dn = dn;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();

    buf.append("Group:");
    buf.append("\n\tGroupname: ").append(groupname);
    buf.append("\n\tDN: ").append(dn);

    return buf.toString();
  }
}
