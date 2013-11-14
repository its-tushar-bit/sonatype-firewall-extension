package com.sonatype.insight.brain.ldap;

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

    buf.append("User:");
    buf.append("\n\tUsername: ").append(groupname);
    buf.append("\n\tDN: ").append(dn);

    return buf.toString();
  }
}
