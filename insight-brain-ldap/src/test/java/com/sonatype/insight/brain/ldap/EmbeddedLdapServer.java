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
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.codehaus.plexus.util.FileUtils;

/**
 * Embedded LDAP server meant to facilitate unit testing of LDAP integration.
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

  public EmbeddedLdapServer(File workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  public void start() throws Exception {
    if (port <= 0) {
      port = getRandomPort();
    }

    // http://svn.apache.org/repos/asf/directory/apacheds/trunk/core-annotations/src/main/java/org/apache/directory/server/core/factory/DefaultDirectoryServiceFactory.java

    directoryService = new DefaultDirectoryService();

    directoryService.setInstanceLayout(new InstanceLayout(workingDirectory));

    SchemaManager schemaManager = new DefaultSchemaManager();
    directoryService.setSchemaManager(schemaManager);

    // initSchema(directoryService);
    initSystemPartition(directoryService);

    ldapServer = new LdapServer();

    Transport transport = new TcpTransport(LOCALHOST, port);
    ldapServer.setTransports(transport);

    ldapServer.setDirectoryService(directoryService);

    // allowed authentication mechanisms
    Authenticator authenticator;
    switch (authLevel) {
      case SIMPLE:
        authenticator = new SimpleAuthenticator();
        break;
      case STRONG:
        authenticator = new StrongAuthenticator();
        ldapServer.setSaslMechanismHandlers(saslHandlers);
        ldapServer.setSaslHost(LOCALHOST);
        ldapServer.setSaslRealms(Arrays.asList(getSaslRealm()));
        ldapServer.setSearchBaseDn("ou=system");
        break;
      case NONE:
      default:
        directoryService.setAllowAnonymousAccess(true);
        authenticator = new AnonymousAuthenticator();
        break;
    }
    AuthenticationInterceptor auth = (AuthenticationInterceptor) directoryService
        .getInterceptor(InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName());
    auth.setAuthenticators(new Authenticator[] { authenticator });

    directoryService.startup();
    ldapServer.start();
  }

  private static void initSystemPartition(DefaultDirectoryService directoryService) throws Exception {
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

  public void stop() throws Exception {
    ldapServer.stop();
    directoryService.shutdown();
  }

  public String getUrl() {
    StringBuilder sb = new StringBuilder();
    sb.append("ldap://localhost:" + port);
    return sb.toString();
  }

  public int getPort() {
    return port;
  }

  public String getSystemUserDN() {
    return "uid=admin,ou=system";
  }

  public String getSystemUser() {
    return "admin";
  }

  public String getSystemUserPassword() {
    return "secret";
  }

  public void setAuthenticationSimple() {
    authLevel = AuthenticationLevel.SIMPLE;
  }

  public void setAuthenticationSasl(String mechanism) {
    authLevel = AuthenticationLevel.STRONG;
    if (SupportedSaslMechanisms.DIGEST_MD5.equals(mechanism)) {
      saslHandlers = Collections.singletonMap(mechanism, (MechanismHandler) new DigestMd5MechanismHandler());
    }
    else if (SupportedSaslMechanisms.CRAM_MD5.equals(mechanism)) {
      saslHandlers = Collections.singletonMap(mechanism, (MechanismHandler) new CramMd5MechanismHandler());
    }
  }

  public static void main(String[] args) throws Exception {
    File workingDirectory = new File("target/apacheds");
    FileUtils.deleteDirectory(workingDirectory);
    EmbeddedLdapServer server = new EmbeddedLdapServer(workingDirectory);
    server.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    server.start();
  }

  public String getSaslRealm() {
    return LOCALHOST;
  }

  /**
   * Creates new LDAP server instance with conventional work directory target/apacheds
   */
  public static EmbeddedLdapServer newEmbeddedLdapServer() throws IOException {
    File workingDirectory = new File("target/apacheds");
    FileUtils.deleteDirectory(workingDirectory);
    return new EmbeddedLdapServer(workingDirectory);
  }

}
