/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.ldap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.7
 */
@Entity
@Table(name = "ldap_connection")
public class LdapConnection
    implements HasStringId, HasLdapServerId
{
  /**
   * Internal id used to identify this LDAP configuration
   *
   * @since 1.7
   */
  @Id
  @Column(name = "ldap_connection_id")
  private String id;

  /**
   * LdapServer id
   *
   * @since 1.7
   */
  @Column(name = "ldap_server_id")
  private String serverId;

  /**
   * @since 1.7
   */
  @Column(name = "protocol")
  @Enumerated(EnumType.STRING)
  private LdapProtocol protocol = LdapProtocol.LDAP;

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
  private int port = 389;

  /**
   * Read http://technet.microsoft.com/en-us/library/cc978021.aspx if "ldap search base" does not tell you anything.
   *
   * @since 1.7
   */
  @Column(name = "search_base")
  private String searchBase;

  @Column(name = "referral_ignored")
  private boolean referralIgnored;

  /**
   * @since 1.7
   */
  @Column(name = "authentication_method")
  @Enumerated(EnumType.STRING)
  private LdapAuthenticationMethod authenticationMethod = LdapAuthenticationMethod.NONE;

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
  @RotatableSecret
  @Column(name = "system_password")
  private char[] systemPassword;

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
  private int retryDelay = 30;

  public LdapConnection() {
  }

  public LdapConnection(LdapConnection other) {
    this.id = other.id;
    this.serverId = other.serverId;
    this.protocol = other.protocol;
    this.hostname = other.hostname;
    this.port = other.port;
    this.searchBase = other.searchBase;
    this.referralIgnored = other.referralIgnored;
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

  @Override
  public String getServerId() {
    return serverId;
  }

  @Override
  public void setServerId(String serverId) {
    this.serverId = serverId;
  }

  public LdapProtocol getProtocol() {
    return protocol;
  }

  public void setProtocol(LdapProtocol protocol) {
    this.protocol = protocol;
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

  public boolean isReferralIgnored() {
    return referralIgnored;
  }

  public void setReferralIgnored(boolean referralIgnored) {
    this.referralIgnored = referralIgnored;
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

  public char[] getSystemPassword() {
    return systemPassword;
  }

  public void setSystemPassword(char[] systemPassword) {
    this.systemPassword = systemPassword;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public int getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(int retryDelay) {
    this.retryDelay = retryDelay;
  }

  @JsonIgnore
  public String getUrl() {
    StringBuilder sb = new StringBuilder();
    sb.append(protocol.getProtocol()).append("://").append(hostname).append(':').append(port);
    if (StringUtils.isNotBlank(searchBase)) {
      sb.append('/').append(LdapUtils.escapeLdapUrl(searchBase));
    }

    return sb.toString();
  }
}
