/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.7
 */
@Entity
@Table(name = "ldap_server")
public class LdapServer
    implements HasStringId
{
  /**
   * Internal id used to identify this LDAP server
   * 
   * @since 1.7
   */
  @Id
  @Column(name = "ldap_server_id")
  private String id;

  /**
   * Human readable name of this LDAP server
   * 
   * @since 1.7
   */
  @Column(name = "name")
  private String name;

  /**
   * Canonical name form used in uniqueness constraint
   * 
   * @since 1.7
   */
  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

  public LdapServer() {
  }

  public LdapServer(LdapServer other) {
    this.id = other.id;
    this.name = other.name;
    this.nameLowercaseNoWhitespace = other.nameLowercaseNoWhitespace;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    nameLowercaseNoWhitespace = NameHelper.normalize(name);
    this.name = name;
  }

  public String getNameLowercaseNoWhitespace() {
    return nameLowercaseNoWhitespace;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * nameLowercaseNoWhitespace field. If this method is not defined, jackson will set/access the
   * nameLowercaseNoWhitespace field directly via reflection, possibly setting it to an incorrect value.
   * 
   * @deprecated This method should not be used explicitly.
   */
  @SuppressWarnings("unused")
  private void setNameLowercaseNoWhitespace(String nameLowercaseNoWhitespace) {
  }
}
