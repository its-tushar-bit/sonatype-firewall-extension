/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import com.sonatype.insight.brain.testsupport.TempFolder;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.env.StandardEnvironment;

public class DropwizardConfigLoaderTest
{
  @RegisterExtension
  public TempFolder tempFolder = new TempFolder();

  @Test
  public void loadConfig_legacyHttpSection_throwsUpgradeGuidance() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "http:\n  port: 8070\n  adminPort: 8071\n");

    assertThatThrownBy(() -> new DropwizardConfigLoader().loadConfig(configFile, new StandardEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Nexus IQ Server version 1.42")
        .hasMessageContaining("updating-your-configuration");
  }

  @Test
  public void loadConfig_legacyLoggingConsoleSection_throwsUpgradeGuidance() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "logging:\n  console:\n    threshold: INFO\n");

    assertThatThrownBy(() -> new DropwizardConfigLoader().loadConfig(configFile, new StandardEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("version 1.42 and lower");
  }

  @Test
  public void loadConfig_legacyLoggingFileSection_throwsUpgradeGuidance() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "logging:\n  file:\n    currentLogFilename: ./log/clm-server.log\n");

    assertThatThrownBy(() -> new DropwizardConfigLoader().loadConfig(configFile, new StandardEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("version 1.42 and lower");
  }

  @Test
  public void loadConfig_legacyLoggingSyslogSection_throwsUpgradeGuidance() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "logging:\n  syslog:\n    host: localhost\n");

    assertThatThrownBy(() -> new DropwizardConfigLoader().loadConfig(configFile, new StandardEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("version 1.42 and lower");
  }

  @Test
  public void loadConfig_modernLoggingAppenders_isAccepted() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "logging:\n  level: INFO\n  appenders:\n    - type: console\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("logging.level.root")).isEqualTo("INFO");
  }

  @Test
  public void flattenMap_convertsScalarListToIndexedProperties() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "customList:\n  - alpha\n  - bravo\n  - charlie\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("customList[0]")).isEqualTo("alpha");
    assertThat(environment.getProperty("customList[1]")).isEqualTo("bravo");
    assertThat(environment.getProperty("customList[2]")).isEqualTo("charlie");
  }

  @Test
  public void flattenMap_convertsListOfMapsToIndexedDotProperties() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(),
        "items:\n  - name: first\n    value: 1\n  - name: second\n    value: 2\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("items[0].name")).isEqualTo("first");
    assertThat(environment.getProperty("items[0].value")).isEqualTo("1");
    assertThat(environment.getProperty("items[1].name")).isEqualTo("second");
    assertThat(environment.getProperty("items[1].value")).isEqualTo("2");
  }

  @Test
  public void flattenMap_preservesScalarProperties() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "sonatypeWork: /opt/sonatype/work\nbaseUrl: http://localhost\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("sonatypeWork")).isEqualTo("/opt/sonatype/work");
    assertThat(environment.getProperty("baseUrl")).isEqualTo("http://localhost");
  }

  @Test
  public void flattenMap_preservesNestedMapProperties() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), "parent:\n  child:\n    key: value\n");

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("parent.child.key")).isEqualTo("value");
  }

  @Test
  public void translateServerSection_translatesThreadPoolSettings() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  maxThreads: 512",
        "  minThreads: 16",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.jetty.threads.max")).isEqualTo("512");
    assertThat(environment.getProperty("server.jetty.threads.min")).isEqualTo("16");
  }

  @Test
  public void translateLoggingSection_handlesMapValuedLoggerWithLevel() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "logging:",
        "  level: INFO",
        "  loggers:",
        "    com.sonatype.insight.audit:",
        "      level: DEBUG",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: ./log/audit.log",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("logging.level.root")).isEqualTo("INFO");
    assertThat(environment.getProperty("logging.level.com.sonatype.insight.audit")).isEqualTo("DEBUG");
  }

  @Test
  public void translateServerSection_translatesGzipSettings() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        "  gzip:",
        "    enabled: true",
        "    minimumEntitySize: 256",
        "    compressedMimeTypes:",
        "      - text/html",
        "      - application/json",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.compression.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("server.compression.min-response-size")).isEqualTo("256");
    assertThat(environment.getProperty("server.compression.mime-types")).isEqualTo("text/html,application/json");
  }

  @Test
  public void translateServerSection_excludedMimeTypes_removedFromCompressedList() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        "  gzip:",
        "    enabled: true",
        "    compressedMimeTypes:",
        "      - text/html",
        "      - application/json",
        "      - text/csv",
        "    excludedMimeTypes:",
        "      - text/csv",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.compression.mime-types")).isEqualTo("text/html,application/json");
  }

  @Test
  public void translateServerSection_gzipAbsent_enablesCompressionByDefault() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.compression.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("server.compression.min-response-size")).isEqualTo("1KB");
    assertThat(environment.getProperty("server.compression.mime-types"))
        .contains("text/javascript", "text/css")
        .doesNotContain("text/csv");
  }

  @Test
  public void translateServerSection_excludedMimeTypes_removedFromDefaultList() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        "  gzip:",
        "    enabled: true",
        "    excludedMimeTypes:",
        "      - text/css",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.compression.mime-types"))
        .contains("text/html", "text/javascript")
        .doesNotContain("text/css");
  }

  @Test
  public void translateServerSection_gzipDisabled() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        "  gzip:",
        "    enabled: false",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.compression.enabled")).isEqualTo("false");
  }

  @Test
  public void translateServerSection_translatesShutdownGracePeriod() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  shutdownGracePeriod: 30 seconds",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.shutdown")).isEqualTo("graceful");
    assertThat(environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
  }

  @Test
  public void translateServerSection_translatesContextPath() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationContextPath: /myapp",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo("/myapp");
  }

  @Test
  public void translateServerSection_translatesAdminConnector() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        "  adminConnectors:",
        "    - type: http",
        "      port: 8071",
        "      bindHost: 127.0.0.1",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("management.server.port")).isEqualTo("8071");
    assertThat(environment.getProperty("management.server.address")).isEqualTo("127.0.0.1");
  }

  @Test
  public void translateServerSection_translatesHttpsConnector() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "server:",
        "  applicationConnectors:",
        "    - type: https",
        "      port: 8443",
        "      keyStorePath: /path/to/keystore",
        "      keyStorePassword: secret",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("server.port")).isEqualTo("8443");
    assertThat(environment.getProperty("server.ssl.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("server.ssl.key-store")).isEqualTo("/path/to/keystore");
    assertThat(environment.getProperty("server.ssl.key-store-password")).isEqualTo("secret");
  }

  @Test
  public void translateServerSection_translatesDwSystemPropertyOverrides() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "sonatypeWork: ./original-work",
        "server:",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        ""));

    System.setProperty("dw.sonatypeWork", "./overridden-work");
    try {
      StandardEnvironment environment = new StandardEnvironment();
      new DropwizardConfigLoader().loadConfig(configFile, environment);

      assertThat(environment.getProperty("sonatypeWork")).isEqualTo("./overridden-work");
    }
    finally {
      System.clearProperty("dw.sonatypeWork");
    }
  }

  @Test
  public void translateLoggingSection_scalarAndMapLoggersTogether() throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), String.join("\n",
        "logging:",
        "  level: INFO",
        "  loggers:",
        "    org.apache.http: WARN",
        "    com.sonatype.insight.audit:",
        "      level: DEBUG",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: ./log/audit.log",
        "    org.eclipse.jetty: INFO",
        ""));

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);

    assertThat(environment.getProperty("logging.level.root")).isEqualTo("INFO");
    assertThat(environment.getProperty("logging.level.org.apache.http")).isEqualTo("WARN");
    assertThat(environment.getProperty("logging.level.com.sonatype.insight.audit")).isEqualTo("DEBUG");
    assertThat(environment.getProperty("logging.level.org.eclipse.jetty")).isEqualTo("INFO");
  }
}
