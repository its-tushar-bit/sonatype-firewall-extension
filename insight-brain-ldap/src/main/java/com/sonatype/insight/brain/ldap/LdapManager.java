/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;

import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;

/**
 * Manages LDAP information.
 * 
 * @since 1.7
 */
@Named
@Singleton
public class LdapManager
{
  public static final String FAKE_PASSWORD = "#~FAKE~CLM~PASSWORD~#";

  private static final String ENC = "CMMDwoV";

  private final LdapServerDAO serverDao = new LdapServerDAO();

  private final LdapConnectionDAO connDao = new LdapConnectionDAO();

  private final LdapUserMappingDAO userDao = new LdapUserMappingDAO();

  private final PlexusCipher cipher;

  @Inject
  public LdapManager(PlexusCipher cipher) {
    this.cipher = cipher;
  }

  // LDAP connections

  /**
   * Loads the LDAP connection from the datastore, hiding any cached password.
   */
  public LdapConnection loadConnection(String serverId) {
    LdapConnection conn = connDao.getByServerId(serverId);
    if (conn == null) {
      conn = new LdapConnection();
      conn.setServerId(serverId);
    }
    return fakeOutPassword(conn);
  }

  /**
   * Saves the LDAP connection to the datastore, encrypting any new password.
   */
  public LdapConnection saveConnection(LdapConnection conn) {
    LdapConnection encrypted = encryptPassword(conn);
    if (encrypted.getId() != null) {
      connDao.update(encrypted);
    }
    else {
      connDao.insert(encrypted);
    }
    return fakeOutPassword(encrypted);
  }

  // LDAP verification

  /**
   * Tests the given connection by querying for the system context.
   * 
   * @throws NamingException if there is a problem with the connection
   */
  public void testConnection(LdapConnection conn) throws NamingException {
    new LdapQuery(restorePassword(conn), null).testConnection();
  }

  /**
   * Tests the given user mapping by querying for matching users.
   * 
   * @throws NamingException if there is a problem with the mapping
   */
  public List<LdapUser> testUserMapping(LdapUserMapping umap, long maxResults) throws NamingException {
    return new LdapQuery(getDecryptedConnection(), umap).getUsers(maxResults);
  }

  /**
   * Tests the given user mapping by attempting to authenticate the given credentials.
   * 
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public void testUserLogin(LdapUserMapping umap, String username, char[] password) throws NamingException {
    new LdapQuery(getDecryptedConnection(), umap).authenticateUser(username, password);
  }

  /**
   * Find a list of users searching the name, login and email address for the substring
   *
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public List<LdapUser> findUsers(String query, long maxResults) throws NamingException {
    LdapConnection conn = getDecryptedConnection();
    return new LdapQuery(conn, userDao.getByServerId(conn.getServerId())).getUsers(query, maxResults);
  }

  /**
   * Tests finding users with a query
   *
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public List<LdapUser> testFindUsers(LdapUserMapping umap, String query, long maxResults) throws NamingException {
    return new LdapQuery(getDecryptedConnection(), umap).getUsers(query, maxResults);
  }

  // local methods used by LdapRealm

  /**
   * Preliminary check used by Shiro to decide whether to start querying the LDAP realm.
   * 
   * @see LdapRealm#supports
   */
  public boolean isLdapEnabled() {
    return !serverDao.getAll().isEmpty(); // we have at least one LDAP server
  }

  /**
   * Slower authentication check; short-circuits if connection or mapping are missing.
   * 
   * @see LdapRealm#queryForAuthenticationInfo
   */
  void authenticateUser(String username, char[] password) throws NamingException {
    LdapConnection conn = getDecryptedConnection();
    LdapUserMapping umap = userDao.getByServerId(conn.getServerId());
    if (umap == null) {
      throw new IllegalStateException("LDAP user mapping is not configured");
    }
    new LdapQuery(conn, umap).authenticateUser(username, password);
  }

  // password encryption

  /**
   * Returns the current stored connection details with the password decrypted.
   */
  private LdapConnection getDecryptedConnection() {
    List<LdapServer> servers = serverDao.getAll();
    if (servers.isEmpty()) {
      throw new IllegalStateException("LDAP server is not configured");
    }
    LdapConnection conn = connDao.getByServerId(servers.get(0).getId());
    if (conn == null) {
      throw new IllegalStateException("LDAP connection is not configured");
    }
    if (StringUtils.isNotBlank(conn.getSystemPassword())) {
      try {
        conn.setSystemPassword(cipher.decryptDecorated(conn.getSystemPassword(), ENC));
      }
      catch (PlexusCipherException e) {
        throw new IllegalStateException(e);
      }
    }
    return conn;
  }

  /**
   * Returns a copy of the given connection for clients with the password faked-out.
   */
  private static LdapConnection fakeOutPassword(LdapConnection conn) {
    if (StringUtils.isNotBlank(conn.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(conn);
      copy.setSystemPassword(FAKE_PASSWORD);
      return copy;
    }
    return conn;
  }

  /**
   * Returns a copy of the given connection for testing with the real password restored.
   */
  private LdapConnection restorePassword(LdapConnection conn) {
    if (FAKE_PASSWORD.equals(conn.getSystemPassword())) {
      try {
        LdapConnection copy = new LdapConnection(conn);
        String encryptedPassword = connDao.getByIdNotNull(conn.getId()).getSystemPassword();
        copy.setSystemPassword(cipher.decryptDecorated(encryptedPassword, ENC));
        return copy;
      }
      catch (PlexusCipherException e) {
        throw new IllegalStateException(e);
      }
    }
    return conn;
  }

  /**
   * Returns a copy of the given connection for storage with the password encrypted.
   */
  private LdapConnection encryptPassword(LdapConnection conn) {
    if (StringUtils.isNotBlank(conn.getSystemPassword())) {
      try {
        LdapConnection copy = new LdapConnection(conn);
        if (FAKE_PASSWORD.equals(conn.getSystemPassword())) {
          copy.setSystemPassword(connDao.getByIdNotNull(conn.getId()).getSystemPassword());
        }
        else {
          copy.setSystemPassword(cipher.encryptAndDecorate(conn.getSystemPassword(), ENC));
        }
        return copy;
      }
      catch (PlexusCipherException e) {
        throw new IllegalStateException(e);
      }
    }
    return conn;
  }
}
