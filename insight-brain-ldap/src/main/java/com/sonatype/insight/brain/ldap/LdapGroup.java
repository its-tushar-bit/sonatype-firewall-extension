/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LdapGroup)) {
      return false;
    }
    LdapGroup group = (LdapGroup)o;
    return StringUtils.equals(group.getGroupname(), groupname) && StringUtils.equals(group.getDn(), dn);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hashCode(groupname) + Objects.hashCode(dn);
  }
}
