/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NamingSecurityException;

import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.codehaus.plexus.util.StringUtils;

/**
 * Manages LDAP information.
 * 
 * @since 1.7
 */
@Named
@Singleton
public class LdapService
{
  public static final String FAKE_PASSWORD = "#~FAKE~PASSWORD~#";

  private static final String ENC = "CMMDwoV";

  private final LdapServerDAO serverDao = new LdapServerDAO();

  private final LdapConnectionDAO connDao = new LdapConnectionDAO();

  private final LdapUserMappingDAO userDao = new LdapUserMappingDAO();

  private final PlexusCipher cipher;

  @Inject
  public LdapService(PlexusCipher cipher) {
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
    resetConnectionFailures(conn);
    return fakeOutPassword(encrypted);
  }

  // LDAP verification

  /**
   * Tests the given connection by querying for the system context.
   * 
   * @throws NamingException if there is a problem with the connection
   */
  public void testConnection(LdapConnection conn) throws NamingException {
    newLdapQuery(restorePassword(conn), null).testConnection();
  }

  /**
   * Tests the given user mapping by querying for matching users.
   * 
   * @throws NamingException if there is a problem with the mapping
   */
  public List<LdapUser> testUserMapping(LdapUserMapping umap, long maxResults) throws NamingException {
    return newLdapQuery(umap).getUsers(maxResults);
  }

  /**
   * Tests the given user mapping by attempting to authenticate the given credentials.
   * 
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public void testUserLogin(LdapUserMapping umap, String username, char[] password) throws NamingException {
    newLdapQuery(umap).authenticateUser(username, password, false);
  }

  /**
   * Retrieve users from list of LdapUsers from an array of names which map to the UserID attribute
   *
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public List<LdapUser> getUsersByName(LdapServer ldapServer, String[] usernames) throws NamingException {
    return newLdapQuery(ldapServer).getUsersByName(usernames);
  }

  public List<LdapGroup> getGroupsByName(LdapServer ldapServer, String[] groupNames) throws NamingException {
    return newLdapQuery(ldapServer).getGroupsByName(groupNames);
  }

  /**
   * Find a list of users, searching the displayName attribute.
   * 
   * @param ldapServer The LDAP server to query
   * @param query String to match against
   * @param maxResults Limit on the number of results to return
   * @return List of LdapUser objects that match the search criteria
   * @throws NamingException if there is a problem with the mapping or the credentials
   */
  public List<LdapUser> findUsersByName(LdapServer ldapServer, String query, long maxResults) throws NamingException {
    return newLdapQuery(ldapServer).findUsersByName(query, maxResults);
  }

  /**
   * Retrieve all users that are members of the specified group.
   *
   * @throws NamingException if there is a problem with the mapping or the credentials
   * 
   * @since 1.14.0
   */
  public List<LdapUser> getUsersByGroup(LdapServer ldapServer, String groupName) throws NamingException {
    return newLdapQuery(ldapServer).getUsersByGroup(groupName);
  }

  /**
   * Find a list of groups, searching the Group ID attribute.
   *
   * @param ldapServer The ldap server to query against
   * @param query       String to match against
   * @param maxResults Limit on the number of results to return
   * @return List of LdapGroup objects that match the search criteria
   */
  public List<LdapGroup> findGroupsByName(LdapServer ldapServer, String query, long maxResults) throws NamingException {
    return newLdapQuery(ldapServer).findGroupsByName(query, maxResults);
  }

  // User authentication
  /**
   * Determines if the ldapServer is enabled by checking whether it has LdapConnection and LdapUserMapping setup
   */
  public boolean isLdapEnabled(LdapServer ldapServer) {
    return connDao.getByServerId(ldapServer.getId()) != null && userDao.getByServerId(ldapServer.getId()) != null;
  }
  
  /**
   * Indicates whether the ldapServer instance can be searched for groups.
   */
  public boolean isGroupSearchEnabled(LdapServer ldapServer) {
    if (isLdapEnabled(ldapServer)) {
      LdapUserMapping mapping = userDao.getByServerId(ldapServer.getId());
      if (mapping != null && mapping.getGroupMappingType() != LdapGroupMappingType.NONE) {
        return mapping.getGroupMappingType() != LdapGroupMappingType.DYNAMIC || mapping.isDynamicGroupSearchEnabled();
      }
    }
    return false;
  }

  /**
   * Gets whether the search of dynamic groups is disabled. (Not an inverse of isGroupSearchEnabled, used by the UI
   * determine whether it should allow manually adding groups)
   */
  public boolean isDynamicGroupSearchDisabled() {
    for (final LdapServer ldapServer : serverDao.getAll()) {
      if (isLdapEnabled(ldapServer)) {
        LdapUserMapping mapping = userDao.getByServerId(ldapServer.getId());
        if (mapping.getGroupMappingType() == LdapGroupMappingType.DYNAMIC && !mapping.isDynamicGroupSearchEnabled()) {
          return true;
        }
      }
    }
    return false;
  }

  private LdapUserMapping getUserMapping(LdapConnection connection) {
    LdapUserMapping umap = userDao.getByServerId(connection.getServerId());
    if (umap == null) {
      throw new IllegalStateException("LDAP user mapping is not configured");
    }
    return umap;
  }

  /**
   * Slower authentication check; short-circuits if connection or mapping are missing.
   * 
   * @see LdapRealm#queryForAuthenticationInfo
   */
  public LdapUser authenticateUser(String username, char[] password) throws NamingException {
    final List<LdapServer> servers = serverDao.getAll();

    final List<LdapServerExceptionWrapper> ldapServerExceptionWrappers = new ArrayList<>();

    for (final LdapServer server : servers) {
      final LdapConnection conn = getDecryptedConnection(server);
      checkValidConnection(conn);
      try {
        LdapUser user = newLdapQuery(conn).authenticateUser(username, password, true);
        resetConnectionFailures(conn);
        return user;
      }
      // NameNotFoundException means unknown user, NamingSecurityException means bad password.
      catch (NameNotFoundException | NamingSecurityException e) {
        ldapServerExceptionWrappers.add(new LdapServerExceptionWrapper(server, e));
      }
      catch (NamingException e) {
        recordConnectionFailure(conn);
        ldapServerExceptionWrappers.add(new LdapServerExceptionWrapper(server, e));
      }
    }

    throw LdapServerExceptionWrapper.getSomethingToThrow(ldapServerExceptionWrappers);
  }

  /**
   * Loads the user along with its group memberships, thereby verifying its general existence, to support integration
   * with 3rd-party SSO frontends that handle authentication and then forward the validated username (and only that).
   */
  public LdapUser getUserByName(String username) throws NamingException {
    final List<LdapServer> servers = serverDao.getAll();
    final List<LdapServerExceptionWrapper> ldapServerExceptionWrappers = new ArrayList<>();

    for (final LdapServer server : servers) {
      try {
        LdapUser ldapUser = getUserByName(server, username);
        if (ldapUser != null) {
          return ldapUser;
        }
      }
      catch (NamingException e) {
        ldapServerExceptionWrappers.add(new LdapServerExceptionWrapper(server, e));
      }
    }

    throw LdapServerExceptionWrapper.getSomethingToThrow(ldapServerExceptionWrappers);
  }

  public LdapUser getUserByName(LdapServer ldapServer, String username) throws NamingException {
    if (!isLdapEnabled(ldapServer)) {
      return null;
    }

    final LdapConnection conn = getDecryptedConnection(ldapServer);
    checkValidConnection(conn);
    try {
      LdapUser user = newLdapQuery(conn).getUser(username, true);
      resetConnectionFailures(conn);
      return user;
    }
    catch (NameNotFoundException e) {
      throw e;
    }
    catch (NamingException e) {
      recordConnectionFailure(conn);
      throw e;
    }
  }

  private LdapQuery newLdapQuery(LdapServer server) {
    return newLdapQuery(getDecryptedConnection(server));
  }

  private LdapQuery newLdapQuery(LdapConnection connection) {
    return newLdapQuery(connection, getUserMapping(connection));
  }

  private LdapQuery newLdapQuery(LdapUserMapping userMapping) {
    LdapServer ldapServer = serverDao.getByIdNotNull(userMapping.getServerId());
    return newLdapQuery(getDecryptedConnection(ldapServer), userMapping);
  }

  private LdapQuery newLdapQuery(LdapConnection connection, LdapUserMapping userMapping) {
    return new LdapQuery(connection, userMapping);
  }

  /**
   * Returns the current stored connection details with the password decrypted for the specified LDAP server.
   */
  private LdapConnection getDecryptedConnection(LdapServer ldapServer) {
    LdapConnection conn = connDao.getByServerId(ldapServer.getId());
    if (conn == null) {
      throw new IllegalStateException(
          "LDAP connection is not configured for LDAP server " + ldapServer.getName() + ".");
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

  private static class FailureInfo
  {
    private long lastFailureMillis = 0;

    private int connectionFailures = 0;
  }

  private final ConcurrentMap<String, FailureInfo> failureInfoMap = new ConcurrentHashMap<>();

  /**
   * Checks failure rate of LDAP connection; throws exception while retry delay is in effect.
   */
  private void checkValidConnection(LdapConnection conn) throws NamingException {
    final FailureInfo failureInfo = failureInfoMap.get(conn.getId());
    if (failureInfo != null) {
      if (failureInfo.lastFailureMillis > 0) {
        if (failureInfo.lastFailureMillis + (conn.getRetryDelay() * 1000) < System.currentTimeMillis()) {
          resetConnectionFailures(conn); // retry delay has elapsed
        }
        else if (failureInfo.connectionFailures >= 3) {
          throw new CommunicationException("Delaying retry of failing LDAP connection.");
        }
      }
    }
  }

  private synchronized void recordConnectionFailure(final LdapConnection conn) {
    final String connId = conn.getId();
    failureInfoMap.putIfAbsent(connId, new FailureInfo());
    final FailureInfo failureInfo = failureInfoMap.get(connId);
    failureInfo.lastFailureMillis = System.currentTimeMillis();
    failureInfo.connectionFailures++;
  }

  private void resetConnectionFailures(final LdapConnection conn) {
    final String connId = conn.getId();
    if (connId != null) {
      failureInfoMap.remove(connId);
    }
  }
}
