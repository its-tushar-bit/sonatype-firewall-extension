/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.7
 */
@Entity
@Table(name = "ldap_server")
public class LdapServer
    extends Nameable
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
   * Priority used for ordering servers
   *
   * @since 1.25
   */
  @Column(name = "priority")
  private int priority;

  public LdapServer() {
  }

  public LdapServer(String name) {
    setName(name);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(final int priority) {
    this.priority = priority;
  }
}
