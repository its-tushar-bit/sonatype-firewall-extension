/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.service.InsightConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

public class DropwizardServerCompatibilityTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldTranslateApplicationAndAdminConnectorsIntoSpringPorts() throws Exception {
    File config = tempFolder.newFile("config.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationContextPath: /iq\n" +
            "  applicationConnectors:\n" +
            "    - type: https\n" +
            "      port: 8072\n" +
            "      bindHost: 127.0.0.2\n" +
            "      keyStorePath: /tmp/application-keystore.jks\n" +
            "      keyStorePassword: secret\n" +
            "  adminConnectors:\n" +
            "    - type: https\n" +
            "      port: 8073\n" +
            "      bindHost: 127.0.0.3\n" +
            "      keyStorePath: /tmp/admin-keystore.jks\n" +
            "      keyStorePassword: adminsecret\n");

    StandardEnvironment environment = new StandardEnvironment();
    loadShippedApplicationDefaults(environment);

    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8072");
    assertThat(environment.getProperty("server.port")).isNotEqualTo("8070");
    assertThat(environment.getProperty("management.server.port")).isEqualTo("8073");
    assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo("/iq");
    assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.2");
    assertThat(environment.getProperty("management.server.address")).isEqualTo("127.0.0.3");
    assertThat(environment.getProperty("server.ssl.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("server.ssl.key-store")).isEqualTo("/tmp/application-keystore.jks");
    assertThat(environment.getProperty("management.server.ssl.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("management.server.ssl.key-store")).isEqualTo("/tmp/admin-keystore.jks");
  }

  @Test
  public void shouldApplyApplicationAndAdminConnectorIdleTimeouts() throws Exception {
    File config = tempFolder.newFile("idle-timeouts.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8072\n" +
            "      bindHost: 127.0.0.2\n" +
            "      idleTimeout: 30 minutes\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 8073\n" +
            "      bindHost: 127.0.0.3\n" +
            "      idleTimeout: 30m\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty(
        DropwizardConnectorSettings.APPLICATION_IDLE_TIMEOUT_PROPERTY, Duration.class))
            .isEqualTo(Duration.ofMinutes(30));
    assertThat(environment.getProperty(
        DropwizardConnectorSettings.ADMIN_IDLE_TIMEOUT_PROPERTY, Duration.class))
            .isEqualTo(Duration.ofMinutes(30));

    DropwizardConnectorSettings connectorSettings = DropwizardConnectorSettings.from(environment);
    assertConnectorIdleTimeout(
        new DropwizardConnectorConfiguration().dropwizardApplicationConnectorCustomizer(connectorSettings),
        Duration.ofMinutes(30));
    assertConnectorIdleTimeout(
        new DropwizardManagementConnectorConfiguration().dropwizardAdminConnectorCustomizer(connectorSettings),
        Duration.ofMinutes(30));
  }

  @Test
  public void shouldTranslatePortsFromPackagedNexusIqConfig() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    loadShippedApplicationDefaults(environment);

    new DropwizardConfigLoader().loadConfig(resolvePackagedNexusIqConfig().toFile(), environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8070");
    assertThat(environment.getProperty("management.server.port")).isEqualTo("8071");
    assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo("/");
    assertThat(environment.getProperty("management.server.address")).isEqualTo("127.0.0.1");
  }

  @Test
  public void shouldLeaveSpringPortsUnsetWhenConnectorListsAreMissingOrEmpty() throws Exception {
    File config = tempFolder.newFile("missing-connectors.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors: []\n");

    StandardEnvironment environment = new StandardEnvironment();

    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.port")).isNull();
    assertThat(environment.getProperty("management.server.port")).isNull();
  }

  @Test
  public void shouldTranslateLoggingLevelsFromPackagedNexusIqConfig() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    loadShippedApplicationDefaults(environment);

    new DropwizardConfigLoader().loadConfig(resolvePackagedNexusIqConfig().toFile(), environment);

    assertThat(environment.getProperty("logging.level.root")).isEqualTo("DEBUG");
    assertThat(environment.getProperty("logging.level.org.eclipse.jetty")).isEqualTo("INFO");
    assertThat(environment.containsProperty("logging.level.com.sonatype.insight.audit")).isFalse();
  }

  @Test
  public void shouldTranslateDropwizardLoggingLevelsIntoSpringLoggingLevels() throws Exception {
    File config = tempFolder.newFile("config.yml");
    Files.writeString(config.toPath(),
        "logging:\n" +
            "  level: INFO\n" +
            "  loggers:\n" +
            "    com.sonatype.insight: WARN\n" +
            "    com.sonatype.insight.audit:\n" +
            "      appenders:\n" +
            "        - type: mtiq-audit-log\n" +
            "          auditLogBasePath: ./sonatype-work/clm-cluster\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("logging.level.root")).isEqualTo("INFO");
    assertThat(environment.getProperty("logging.level.com.sonatype.insight")).isEqualTo("WARN");
    assertThat(environment.getProperty("auditLogBasePath")).isEqualTo("./sonatype-work/clm-cluster");
  }

  @Test
  public void shouldResolveEnvironmentVariablesBeforeExposingSpringProperties() throws Exception {
    String home = System.getenv("HOME");
    Assume.assumeTrue(home != null && !home.isEmpty());

    File config = tempFolder.newFile("env-config.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationContextPath: ${HOME}\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo(home);
  }

  @Test
  public void shouldResolveEnvironmentVariablesBeforeBindingInsightConfig() throws Exception {
    String home = System.getenv("HOME");
    Assume.assumeTrue(home != null && !home.isEmpty());

    File config = tempFolder.newFile("env-binding.yml");
    Files.writeString(config.toPath(),
        "hdsUrl: ${HOME}\n" +
            "database:\n" +
            "  type: postgresql\n" +
            "  hostname: ${HOME}\n" +
            "  port: 5432\n" +
            "  name: iq\n" +
            "  username: iq\n" +
            "  password: ${DOES_NOT_EXIST_39214}\n" +
            "  parameters:\n" +
            "    currentSchema: ${HOME}\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHdsUrl()).isEqualTo(home);
    assertThat(insightConfig.getDatabase().getHostname()).isEqualTo(home);
    assertThat(insightConfig.getDatabase().getParameters()).containsEntry("currentSchema", home);
    assertThat(insightConfig.getDatabase().getPassword()).isEqualTo("${DOES_NOT_EXIST_39214}");
  }

  @Test
  public void shouldMapKeyManagerPasswordToSpringKeyPasswordProperties() throws Exception {
    File config = tempFolder.newFile("https-passwords.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: https\n" +
            "      port: 8072\n" +
            "      keyStorePath: /tmp/application-keystore.jks\n" +
            "      keyStorePassword: store-secret\n" +
            "      keyManagerPassword: application-key-secret\n" +
            "  adminConnectors:\n" +
            "    - type: https\n" +
            "      port: 8073\n" +
            "      keyStorePath: /tmp/admin-keystore.jks\n" +
            "      keyStorePassword: admin-store-secret\n" +
            "      keyManagerPassword: admin-key-secret\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.ssl.key-store-password")).isEqualTo("store-secret");
    assertThat(environment.getProperty("server.ssl.key-password")).isEqualTo("application-key-secret");
    assertThat(environment.getProperty("management.server.ssl.key-store-password")).isEqualTo("admin-store-secret");
    assertThat(environment.getProperty("management.server.ssl.key-password")).isEqualTo("admin-key-secret");
  }

  @Test
  public void shouldStartHttpsAdditionalApplicationConnectorBackedByKeystore() throws Exception {
    String keyStorePath =
        new ClassPathResource("InsightBrainServiceTest/localhost.p12").getFile().getAbsolutePath();
    File config = tempFolder.newFile("http-plus-https-additional.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 18070\n" +
            "    - type: https\n" +
            "      port: 18443\n" +
            "      keyStorePath: " + keyStorePath + "\n" +
            "      keyStorePassword: changeit\n" +
            "      keyStoreType: PKCS12\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    DropwizardConnectorSettings connectorSettings = DropwizardConnectorSettings.from(environment);
    WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer =
        new DropwizardConnectorConfiguration().dropwizardApplicationConnectorCustomizer(connectorSettings);

    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(18070);
    customizer.customize(factory);

    WebServer webServer = factory.getWebServer(servletContext -> {
    });
    try {
      webServer.start();
      Server server = ((JettyWebServer) webServer).getServer();
      ServerConnector httpsConnector = Arrays.stream(server.getConnectors())
          .filter(ServerConnector.class::isInstance)
          .map(ServerConnector.class::cast)
          .filter(connector -> connector.getLocalPort() == 18443)
          .findFirst()
          .orElseThrow();

      assertThat(httpsConnector.getConnectionFactory(SslConnectionFactory.class)).isNotNull();
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  public void shouldApplyAdditionalConnectorOwnIdleTimeoutThenFallBackToDefault() {
    Server server = new Server();

    DropwizardConnectorSettings.AdditionalConnector ownTimeout =
        new DropwizardConnectorSettings.AdditionalConnector("http", 0, null, Duration.ofMinutes(5), null);
    DropwizardConnectorSettings.AdditionalConnector noTimeout =
        new DropwizardConnectorSettings.AdditionalConnector("http", 0, null, null, null);

    ServerConnector withOwnTimeout =
        DropwizardConnectorConfiguration.buildAdditionalConnector(server, ownTimeout, Duration.ofMinutes(1));
    ServerConnector inheritingDefault =
        DropwizardConnectorConfiguration.buildAdditionalConnector(server, noTimeout, Duration.ofMinutes(3));

    assertThat(withOwnTimeout.getIdleTimeout()).isEqualTo(Duration.ofMinutes(5).toMillis());
    assertThat(inheritingDefault.getIdleTimeout()).isEqualTo(Duration.ofMinutes(3).toMillis());
  }

  @Test
  public void shouldTranslatePrimaryConnectorCertAliasToSpringKeyAlias() throws Exception {
    File config = tempFolder.newFile("cert-alias.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: https\n" +
            "      port: 8443\n" +
            "      keyStorePath: /tmp/application-keystore.jks\n" +
            "      keyStorePassword: secret\n" +
            "      certAlias: iq-app\n" +
            "  adminConnectors:\n" +
            "    - type: https\n" +
            "      port: 8444\n" +
            "      keyStorePath: /tmp/admin-keystore.jks\n" +
            "      keyStorePassword: adminsecret\n" +
            "      certAlias: iq-admin\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.ssl.key-alias")).isEqualTo("iq-app");
    assertThat(environment.getProperty("management.server.ssl.key-alias")).isEqualTo("iq-admin");
  }

  @Test
  public void shouldDisableApplicationSslWhenApplicationConnectorIsHttp() throws Exception {
    File config = tempFolder.newFile("http-application.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.ssl.enabled")).isEqualTo("false");
  }

  @Test
  public void shouldDisableManagementSslWhenAdminConnectorIsHttpAndApplicationConnectorIsHttps() throws Exception {
    File config = tempFolder.newFile("https-app-http-admin.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: https\n" +
            "      port: 8443\n" +
            "      keyStorePath: /tmp/application-keystore.jks\n" +
            "      keyStorePassword: secret\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 8071\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.ssl.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("management.server.ssl.enabled")).isEqualTo("false");
  }

  @Test
  public void shouldTranslateMultipleApplicationConnectorsIntoPrimaryAndAdditionalProperties() throws Exception {
    File config = tempFolder.newFile("multiple-application-connectors.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: https\n" +
            "      port: 8443\n" +
            "      keyStorePath: /tmp/application-keystore.jks\n" +
            "      keyStorePassword: secret\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "      bindHost: 127.0.0.9\n");

    StandardEnvironment environment = new StandardEnvironment();

    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8443");
    assertThat(environment.getProperty("server.ssl.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("server.ssl.key-store")).isEqualTo("/tmp/application-keystore.jks");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.application-connector.additional[0].type")).isEqualTo("http");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.application-connector.additional[0].port")).isEqualTo("8070");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.application-connector.additional[0].bind-host")).isEqualTo("127.0.0.9");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.application-connector.additional[1].port")).isNull();
  }

  @Test
  public void shouldFailFastWhenPrimaryConnectorTypeIsUnknown() throws Exception {
    File config = tempFolder.newFile("unknown-primary-connector-type.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: htps\n" +
            "      port: 8443\n");

    StandardEnvironment environment = new StandardEnvironment();

    assertThatThrownBy(() -> new DropwizardConfigLoader().loadConfig(config, environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("htps")
        .hasMessageContaining("expected 'http' or 'https'");
  }

  @Test
  public void shouldFailFastWhenAdditionalConnectorTypeIsUnknown() {
    assertThatThrownBy(
        () -> new DropwizardConnectorSettings.AdditionalConnector("htps", 8070, null, null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("htps")
            .hasMessageContaining("expected 'http' or 'https'");
  }

  @Test
  public void shouldFailFastWhenAdditionalConnectorTypeIsUnknownInConfigFile() throws Exception {
    File config = tempFolder.newFile("unknown-additional-connector-type.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "    - type: htps\n" +
            "      port: 8443\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThatThrownBy(() -> DropwizardConnectorSettings.from(environment))
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("htps")
        .hasMessageContaining("expected 'http' or 'https'");
  }

  @Test
  public void shouldFailFastWhenAdditionalHttpsConnectorHasNoKeyStore() {
    assertThatThrownBy(
        () -> new DropwizardConnectorSettings.AdditionalConnector("https", 8443, null, null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("8443")
            .hasMessageContaining("No keyStorePath configured");
  }

  @Test
  public void shouldFailFastWhenAdditionalHttpsConnectorHasNoKeyStoreInConfigFile() throws Exception {
    File config = tempFolder.newFile("additional-https-connector-without-keystore.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "    - type: https\n" +
            "      port: 8443\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThatThrownBy(() -> DropwizardConnectorSettings.from(environment))
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("8443")
        .hasMessageContaining("No keyStorePath configured");
  }

  @Test
  public void shouldBindAllApplicationConnectorPortsWhenMultipleHttpConnectorsConfigured() throws Exception {
    File config = tempFolder.newFile("multiple-http-application-connectors.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 18070\n" +
            "    - type: http\n" +
            "      port: 18071\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    DropwizardConnectorSettings connectorSettings = DropwizardConnectorSettings.from(environment);
    WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer =
        new DropwizardConnectorConfiguration().dropwizardApplicationConnectorCustomizer(connectorSettings);

    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(18070);
    customizer.customize(factory);

    WebServer webServer = factory.getWebServer(servletContext -> {
    });
    try {
      webServer.start();
      Server server = ((JettyWebServer) webServer).getServer();
      assertThat(Arrays.stream(server.getConnectors())
          .filter(ServerConnector.class::isInstance)
          .map(ServerConnector.class::cast)
          .map(ServerConnector::getLocalPort))
              .contains(18070, 18071);
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  public void shouldTranslateMultipleAdminConnectorsIntoPrimaryAndAdditionalProperties() throws Exception {
    File config = tempFolder.newFile("multiple-admin-connectors.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 8071\n" +
            "    - type: https\n" +
            "      port: 8444\n" +
            "      keyStorePath: /tmp/admin-keystore.jks\n" +
            "      keyStorePassword: adminsecret\n" +
            "      certAlias: iq-admin\n" +
            "      needClientAuth: true\n");

    StandardEnvironment environment = new StandardEnvironment();

    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("management.server.port")).isEqualTo("8071");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.admin-connector.additional[0].type")).isEqualTo("https");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.admin-connector.additional[0].port")).isEqualTo("8444");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.admin-connector.additional[0].ssl.key-store")).isEqualTo("/tmp/admin-keystore.jks");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.admin-connector.additional[0].ssl.key-store-password")).isEqualTo("adminsecret");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.admin-connector.additional[0].ssl.certificate-alias")).isEqualTo("iq-admin");
    assertThat(environment.getProperty(
        "sonatype.dropwizard.admin-connector.additional[0].ssl.client-auth")).isEqualTo("need");
  }

  @Test
  public void shouldBindAllAdminConnectorPortsWhenMultipleHttpConnectorsConfigured() throws Exception {
    File config = tempFolder.newFile("multiple-http-admin-connectors.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 18071\n" +
            "    - type: http\n" +
            "      port: 18081\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    DropwizardConnectorSettings connectorSettings = DropwizardConnectorSettings.from(environment);
    WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer =
        new DropwizardManagementConnectorConfiguration().dropwizardAdminConnectorCustomizer(connectorSettings);

    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(18071);
    customizer.customize(factory);

    WebServer webServer = factory.getWebServer(servletContext -> {
    });
    try {
      webServer.start();
      Server server = ((JettyWebServer) webServer).getServer();
      assertThat(Arrays.stream(server.getConnectors())
          .filter(ServerConnector.class::isInstance)
          .map(ServerConnector.class::cast)
          .map(ServerConnector::getLocalPort))
              .contains(18071, 18081);
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  public void shouldAcceptDeprecatedApplicationConnectorSettings() throws Exception {
    File config = tempFolder.newFile("deprecated-application-connector-setting.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "      acceptQueueSize: 1024\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8070");
  }

  @Test
  public void shouldAcceptDeprecatedAdminConnectorSettingsWhenBindingInsightConfig() throws Exception {
    File config = tempFolder.newFile("deprecated-admin-connector-setting.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 8071\n" +
            "      acceptQueueSize: 1024\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig).isNotNull();
  }

  @Test
  public void shouldBindMultipleApplicationConnectorPortsAndTypesWhenBindingInsightConfig() throws Exception {
    File config = tempFolder.newFile("multi-connector-ports.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "    - type: https\n" +
            "      port: 8443\n" +
            "  adminConnectors:\n" +
            "    - type: https\n" +
            "      port: 8444\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getApplicationConnectorPorts()).isEqualTo("8070,8443");
    assertThat(insightConfig.getApplicationConnectorTypes()).isEqualTo("http,https");
  }

  @Test
  public void shouldBindMultipleAdminConnectorTypesWhenBindingInsightConfig() throws Exception {
    File config = tempFolder.newFile("multi-admin-connector-ports.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 8071\n" +
            "    - type: https\n" +
            "      port: 8444\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getAdminConnectorTypes()).isEqualTo("http,https");
  }

  @Test
  public void shouldExtractSingleApplicationConnectorPort() throws Exception {
    File config = tempFolder.newFile("single-connector-port.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 9090\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getApplicationConnectorPorts()).isEqualTo("9090");
  }

  @Test
  public void shouldKeepDefaultPortWhenNoConnectorsDefined() throws Exception {
    File config = tempFolder.newFile("no-connectors.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: sonatype-work/test\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getApplicationConnectorPorts()).isEqualTo("8070");
  }

  @Test
  public void shouldParseHstsConfigFromWebSection() throws Exception {
    File config = tempFolder.newFile("hsts-config.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  hsts:\n" +
            "    enabled: true\n" +
            "    maxAge: 180 days\n" +
            "    includeSubDomains: false\n" +
            "    preload: true\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHstsConfig().isEnabled()).isTrue();
    assertThat(insightConfig.getHstsConfig().getMaxAgeSeconds()).isEqualTo(180L * 24 * 60 * 60);
    assertThat(insightConfig.getHstsConfig().isIncludeSubDomains()).isFalse();
    assertThat(insightConfig.getHstsConfig().isPreload()).isTrue();
    assertThat(insightConfig.getHstsConfig().buildHeaderValue())
        .isEqualTo("max-age=" + (180L * 24 * 60 * 60) + "; preload");
  }

  @Test
  public void shouldDisableHstsWhenExplicitlyConfigured() throws Exception {
    File config = tempFolder.newFile("hsts-disabled.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  hsts:\n" +
            "    enabled: false\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHstsConfig().isEnabled()).isFalse();
  }

  @Test
  public void shouldDefaultHstsToEnabledWhenNoWebSectionPresent() throws Exception {
    File config = tempFolder.newFile("no-web-section.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: sonatype-work/test\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHstsConfig().isEnabled()).isTrue();
    assertThat(insightConfig.getHstsConfig().getMaxAgeSeconds()).isEqualTo(365L * 24 * 60 * 60);
    assertThat(insightConfig.getHstsConfig().isIncludeSubDomains()).isTrue();
    assertThat(insightConfig.getHstsConfig().isPreload()).isFalse();
  }

  @Test
  public void shouldParseHstsDurationWithDaysUnit() {
    assertThat(DropwizardConfigConfiguration.parseDurationToSeconds("365 days")).isEqualTo(365L * 24 * 60 * 60);
    assertThat(DropwizardConfigConfiguration.parseDurationToSeconds("30 days")).isEqualTo(30L * 24 * 60 * 60);
  }

  @Test
  public void shouldParseHstsDurationWithHoursUnit() {
    assertThat(DropwizardConfigConfiguration.parseDurationToSeconds("24 hours")).isEqualTo(24L * 60 * 60);
  }

  @Test
  public void shouldParseHstsDurationUsingLegacyShortUnits() {
    assertThat(DropwizardConfigConfiguration.parseDurationToSeconds("30m")).isEqualTo(30L * 60);
    assertThat(DropwizardConfigConfiguration.parseDurationToSeconds("24h")).isEqualTo(24L * 60 * 60);
    assertThat(DropwizardConfigConfiguration.parseDurationToSeconds("365d")).isEqualTo(365L * 24 * 60 * 60);
  }

  @Test
  public void shouldParseHstsDurationAsRawSeconds() {
    assertThat(DropwizardConfigConfiguration.parseDurationToSeconds("86400")).isEqualTo(86400L);
  }

  @Test
  public void shouldTranslateLegacyFrameOptionsSettingInsteadOfFailingStartup() throws Exception {
    File config = tempFolder.newFile("legacy-frame-options.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  frame-options:\n" +
            "    enabled: true\n" +
            "    option: SAMEORIGIN\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getFrameOptionsConfig().isEnabled()).isTrue();
    assertThat(insightConfig.getFrameOptionsConfig().getOption())
        .isEqualTo(InsightConfig.FrameOptionsConfig.FrameOption.SAMEORIGIN);
  }

  @Test
  public void shouldAcceptSupportedLegacyWebSettings() throws Exception {
    File config = tempFolder.newFile("supported-legacy-web-config.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  hsts:\n" +
            "    enabled: false\n" +
            "  frame-options:\n" +
            "    enabled: false\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHstsConfig().isEnabled()).isFalse();
    assertThat(insightConfig.getFrameOptionsConfig().isEnabled()).isFalse();
  }

  @Test
  public void shouldAcceptLegacyWebHeaderSettingsInsteadOfFailingStartup() throws Exception {
    File config = tempFolder.newFile("legacy-web-headers.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  uriPath: /iq\n" +
            "  content-type-options:\n" +
            "    enabled: true\n" +
            "  xss-protection:\n" +
            "    enabled: true\n" +
            "    on: true\n" +
            "    block: true\n" +
            "  csp:\n" +
            "    enabled: true\n" +
            "    policy: default-src 'self'\n" +
            "    reportOnlyPolicy: default-src 'none'\n" +
            "  headers:\n" +
            "    X-Custom-Legacy-Header: legacy-value\n");

    DropwizardConfigConfiguration configuration = new DropwizardConfigConfiguration();
    InsightConfig insightConfig = configuration.insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());
    DropwizardWebSettings webSettings = configuration.dropwizardWebSettings(config.getAbsolutePath());

    assertThat(insightConfig).isNotNull();
    assertThat(webSettings.getUriPath()).isEqualTo("/iq");
    assertThat(webSettings.getUrlPattern()).isEqualTo("/iq/*");
    assertThat(webSettings.getHeaders()).containsExactlyInAnyOrderEntriesOf(Map.of(
        LegacyWebHeaderFilter.X_CONTENT_TYPE_OPTIONS, "nosniff",
        LegacyWebHeaderFilter.X_XSS_PROTECTION, "1; mode=block",
        LegacyWebHeaderFilter.CONTENT_SECURITY_POLICY, "default-src 'self'",
        LegacyWebHeaderFilter.CONTENT_SECURITY_POLICY_REPORT_ONLY, "default-src 'none'",
        "X-Custom-Legacy-Header", "legacy-value"));
  }

  @Test
  public void shouldAcceptDisabledLegacyContentTypeOptionsWithoutAddingLegacyHeader() throws Exception {
    File config = tempFolder.newFile("legacy-content-type-options-disabled.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  content-type-options:\n" +
            "    enabled: false\n");

    DropwizardConfigConfiguration configuration = new DropwizardConfigConfiguration();
    InsightConfig insightConfig = configuration.insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());
    DropwizardWebSettings webSettings = configuration.dropwizardWebSettings(config.getAbsolutePath());

    assertThat(insightConfig).isNotNull();
    assertThat(webSettings.getHeaders()).doesNotContainKey(LegacyWebHeaderFilter.X_CONTENT_TYPE_OPTIONS);
  }

  @Test
  public void shouldMapLegacyXssProtectionOffValue() throws Exception {
    File config = tempFolder.newFile("legacy-xss-protection-off.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  xss-protection:\n" +
            "    enabled: true\n" +
            "    on: false\n");

    DropwizardWebSettings webSettings = new DropwizardConfigConfiguration().dropwizardWebSettings(
        config.getAbsolutePath());

    assertThat(webSettings.getHeaders()).containsEntry(LegacyWebHeaderFilter.X_XSS_PROTECTION, "0");
  }

  @Test
  public void shouldAcceptLegacyCorsSettingsInsteadOfFailingStartup() throws Exception {
    File config = tempFolder.newFile("legacy-cors.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  cors:\n" +
            "    allowedOrigins:\n" +
            "      - https://app.example.com\n" +
            "    allowedTimingOrigins:\n" +
            "      - https://timing.example.com\n" +
            "    allowedMethods:\n" +
            "      - GET\n" +
            "      - OPTIONS\n" +
            "    allowedHeaders:\n" +
            "      - Authorization\n" +
            "      - Content-Type\n" +
            "    preflightMaxAge: 30 minutes\n" +
            "    allowCredentials: true\n" +
            "    exposedHeaders:\n" +
            "      - X-Result\n" +
            "    chainPreflight: true\n");

    DropwizardConfigConfiguration configuration = new DropwizardConfigConfiguration();
    InsightConfig insightConfig = configuration.insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());
    DropwizardWebSettings.CorsSettings corsSettings = configuration.dropwizardWebSettings(config.getAbsolutePath())
        .getCorsSettings();

    assertThat(insightConfig).isNotNull();
    assertThat(corsSettings.getAllowedOrigins()).containsExactly("https://app.example.com");
    assertThat(corsSettings.getAllowedTimingOrigins()).containsExactly("https://timing.example.com");
    assertThat(corsSettings.getAllowedMethods()).containsExactly("GET", "OPTIONS");
    assertThat(corsSettings.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
    assertThat(corsSettings.getPreflightMaxAge()).isEqualTo(Duration.ofMinutes(30));
    assertThat(corsSettings.isAllowCredentials()).isTrue();
    assertThat(corsSettings.getExposedHeaders()).containsExactly("X-Result");
    assertThat(corsSettings.isChainPreflight()).isTrue();
  }

  @Test
  public void shouldRejectUnknownWebSettingsInsteadOfIgnoringThem() throws Exception {
    File config = tempFolder.newFile("unknown-web.yml");
    Files.writeString(config.toPath(),
        "web:\n" +
            "  unexpected-header-config: true\n");

    assertThatThrownBy(() -> new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unrecognized field \"unexpected-header-config\"");
  }

  @Test
  public void shouldCaptureApplicationAndAdminConnectorTypesFromDropwizardConfig() throws Exception {
    File config = tempFolder.newFile("connector-types.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "  adminConnectors:\n" +
            "    - type: https\n" +
            "      port: 8444\n" +
            "  requestLog:\n" +
            "    appenders:\n" +
            "      - type: file\n" +
            "        currentLogFilename: /var/log/nexus-iq-server/request.log\n" +
            "logging:\n" +
            "  appenders:\n" +
            "    - type: file\n" +
            "      currentLogFilename: /var/log/nexus-iq-server/clm-server.log\n" +
            "  loggers:\n" +
            "    com.sonatype.insight.audit:\n" +
            "      appenders:\n" +
            "        - type: file\n" +
            "          currentLogFilename: /var/log/nexus-iq-server/audit.log\n" +
            "    com.sonatype.insight.policy.violation:\n" +
            "      appenders:\n" +
            "        - type: file\n" +
            "          currentLogFilename: /var/log/nexus-iq-server/policy-violation.log\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getApplicationConnectorTypes()).isEqualTo("http");
    assertThat(insightConfig.getAdminConnectorTypes()).isEqualTo("https");
    assertThat(insightConfig.getServerLogFilename()).isEqualTo("/var/log/nexus-iq-server/clm-server.log");
    assertThat(insightConfig.getRequestLogFilename()).isEqualTo("/var/log/nexus-iq-server/request.log");
    assertThat(insightConfig.getAuditLogFilename()).isEqualTo("/var/log/nexus-iq-server/audit.log");
    assertThat(insightConfig.getPolicyViolationLogFilename())
        .isEqualTo("/var/log/nexus-iq-server/policy-violation.log");
  }

  private void assertConnectorIdleTimeout(
      WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer,
      Duration expectedIdleTimeout)
  {
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    customizer.customize(factory);

    WebServer webServer = factory.getWebServer(servletContext -> {
    });
    try {
      webServer.start();
      Server server = ((JettyWebServer) webServer).getServer();
      assertThat(Arrays.stream(server.getConnectors())
          .filter(ServerConnector.class::isInstance)
          .map(ServerConnector.class::cast)
          .map(ServerConnector::getIdleTimeout))
              .containsOnly(expectedIdleTimeout.toMillis());
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  public void shouldTranslatePortsFromPackagedNexusMtiqConfig() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    loadShippedApplicationDefaults(environment);

    new DropwizardConfigLoader().loadConfig(resolvePackagedNexusMtiqConfig().toFile(), environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8070");
    assertThat(environment.getProperty("management.server.port")).isEqualTo("8071");
    assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo("/");
    assertThat(environment.getProperty("management.server.address")).isEqualTo("127.0.0.1");
    assertThat(environment.getProperty("logging.level.root")).isEqualTo("DEBUG");
    assertThat(environment.getProperty("logging.level.org.eclipse.jetty")).isEqualTo("INFO");
  }

  @Test
  public void shouldBindInsightConfigFromPackagedNexusMtiqConfig() throws Exception {
    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(resolvePackagedNexusMtiqConfig().toString(), InsightConfig.class.getName());

    assertThat(insightConfig).isNotNull();
    assertThat(insightConfig.getSonatypeWork().getPath()).endsWith("sonatype-work/clm-server");
    assertThat(insightConfig.isCreateSampleData()).isTrue();
    assertThat(insightConfig.getApplicationConnectorPorts()).isEqualTo("8070");
    assertThat(insightConfig.getApplicationConnectorTypes()).isEqualTo("http");
    assertThat(insightConfig.getAdminConnectorTypes()).isEqualTo("http");
    assertThat(insightConfig.getHstsConfig().isEnabled()).isTrue();
    assertThat(insightConfig.getHstsConfig().getMaxAgeSeconds()).isEqualTo(365L * 24 * 60 * 60);
    assertThat(insightConfig.getHstsConfig().isIncludeSubDomains()).isTrue();
  }

  @Test
  public void shouldPreserveEscapedDollarDollarVariablesAsLiteralPlaceholders() throws Exception {
    File config = tempFolder.newFile("escaped-vars.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: ./sonatype-work/clm-server\n" +
            "database:\n" +
            "  type: postgresql\n" +
            "  hostname: $${NXIQ_DATABASE_HOSTNAME}\n" +
            "  port: 5432\n" +
            "  name: $${NXIQ_DATABASE_NAME}\n" +
            "  username: $${NXIQ_DATABASE_USERNAME}\n" +
            "  password: $${NXIQ_DATABASE_PASSWORD}\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getDatabase().getHostname()).isEqualTo("${NXIQ_DATABASE_HOSTNAME}");
    assertThat(insightConfig.getDatabase().getName()).isEqualTo("${NXIQ_DATABASE_NAME}");
    assertThat(insightConfig.getDatabase().getUsername()).isEqualTo("${NXIQ_DATABASE_USERNAME}");
    assertThat(insightConfig.getDatabase().getPassword()).isEqualTo("${NXIQ_DATABASE_PASSWORD}");
  }

  @Test
  public void shouldHandleRequestLogWithJsonLayoutAppenderWithoutError() throws Exception {
    File config = tempFolder.newFile("json-layout-request-log.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 8071\n" +
            "  requestLog:\n" +
            "    appenders:\n" +
            "      - type: console\n" +
            "        layout:\n" +
            "          type: access-json\n" +
            "          includes:\n" +
            "            - timestamp\n" +
            "            - statusCode\n" +
            "            - requestUrl\n" +
            "            - method\n" +
            "          customFieldNames:\n" +
            "            url: message\n" +
            "            status: http.status_code\n" +
            "          additionalFields:\n" +
            "            logType: RequestLog\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8070");

    // An access-json layout appender is handled by the access-json path, so the classic requestLogSettings disables.
    RequestLoggingConfiguration requestLoggingConfiguration = new RequestLoggingConfiguration();
    RequestLogConfig requestLog = new RequestLogConfig();
    requestLog.appenders = List.of(Map.of("type", "console", "layout", Map.of("type", "access-json")));
    RequestLoggingConfiguration.RequestLogSettings settings =
        requestLoggingConfiguration.requestLogSettings(requestLog);
    assertThat(settings.enabled()).isFalse();
  }

  @Test
  public void shouldRejectUnknownTopLevelKeysWhenBindingInsightConfig() throws Exception {
    File config = tempFolder.newFile("unknown-top-level.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: ./sonatype-work/clm-server\n" +
            "unknownProperty: true\n");

    assertThatThrownBy(() -> new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unrecognized field \"unknownProperty\"");
  }

  @Test
  public void shouldAcceptServerSectionWithVirtualThreadsAndAdminContextPath() throws Exception {
    File config = tempFolder.newFile("virtual-threads.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  enableVirtualThreads: true\n" +
            "  enableAdminVirtualThreads: true\n" +
            "  applicationContextPath: /\n" +
            "  adminContextPath: /\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "  adminConnectors:\n" +
            "    - type: http\n" +
            "      port: 8071\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(config, environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8070");
    assertThat(environment.getProperty("management.server.port")).isEqualTo("8071");
    assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo("/");
    assertThat(environment.getProperty("spring.threads.virtual.enabled")).isEqualTo("true");
  }

  @Test
  public void shouldEnableHstsByDefaultWhenWebSectionAbsent() throws Exception {
    File config = tempFolder.newFile("no-web.yml");
    Files.writeString(config.toPath(), "sonatypeWork: ./sonatype-work/clm-server\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHstsConfig().isEnabled()).isTrue();
    assertThat(insightConfig.getHstsConfig().getMaxAgeSeconds()).isEqualTo(365L * 24 * 60 * 60);
    assertThat(insightConfig.getHstsConfig().isIncludeSubDomains()).isTrue();
  }

  @Test
  public void shouldDisableHstsWhenConfiguredExplicitly() throws Exception {
    File config = tempFolder.newFile("hsts-disabled.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: ./sonatype-work/clm-server\n" +
            "web:\n" +
            "  hsts:\n" +
            "    enabled: false\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHstsConfig().isEnabled()).isFalse();
  }

  @Test
  public void shouldApplyCustomHstsSettings() throws Exception {
    File config = tempFolder.newFile("hsts-custom.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: ./sonatype-work/clm-server\n" +
            "web:\n" +
            "  hsts:\n" +
            "    enabled: true\n" +
            "    maxAge: 600\n" +
            "    includeSubDomains: false\n" +
            "    preload: true\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getHstsConfig().isEnabled()).isTrue();
    assertThat(insightConfig.getHstsConfig().getMaxAgeSeconds()).isEqualTo(600L);
    assertThat(insightConfig.getHstsConfig().isIncludeSubDomains()).isFalse();
    assertThat(insightConfig.getHstsConfig().isPreload()).isTrue();
  }

  @Test
  public void shouldDisableFrameOptionsByDefault() throws Exception {
    File config = tempFolder.newFile("no-frame-options.yml");
    Files.writeString(config.toPath(), "sonatypeWork: ./sonatype-work/clm-server\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getFrameOptionsConfig().isEnabled()).isFalse();
    assertThat(insightConfig.getFrameOptionsConfig().buildHeaderValue()).isEqualTo("DENY");
  }

  @Test
  public void shouldApplyFrameOptionsAllowFromWithOrigin() throws Exception {
    File config = tempFolder.newFile("frame-options-allow-from.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: ./sonatype-work/clm-server\n" +
            "web:\n" +
            "  frame-options:\n" +
            "    enabled: true\n" +
            "    option: ALLOW-FROM\n" +
            "    origin: https://example.com\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getFrameOptionsConfig().isEnabled()).isTrue();
    assertThat(insightConfig.getFrameOptionsConfig().buildHeaderValue())
        .isEqualTo("ALLOW-FROM https://example.com");
  }

  @Test
  public void shouldFallBackToDenyWhenAllowFromHasNoOrigin() throws Exception {
    File config = tempFolder.newFile("frame-options-allow-from-no-origin.yml");
    Files.writeString(config.toPath(),
        "sonatypeWork: ./sonatype-work/clm-server\n" +
            "web:\n" +
            "  frame-options:\n" +
            "    option: ALLOW-FROM\n");

    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(config.getAbsolutePath(), InsightConfig.class.getName());

    assertThat(insightConfig.getFrameOptionsConfig().buildHeaderValue()).isEqualTo("DENY");
  }

  private Path resolvePackagedNexusIqConfig() {
    Path current = Path.of("").toAbsolutePath();
    for (Path directory = current; directory != null; directory = directory.getParent()) {
      Path candidate = directory.resolve("nexus-iq-server/src/main/resources/config.yml");
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
    }
    throw new AssertionError("Could not locate nexus-iq-server/src/main/resources/config.yml from " + current);
  }

  private Path resolvePackagedNexusMtiqConfig() {
    Path current = Path.of("").toAbsolutePath();
    for (Path directory = current; directory != null; directory = directory.getParent()) {
      Path candidate = directory.resolve("nexus-mtiq-server/src/main/resources/config.yml");
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
    }
    throw new AssertionError("Could not locate nexus-mtiq-server/src/main/resources/config.yml from " + current);
  }

  private void loadShippedApplicationDefaults(StandardEnvironment environment) throws Exception {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> propertySources = loader.load(
        "applicationConfig",
        new ClassPathResource("application.yml"));
    propertySources.forEach(environment.getPropertySources()::addLast);
  }
}
