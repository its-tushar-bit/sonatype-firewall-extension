/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.authn.AnonymousAuthenticator;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.authn.SimpleAuthenticator;
import org.apache.directory.server.core.authn.StrongAuthenticator;
import org.apache.directory.server.core.factory.AvlPartitionFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.codehaus.plexus.util.FileUtils;

/**
 * Embedded LDAP server meant to facilitate unit testing of LDAP integration.
 * 
 * @since 1.7
 */
public class EmbeddedLdapServer
{
  private static final String LOCALHOST = "localhost";

  private File workingDirectory;

  private DefaultDirectoryService directoryService;

  private LdapServer ldapServer;

  private int port;

  private AuthenticationLevel authLevel = AuthenticationLevel.NONE;

  private Map<String, MechanismHandler> saslHandlers = new HashMap<String, MechanismHandler>();

  private File ldapsKeystore;

  private String ldapsKeystorePassword;

  /**
   * @since 1.7
   */
  public EmbeddedLdapServer(File workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  /**
   * @since 1.7
   */
  public void start() throws Exception {
    if (port <= 0) {
      port = getRandomPort();
    }

    // an example that shows how to create and configure embedded apacheds instance
    // http://svn.apache.org/repos/asf/directory/apacheds/trunk/core-annotations/src/main/java/org/apache/directory/server/core/factory/DefaultDirectoryServiceFactory.java

    directoryService = new DefaultDirectoryService();

    directoryService.setInstanceLayout(new InstanceLayout(workingDirectory));

    SchemaManager schemaManager = new DefaultSchemaManager();
    directoryService.setSchemaManager(schemaManager);

    initPartitions(directoryService);

    ldapServer = new LdapServer();

    Transport transport = new TcpTransport(LOCALHOST, port);
    transport.setEnableSSL(ldapsKeystore != null);
    ldapServer.setTransports(transport);
    if (ldapsKeystore != null) {
      ldapServer.setKeystoreFile(ldapsKeystore.getCanonicalPath());
    }
    if (ldapsKeystorePassword != null) {
      ldapServer.setCertificatePassword(ldapsKeystorePassword);
    }

    ldapServer.setDirectoryService(directoryService);

    // allowed authentication mechanisms
    Authenticator[] authenticators;
    switch (authLevel) {
      case SIMPLE:
        authenticators = new Authenticator[] { new SimpleAuthenticator() };
        break;
      case STRONG:
        authenticators = new Authenticator[] { new StrongAuthenticator() };
        ldapServer.setSaslMechanismHandlers(saslHandlers);
        ldapServer.setSaslHost(LOCALHOST);
        ldapServer.setSaslRealms(Arrays.asList(getSaslRealm()));
        ldapServer.setSearchBaseDn("ou=system");
        break;
      case NONE:
      default:
        directoryService.setAllowAnonymousAccess(true);
        authenticators = new Authenticator[] { new AnonymousAuthenticator(), new SimpleAuthenticator() };
        break;
    }
    AuthenticationInterceptor auth = (AuthenticationInterceptor) directoryService
        .getInterceptor(InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName());
    auth.setAuthenticators(authenticators);

    directoryService.startup();
    ldapServer.start();

    loadUsers(workingDirectory, directoryService);
  }

  private static void initPartitions(DefaultDirectoryService directoryService) throws Exception {
    LdifPartition ldifPartition = new LdifPartition(directoryService.getSchemaManager());
    ldifPartition.setPartitionPath(new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "schema")
        .toURI());
    SchemaPartition schemaPartition = new SchemaPartition(directoryService.getSchemaManager());
    schemaPartition.setWrappedPartition(ldifPartition);
    directoryService.setSchemaPartition(schemaPartition);
    PartitionFactory partitionFactory = new AvlPartitionFactory();

    Partition systemPartition = partitionFactory.createPartition(directoryService.getSchemaManager(), "system",
        ServerDNConstants.SYSTEM_DN, 500, new File(directoryService.getInstanceLayout().getPartitionsDirectory(),
            "system"));
    systemPartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.setSystemPartition(systemPartition);

    Partition groupsPartition = partitionFactory.createPartition(directoryService.getSchemaManager(), "groups",
        "ou=groups,dc=company,dc=com", 500, new File(directoryService.getInstanceLayout().getPartitionsDirectory(),
            "groups"));
    groupsPartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(groupsPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.addPartition(groupsPartition);

    Partition usersPartition = partitionFactory.createPartition(directoryService.getSchemaManager(), "users",
        "ou=users,dc=company,dc=com", 500, new File(directoryService.getInstanceLayout().getPartitionsDirectory(),
            "users"));
    usersPartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(usersPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.addPartition(usersPartition);
  }

  private static void loadUsers(File workingDirectory, DirectoryService directoryService) throws IOException {
    File usersLdif = new File(workingDirectory, "ldap_users.ldif");
    FileUtils.copyURLToFile(EmbeddedLdapServer.class.getResource("/ldap_users.ldif"), usersLdif);
    new LdifFileLoader(directoryService.getAdminSession(), usersLdif.getAbsolutePath()).execute();
  }

  private static int getRandomPort() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    try {
      return socket.getLocalPort();
    }
    finally {
      socket.close();
    }
  }

  /**
   * @since 1.7
   */
  public void stop() throws Exception {
    ldapServer.stop();
    directoryService.shutdown();
    port = 0;
  }

  /**
   * @since 1.7
   */
  public String getUrl() {
    StringBuilder sb = new StringBuilder();
    sb.append(ldapsKeystore != null ? "ldaps" : "ldap");
    sb.append("://" + LOCALHOST + ":");
    sb.append(port);
    return sb.toString();
  }

  /**
   * @since 1.7
   */
  public String getHostname() {
    return LOCALHOST;
  }

  /**
   * @since 1.7
   */
  public int getPort() {
    return port;
  }

  /**
   * @since 1.7
   */
  public String getSystemUserDN() {
    return "uid=admin,ou=system";
  }

  /**
   * @since 1.7
   */
  public String getSystemUser() {
    return "admin";
  }

  /**
   * @since 1.7
   */
  public String getSystemUserPassword() {
    return "secret";
  }

  /**
   * @since 1.7
   */
  public void setAuthenticationSimple() {
    authLevel = AuthenticationLevel.SIMPLE;
  }

  /**
   * @since 1.7
   */
  public void setAuthenticationSasl(String mechanism) {
    authLevel = AuthenticationLevel.STRONG;
    if (SupportedSaslMechanisms.DIGEST_MD5.equals(mechanism)) {
      saslHandlers = Collections.singletonMap(mechanism, (MechanismHandler) new DigestMd5MechanismHandler());
    }
    else if (SupportedSaslMechanisms.CRAM_MD5.equals(mechanism)) {
      saslHandlers = Collections.singletonMap(mechanism, (MechanismHandler) new CramMd5MechanismHandler());
    }
  }

  /**
   * @since 1.7
   */
  public String getSaslRealm() {
    return LOCALHOST;
  }

  public void enableLdaps(File keystore, String keystorePassword) {
    this.ldapsKeystore = keystore;
    this.ldapsKeystorePassword = keystorePassword;
  }

  /**
   * Creates new LDAP server instance with conventional work directory target/apacheds
   * 
   * @since 1.7
   */
  public static EmbeddedLdapServer newEmbeddedLdapServer() throws IOException {
    File workingDirectory = new File("target/apacheds");
    FileUtils.deleteDirectory(workingDirectory);
    return new EmbeddedLdapServer(workingDirectory);
  }

  // this method is meant to help test this test harness, it is not part of API, do not use
  public static void main(String[] args) throws Exception {
    File workingDirectory = new File("target/apacheds");
    FileUtils.deleteDirectory(workingDirectory);
    EmbeddedLdapServer server = new EmbeddedLdapServer(workingDirectory);
    server.enableLdaps(new File("src/test/resources/keystore/insight-test.ks"), "secret");
    server.start();
  }
}
