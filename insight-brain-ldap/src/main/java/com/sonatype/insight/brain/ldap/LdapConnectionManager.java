/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.codehaus.plexus.util.StringUtils;

/**
 * Manages encryption of LDAP connection details.
 * 
 * @since 1.7
 */
@Named
@Singleton
public class LdapConnectionManager
{
  public static final String FAKE_PASSWORD = "#~FAKE~CLM~PASSWORD~#";

  private static final String ENC = "CMMDwoV";

  private final LdapServerDAO serverDao = new LdapServerDAO();

  private final LdapConnectionDAO connDao = new LdapConnectionDAO();

  private final PlexusCipher cipher;

  @Inject
  public LdapConnectionManager(PlexusCipher cipher) {
    this.cipher = cipher;
  }

  public LdapConnection loadConnection(String serverId) {
    LdapConnection conn = connDao.getByServerId(serverId);
    if (conn == null) {
      conn = new LdapConnection();
      conn.setServerId(serverId);
    }
    return fakeOutPassword(conn);
  }

  public LdapConnection saveConnection(LdapConnection conn) {
    try {
      LdapConnection encrypted = encryptPassword(conn);
      if (encrypted.getId() != null) {
        connDao.update(encrypted);
      }
      else {
        connDao.insert(encrypted);
      }
      return fakeOutPassword(encrypted);
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }

  public void testConnection(LdapConnection conn) throws NamingException {
    try {
      LdapRealm.testConnection(restorePassword(conn));
    }
    catch (PlexusCipherException e) {
      throw new IllegalStateException(e);
    }
  }

  // local methods used by LdapRealm

  boolean isLdapConfigured() {
    return !serverDao.getAll().isEmpty(); // we have at least one LDAP server configured
  }

  LdapConnection getDecryptedConnection() {
    try {
      LdapServer server = serverDao.getAll().get(0);
      LdapConnection conn = connDao.getByServerId(server.getId());
      if (conn != null && StringUtils.isNotBlank(conn.getSystemPassword())) {
        conn.setSystemPassword(cipher.decryptDecorated(conn.getSystemPassword(), ENC));
      }
      return conn;
    }
    catch (Exception e) {
      throw new IllegalStateException("LDAP is not configured");
    }
  }

  // password encryption

  private static LdapConnection fakeOutPassword(LdapConnection conn) {
    if (StringUtils.isNotBlank(conn.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(conn);
      copy.setSystemPassword(FAKE_PASSWORD);
      return copy;
    }
    return conn;
  }

  private LdapConnection restorePassword(LdapConnection conn) throws PlexusCipherException {
    if (FAKE_PASSWORD.equals(conn.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(conn);
      String encryptedPassword = connDao.getByIdNotNull(conn.getId()).getSystemPassword();
      copy.setSystemPassword(cipher.decryptDecorated(encryptedPassword, ENC));
      return copy;
    }
    return conn;
  }

  private LdapConnection encryptPassword(LdapConnection conn) throws PlexusCipherException {
    if (StringUtils.isNotBlank(conn.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(conn);
      if (FAKE_PASSWORD.equals(conn.getSystemPassword())) {
        copy.setSystemPassword(connDao.getByIdNotNull(conn.getId()).getSystemPassword());
      }
      else {
        copy.setSystemPassword(cipher.encryptAndDecorate(conn.getSystemPassword(), ENC));
      }
      return copy;
    }
    return conn;
  }
}
