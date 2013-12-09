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
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NamingSecurityException;

import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
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

  private long lastFailureMillis = 0;

  private int connectionFailures = 0;

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
    resetConnectionFailures();
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
    return new LdapQuery(getDecryptedConnection(), umap).getUsers(maxResults, true);
  }

  /**
   * Tests the given user mapping by attempting to authenticate the given credentials.
   * 
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public void testUserLogin(LdapUserMapping umap, String username, char[] password) throws NamingException {
    new LdapQuery(getDecryptedConnection(), umap).authenticateUser(username, password, false);
  }

  /**
   * Retrieve users from list of LdapUsers from an array of names which map to the UserID attribute
   *
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public List<LdapUser> getUsers(String[] names, long maxResults) throws NamingException {
    LdapConnection conn = getDecryptedConnection();
    return new LdapQuery(conn, userDao.getByServerId(conn.getServerId())).getUsers(names, maxResults);
  }

  public List<LdapGroup> getGroups(String[] names, long maxResults) throws NamingException {
    LdapConnection conn = getDecryptedConnection();
    return new LdapQuery(conn, userDao.getByServerId(conn.getServerId())).getGroups(names, maxResults);
  }

  /**
   * Find a list of users, searching the displayName attribute and adding a prefix and suffix wildcard to the nameFragment
   * 
   * @param nameFragment String to match against
   * @param maxResults Limit on the number of results to return
   * @return List of LdapUser objects that match the search criteria
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public List<LdapUser> findUsersByName(String nameFragment, long maxResults) throws NamingException {
    LdapConnection conn = getDecryptedConnection();
    return new LdapQuery(conn, userDao.getByServerId(conn.getServerId())).queryUsersByName(nameFragment, maxResults);
  }

  /**
   * Find a list of groups, searching the Group ID attribute and adding a prefix and suffix wildcard to the nameFragment
   *
   * @param nameFragment String to match against
   * @param maxResults Limit on the number of results to return
   * @return List of LdapGroup objects that match the search criteria
   */
  public List<LdapGroup> findGroupsByName(String nameFragment, long maxResults) throws NamingException {
    LdapConnection conn = getDecryptedConnection();
    return new LdapQuery(conn, userDao.getByServerId(conn.getServerId())).queryGroupsByName(nameFragment, maxResults);
  }

  /**
   * Tests finding users with a nameFragment
   * 
   * @param umap user mappings to find proper attributes for the ldap query
   * @param nameFragment String to match against
   * @param maxResults Limit on the number of results to return
   * @return List of LdapUser objects that match the search criteria
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public List<LdapUser> testFindUsersByName(LdapUserMapping umap, String nameFragment, long maxResults)
      throws NamingException
  {
    return new LdapQuery(getDecryptedConnection(), umap).queryUsersByName(nameFragment, maxResults);
  }

  // User authentication

  /**
   * Preliminary check used by Shiro to decide whether to start querying the LDAP realm.
   * 
   * @see LdapRealm#supports
   */
  public boolean isLdapEnabled() {
    List<LdapServer> servers = serverDao.getAll();
    return !servers.isEmpty() && connDao.getByServerId(servers.get(0).getId()) != null &&
        userDao.getByServerId(servers.get(0).getId()) != null;
  }

  public boolean isLdapGroupEnabled() {
    if (isLdapEnabled()) {
      LdapConnection conn = getDecryptedConnection();
      LdapUserMapping mapping = userDao.getByServerId(conn.getServerId());
      if (mapping != null) {
        return mapping.getGroupMappingType() != LdapGroupMappingType.NONE;
      }
    }
    return false;
  }

  /**
   * Returns the name of the first ldap server configured. Throws an exception when no LdapServers have been configured
   * in the database.
   */
  public String getLdapServerName() {
    List<LdapServer> servers = serverDao.getAll();
    if (servers.isEmpty()) {
      throw new IllegalStateException("LDAP server is not configured");
    }
    return servers.get(0).getName();
  }

  /**
   * Slower authentication check; short-circuits if connection or mapping are missing.
   * 
   * @see LdapRealm#queryForAuthenticationInfo
   */
  public LdapUser authenticateUser(String username, char[] password) throws NamingException {
    LdapConnection conn = getDecryptedConnection();
    LdapUserMapping umap = userDao.getByServerId(conn.getServerId());
    if (umap == null) {
      throw new IllegalStateException("LDAP user mapping is not configured");
    }
    checkValidConnection(conn);
    try {
      LdapUser user = new LdapQuery(conn, umap).authenticateUser(username, password, true);
      resetConnectionFailures();
      return user;
    }
    catch (NameNotFoundException e) {
      throw e; // unknown user
    }
    catch (NamingSecurityException e) {
      throw e; // bad password
    }
    catch (NamingException e) {
      recordConnectionFailure();
      throw e;
    }
  }

  // Password encryption

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
        synchronized (cipher) {
          conn.setSystemPassword(cipher.decryptDecorated(conn.getSystemPassword(), ENC));
        }
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
        synchronized (cipher) {
          copy.setSystemPassword(cipher.decryptDecorated(encryptedPassword, ENC));
        }
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
          synchronized (cipher) {
            copy.setSystemPassword(cipher.encryptAndDecorate(conn.getSystemPassword(), ENC));
          }
        }
        return copy;
      }
      catch (PlexusCipherException e) {
        throw new IllegalStateException(e);
      }
    }
    return conn;
  }

  // Retry delay support

  /**
   * Checks failure rate of LDAP connection; throws exception while retry delay is in effect.
   */
  private void checkValidConnection(LdapConnection conn) throws NamingException {
    if (lastFailureMillis > 0) {
      if (lastFailureMillis + (conn.getRetryDelay() * 1000) < System.currentTimeMillis()) {
        resetConnectionFailures(); // retry delay has elapsed
      }
      else if (connectionFailures >= 3) {
        throw new CommunicationException("Delaying retry of failing LDAP connection.");
      }
    }
  }

  /**
   * If we want absolute precision we should ideally use AtomicInteger or add synchronized to these methods.
   * However we don't need such precision as this is more about stopping requests for a grace period when we
   * detect a problem with the LDAP connection. Without AtomicInteger/synchronized we may miscount by one or
   * two requests when dealing with heavily concurrent requests, but will eventually apply the grace period.
   */
  private void recordConnectionFailure() {
    lastFailureMillis = System.currentTimeMillis();
    connectionFailures++;
  }

  private void resetConnectionFailures() {
    lastFailureMillis = 0;
    connectionFailures = 0;
  }
}
