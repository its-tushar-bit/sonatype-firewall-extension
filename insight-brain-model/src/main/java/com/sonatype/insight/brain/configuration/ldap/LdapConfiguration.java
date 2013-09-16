/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.7
 */
@Entity
@Table(name = "ldap_configuration")
public class LdapConfiguration
    implements HasStringId
{
  /**
   * Internal id used to identify this LDAP configuration
   * 
   * @since 1.7
   */
  @Id
  @Column(name = "ldap_configuration_id")
  private String id;

  /**
   * Human readable name of this LDAP configuration
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

  /**
   * @since 1.7
   */
  @Column(name = "protocol")
  @Enumerated(EnumType.STRING)
  private LdapProtocol protocol;

  /**
   * LDAP server hostname
   * 
   * @since 1.7
   */
  @Column(name = "hostname")
  private String hostname;

  /**
   * LDAP server port
   * 
   * @since 1.7
   */
  @Column(name = "port")
  private int port;

  /**
   * Read http://technet.microsoft.com/en-us/library/cc978021.aspx if "ldap search base" does not tell you anything.
   * 
   * @since 1.7
   */
  @Column(name = "search_base")
  private String searchBase;

  /**
   * @since 1.7
   */
  @Column(name = "authentication_method")
  @Enumerated(EnumType.STRING)
  private LdapAuthenticationMethod authenticationMethod;

  /**
   * Optional SASL realm for digest authentication.
   * 
   * @since 1.7
   */
  @Column(name = "sasl_realm")
  private String saslRealm;

  /**
   * Username or DN to bind to LDAP server with.
   * 
   * @since 1.7
   */
  @Column(name = "system_username")
  private String systemUsername;

  /**
   * The password to bind with.
   * 
   * @since 1.7
   */
  @Column(name = "system_password")
  private String systemPassword;

  /**
   * From nexus ui help: the number of seconds to wait before timeout on connection to LDAP server. The key takeaway,
   * the value is in <strong>seconds</strong>.
   * 
   * @since 1.7
   */
  @Column(name = "connection_timeout")
  private int connectionTimeout = 30;

  /**
   * The number of <strong>seconds</strong> to wait before retrying a request to the LDAP server.
   * 
   * @since 1.7
   */
  @Column(name = "retry_delay")
  private long retryDelay = 300;

  public LdapConfiguration() {
  }

  public LdapConfiguration(LdapConfiguration other) {
    this.id = other.id;
    this.name = other.name;
    this.nameLowercaseNoWhitespace = other.nameLowercaseNoWhitespace;
    this.protocol = other.protocol;
    this.hostname = other.hostname;
    this.port = other.port;
    this.searchBase = other.searchBase;
    this.authenticationMethod = other.authenticationMethod;
    this.saslRealm = other.saslRealm;
    this.systemUsername = other.systemUsername;
    this.systemPassword = other.systemPassword;
    this.connectionTimeout = other.connectionTimeout;
    this.retryDelay = other.retryDelay;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public LdapProtocol getProtocol() {
    return protocol;
  }

  public void setProtocol(LdapProtocol protocol) {
    this.protocol = protocol;
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

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getSearchBase() {
    return searchBase;
  }

  public void setSearchBase(String searchBase) {
    this.searchBase = searchBase;
  }

  public LdapAuthenticationMethod getAuthenticationMethod() {
    return authenticationMethod;
  }

  public void setAuthenticationMethod(LdapAuthenticationMethod authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
  }

  public String getSaslRealm() {
    return saslRealm;
  }

  public void setSaslRealm(String saslRealm) {
    this.saslRealm = saslRealm;
  }

  public String getSystemUsername() {
    return systemUsername;
  }

  // XXX poor choice of method name, depending on auth method this can be either username or user DN
  public void setSystemUsername(String systemUsername) {
    this.systemUsername = systemUsername;
  }

  public String getSystemPassword() {
    return systemPassword;
  }

  public void setSystemPassword(String systemPassword) {
    this.systemPassword = systemPassword;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public long getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(long retryDelay) {
    this.retryDelay = retryDelay;
  }

  public String getUrl() {
    StringBuilder sb = new StringBuilder();
    sb.append(protocol.getProtocol()).append("://").append(hostname).append(':').append(port);
    return sb.toString();
  }
}
