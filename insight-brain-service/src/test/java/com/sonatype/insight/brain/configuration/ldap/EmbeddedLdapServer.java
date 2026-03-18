/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.test.networking.PortAllocator;
import com.sonatype.insight.test.networking.SslProperties;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded LDAP server meant to facilitate unit testing of LDAP integration. For convenience, use the
 * TestLdapServer subclass in tests.
 *
 * @since 1.7
 */
public class EmbeddedLdapServer
{
  private static final Logger log = LoggerFactory.getLogger(EmbeddedLdapServer.class);

  private static final String LOCALHOST = "localhost";

  private File workingDirectory = new File("target/apacheds", UUID.randomUUID().toString()).getAbsoluteFile();

  private DefaultDirectoryService directoryService;

  private LdapServer ldapServer;

  private int port;

  private AuthenticationLevel authLevel = AuthenticationLevel.NONE;

  private Map<String, MechanismHandler> saslHandlers = new HashMap<>();

  private File ldapsKeystore;

  private String ldapsKeystorePassword;

  private boolean running = false;

  private String ldifResourceName;

  public EmbeddedLdapServer setWorkingDirectory(File workingDirectory) {
    this.workingDirectory = workingDirectory.getAbsoluteFile();
    return this;
  }

  public EmbeddedLdapServer setLdifResourceName(String ldifResourceName) {
    this.ldifResourceName = ldifResourceName;
    return this;
  }

  /**
   * @since 1.7
   */
  public void start() throws Exception {
    if (running) {
      throw new IllegalStateException("The EmbeddedLdapServer is already running");
    }

    long start = System.currentTimeMillis();

    if (port <= 0) {
      port = PortAllocator.nextFreePort();
    }

    log.debug("Starting EmbeddedLdapServer with working directory {} and LDIF {} on port {}", workingDirectory,
        ldifResourceName, port);

    // an example that shows how to create and configure embedded apacheds instance
    // http://svn.apache.org/repos/asf/directory/apacheds/trunk/core-annotations/
    // src/main/java/org/apache/directory/server/core/factory/DefaultDirectoryServiceFactory.java

    directoryService = new DefaultDirectoryService();
    directoryService.setShutdownHookEnabled(false); // avoid memory leak
    directoryService.setInstanceLayout(new InstanceLayout(workingDirectory));

    SchemaManager schemaManager = new DefaultSchemaManager();
    directoryService.setSchemaManager(schemaManager);

    schemaManager.enable("nis"); // required by group mapping tests

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
        authenticators = new Authenticator[]{new SimpleAuthenticator()};
        break;
      case STRONG:
        authenticators = new Authenticator[]{new StrongAuthenticator()};
        ldapServer.setSaslMechanismHandlers(saslHandlers);
        ldapServer.setSaslHost(LOCALHOST);
        ldapServer.setSaslRealms(Collections.singletonList(getSaslRealm()));
        ldapServer.setSearchBaseDn("ou=system");
        break;
      case NONE:
      default:
        directoryService.setAllowAnonymousAccess(true);
        authenticators = new Authenticator[]{new AnonymousAuthenticator(), new SimpleAuthenticator()};
        break;
    }
    AuthenticationInterceptor auth = (AuthenticationInterceptor) directoryService
        .getInterceptor(InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName());
    auth.setAuthenticators(authenticators);

    directoryService.startup();
    ldapServer.start();

    running = true;

    log.debug("Started EmbeddedLdapServer in {} ms", System.currentTimeMillis() - start);

    if (ldifResourceName != null) {
      loadData(ldifResourceName);
    }
  }

  public void loadData(String ldifResourceName) throws IOException {
    File ldif = new File(workingDirectory, "data" + ldifResourceName);
    FileUtils.copyURLToFile(EmbeddedLdapServer.class.getResource(ldifResourceName), ldif);
    new LdifFileLoader(directoryService.getAdminSession(), ldif.getAbsolutePath()).execute();
  }

  private static void initPartitions(DefaultDirectoryService directoryService) throws Exception {
    LdifPartition ldifPartition = new LdifPartition(directoryService.getSchemaManager(),
        directoryService.getDnFactory());
    ldifPartition
        .setPartitionPath(new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "schema").toURI());
    SchemaPartition schemaPartition = new SchemaPartition(directoryService.getSchemaManager());
    schemaPartition.setWrappedPartition(ldifPartition);
    directoryService.setSchemaPartition(schemaPartition);
    PartitionFactory partitionFactory = new AvlPartitionFactory();

    Partition systemPartition = partitionFactory.createPartition(directoryService.getSchemaManager(),
        directoryService.getDnFactory(), "system", ServerDNConstants.SYSTEM_DN, 500,
        new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "system"));
    systemPartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.setSystemPartition(systemPartition);

    Partition sonatypePartition = partitionFactory.createPartition(directoryService.getSchemaManager(),
        directoryService.getDnFactory(), "sonatype", "o=sonatype", 500,
        new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "sonatype"));
    sonatypePartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(sonatypePartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.addPartition(sonatypePartition);

    Partition groupsPartition = partitionFactory.createPartition(directoryService.getSchemaManager(),
        directoryService.getDnFactory(), "groups", "ou=groups,dc=company,dc=com", 500,
        new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "groups"));
    groupsPartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(groupsPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.addPartition(groupsPartition);

    Partition usersPartition = partitionFactory.createPartition(directoryService.getSchemaManager(),
        directoryService.getDnFactory(), "users", "ou=users,dc=company,dc=com", 500,
        new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "users"));
    usersPartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(usersPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.addPartition(usersPartition);

    Partition acmeBrickPartition = partitionFactory.createPartition(directoryService.getSchemaManager(),
        directoryService.getDnFactory(), "acme_brick", "dc=acme brick,dc=com", 500,
        new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "acme_brick"));
    acmeBrickPartition.setSchemaManager(directoryService.getSchemaManager());
    partitionFactory.addIndex(acmeBrickPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
    directoryService.addPartition(acmeBrickPartition);
  }

  /**
   * @since 1.7
   */
  public void stop() throws Exception {
    if (!running) {
      return;
    }

    long start = System.currentTimeMillis();

    ldapServer.stop();
    directoryService.shutdown();
    port = 0;
    running = false;

    try {
      new FileCleaner().delete(workingDirectory);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    log.debug("Stopped EmbeddedLdapServer in {} ms", System.currentTimeMillis() - start);
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

  public void setPort(int port) {
    this.port = port;
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
  public char[] getSystemUserPassword() {
    return "secret".toCharArray();
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

  // this method is meant to help test this test harness, it is not part of API, do not use
  public static void main(String[] args) throws Exception {
    File workingDirectory = new File("target/apacheds");
    new FileCleaner().delete(workingDirectory);
    EmbeddedLdapServer testLdapServer = new EmbeddedLdapServer().setWorkingDirectory(workingDirectory);
    testLdapServer.enableLdaps(SslProperties.SERVER_STORE_FILE, SslProperties.KEY_STORE_PASSWORD);
    testLdapServer.start();
  }
}
