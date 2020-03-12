/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NamingSecurityException;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.configuration.ldap.LdapConnectionStatus.Status;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.HasLdapServerId;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages LDAP information.
 * 
 * @since 1.7
 */
@Named
@Singleton
public class LdapService
{
  private static final Logger log = LoggerFactory.getLogger(LdapService.class);

  public static final char[] FAKE_PASSWORD = "#~FAKE~PASSWORD~#".toCharArray();

  private final LdapServerDAO serverDao = new LdapServerDAO();

  private final LdapConnectionDAO connDao = new LdapConnectionDAO();

  private final LdapUserMappingDAO userDao = new LdapUserMappingDAO();

  private final PasswordHandler passwordHandler;

  @Inject
  public LdapService(PasswordHandler passwordHandler) {
    this.passwordHandler = passwordHandler;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  LdapServer addLdapServer(LdapServer ldapServer) {
    serverDao.insert(ldapServer);
    auditLdapServer(ldapServer);
    return ldapServer;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  LdapServer updateLdapServer(LdapServer ldapServer) {
    serverDao.update(ldapServer);
    auditLdapServer(ldapServer);
    return ldapServer;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  void deleteLdapServer(String ldapServerId) {
    LdapServer ldapServer = serverDao.getByIdNotNull(ldapServerId);
    serverDao.delete(ldapServer);
    auditLdapServer(ldapServer);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  List<LdapServer> getAllLdapServers() {
    return serverDao.getAll();
  }

  /**
   * Loads the LDAP connection from the datastore, hiding any cached password.
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LdapConnection getLdapConnection(String ldapServerId) {
    LdapConnection conn = connDao.getByServerId(ldapServerId);
    if (conn == null) {
      conn = new LdapConnection();
      conn.setServerId(ldapServerId);
    }
    return fakeOutPassword(conn);
  }

  /**
   * Saves the LDAP connection to the datastore, encrypting any new password.
   */
  public LdapConnection upsertLdapConnection(LdapConnection ldapConnection) {
    LdapConnection encrypted = encryptPassword(ldapConnection);
    if (encrypted.getId() != null) {
      connDao.update(encrypted);
    }
    else {
      connDao.insert(encrypted);
    }
    resetConnectionFailures(ldapConnection);
    return fakeOutPassword(encrypted);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  LdapConnection upsertLdapConnection(String ldapServerId, LdapConnection ldapConnection) {
    validateServerId(ldapServerId, ldapConnection);
    ldapConnection = upsertLdapConnection(ldapConnection);
    auditLdapConnection(ldapConnection);
    return ldapConnection;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  LdapUserMapping getLdapUserMapping(String ldapServerId) {
    LdapUserMapping ldapUserMapping = userDao.getByServerId(ldapServerId);
    if (ldapUserMapping == null) {
      ldapUserMapping = new LdapUserMapping();
      ldapUserMapping.setServerId(ldapServerId);
    }
    return ldapUserMapping;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  LdapUserMapping upsertLdapUserMapping(String ldapServerId, LdapUserMapping ldapUserMapping) {
    validateServerId(ldapServerId, ldapUserMapping);

    if (ldapUserMapping.getId() != null) {
      userDao.update(ldapUserMapping);
    }
    else {
      userDao.insert(ldapUserMapping);
    }
    auditLdapUserMapping(ldapUserMapping);
    return ldapUserMapping;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  LdapConnectionStatus testLdapConnection(String ldapServerId, LdapConnection ldapConnection) {
    validateServerId(ldapServerId, ldapConnection);

    try {
      newLdapQuery(restorePassword(ldapConnection), null).testConnection();
      return LdapConnectionStatus.SUCCESS;
    }
    catch (NamingException e) {
      // Log the exception at debug level for customer and Sonatype support investigations
      // (see https://issues.sonatype.org/browse/CLM-13799)
      log.debug("LDAP connection test failed", e);
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
  }

  /**
   * Tests the given user mapping by querying for matching users.
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  List<LdapUser> testLdapUserMapping(String ldapServerId, LdapUserMapping ldapUserMapping, long maxResults) {
    validateServerId(ldapServerId, ldapUserMapping);

    try {
      return newLdapQuery(ldapUserMapping).getUsers(maxResults);
    }
    catch (IllegalStateException e) {
      // happens when ldap server connection is not configured
      throw new BadRequestException(e);
    }
    catch (NamingException e) {
      throw new BadRequestException(e);
    }
  }

  /**
   * Tests the given user mapping by attempting to authenticate the given credentials.
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  LdapConnectionStatus testUserLogin(
      String ldapServerId,
      LdapUserMapping ldapUserMapping,
      String username,
      char[] password)
  {
    validateServerId(ldapServerId, ldapUserMapping);

    try {
      newLdapQuery(ldapUserMapping).authenticateUser(username, password, false);
      return LdapConnectionStatus.SUCCESS;
    }
    catch (IllegalStateException e) {
      // happens when ldap server connection is not configured
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
    catch (NamingException e) {
      // Log the exception at debug level for customer and Sonatype support investigations
      // (see https://issues.sonatype.org/browse/CLM-13799)
      log.debug("LDAP login test failed", e);
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
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
    conn.setSystemPassword(passwordHandler.decryptPassword(conn.getSystemPassword()));
    return conn;
  }
  
  /**
   * Returns a copy of the given connection for clients with the password faked-out.
   */
  private static LdapConnection fakeOutPassword(LdapConnection conn) {
    if (conn.getSystemPassword() != null && conn.getSystemPassword().length > 0) {
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
    if (Arrays.equals(FAKE_PASSWORD, conn.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(conn);
      char[] encryptedPassword = connDao.getByIdNotNull(conn.getId()).getSystemPassword();
      copy.setSystemPassword(passwordHandler.decryptPassword(encryptedPassword));
      return copy;
    }
    return conn;
  }

  /**
   * Returns a copy of the given connection for storage with the password encrypted.
   */
  private LdapConnection encryptPassword(LdapConnection conn) {
    LdapConnection copy = new LdapConnection(conn);
    if (Arrays.equals(FAKE_PASSWORD, conn.getSystemPassword())) {
      copy.setSystemPassword(connDao.getByIdNotNull(conn.getId()).getSystemPassword());
    }
    else {
      copy.setSystemPassword(passwordHandler.encryptPassword(conn.getSystemPassword()));
    }
    return copy;
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

  private void validateServerId(String serverId, HasLdapServerId entity) {
    if (serverId == null || entity == null || !serverId.equals(entity.getServerId())) {
      throw new BadRequestException("Inconsistent LDAP server ID.");
    }
  }

  private void auditLdapServer(LdapServer ldapServer) {
    AuditData.get().setData("ldapServerId", ldapServer.getId()).setData("ldapServerName", ldapServer.getName());
  }

  private void auditLdapConnection(LdapConnection ldapConnection) {
    auditLdapServer(serverDao.getByIdNotNull(ldapConnection.getServerId()));
    AuditData.get().setData("ldapProtocol", ldapConnection.getProtocol().getProtocol()) //
        .setData("ldapHostname", ldapConnection.getHostname()) //
        .setData("ldapPort", ldapConnection.getPort()) //
        .setData("ldapSearchBaseDn", ldapConnection.getSearchBase()) //
        .setData("ldapAuthenticationMethod",
            ldapConnection.getAuthenticationMethod().getMethod().toLowerCase(Locale.ROOT)) //
        .setData("ldapSaslRealm", ldapConnection.getSaslRealm()) //
        .setData("ldapUsername", ldapConnection.getSystemUsername()) //
        .setData("ldapConnectionTimeoutInSeconds", ldapConnection.getConnectionTimeout()) //
        .setData("ldapRetryDelayInSeconds", ldapConnection.getRetryDelay()); //
  }

  private void auditLdapUserMapping(LdapUserMapping ldapUserMapping) {
    auditLdapServer(serverDao.getByIdNotNull(ldapUserMapping.getServerId()));
    AuditData.get().setData("ldapUserBaseDn", ldapUserMapping.getUserBaseDN())
        .setData("ldapUserSubtree", ldapUserMapping.isUserSubtree() ? "enabled" : "disabled")
        .setData("ldapUserObjectClass", ldapUserMapping.getUserObjectClass())
        .setData("ldapUserFilter", ldapUserMapping.getUserFilter())
        .setData("ldapUserIdAttribute", ldapUserMapping.getUserIDAttribute())
        .setData("ldapUserRealNameAttribute", ldapUserMapping.getUserRealNameAttribute())
        .setData("ldapUserEmailAttribute", ldapUserMapping.getUserEmailAttribute())
        .setData("ldapUserPasswordAttribute", ldapUserMapping.getUserPasswordAttribute())
        .setEnum("ldapGroupType", ldapUserMapping.getGroupMappingType());
    if (ldapUserMapping.getGroupMappingType().equals(LdapGroupMappingType.STATIC)) {
      AuditData.get().setData("ldapStaticGroupBaseDn", ldapUserMapping.getGroupBaseDN())
          .setData("ldapStaticGroupSubtree", ldapUserMapping.isGroupSubtree() ? "enabled" : "disabled")
          .setData("ldapStaticGroupObjectClass", ldapUserMapping.getGroupObjectClass())
          .setData("ldapStaticGroupIdAttribute", ldapUserMapping.getGroupIDAttribute())
          .setData("ldapStaticGroupMemberAttribute", ldapUserMapping.getGroupMemberAttribute())
          .setData("ldapStaticGroupMemberFormat", ldapUserMapping.getGroupMemberFormat());
    }
    else if (ldapUserMapping.getGroupMappingType().equals(LdapGroupMappingType.DYNAMIC)) {
      AuditData.get().setData("ldapDynamicGroupMemberOfAttribute", ldapUserMapping.getUserMemberOfGroupAttribute())
          .setData("ldapDynamicGroupSearch", ldapUserMapping.isDynamicGroupSearchEnabled() ? "enabled" : "disabled");
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  void updatePriority(List<String> ldapServerIds) {
    List<LdapServerDTO> ldapServerList = ldapServerIds.stream()
        .map(ldapServerId -> new LdapServerDTO(serverDao.getByIdNotNull(ldapServerId))).collect(Collectors.toList());
    serverDao.updatePriority(ldapServerIds);
    AuditData.get().setData("ldapServerOrder", ldapServerList);
  }
}
