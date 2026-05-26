/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.db.DatabaseConfigProviderFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.scheduler.QuartzConcurrencyListener;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.spring.config.DropwizardConfigConfiguration;
import com.sonatype.insight.brain.spring.config.NamedBeanRegistrationConfiguration;
import com.sonatype.insight.brain.spring.config.ScheduledConfiguration;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

public class DropwizardConfigBootstrapTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldFallBackToDefaultsWhenImplicitDefaultConfigFileIsMissing() throws Exception {
    File missingConfig = new File(tempFolder.getRoot(), "config.yml");
    SpringApplicationBuilder builder = new SpringApplicationBuilder(TestDropwizardBootstrapApplication.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=false",
            "spring.main.allow-bean-definition-overriding=true");

    DropwizardConfigBootstrap.configure(builder, missingConfig.getAbsolutePath(), InsightConfig.class, true);

    try (ConfigurableApplicationContext context = builder.run()) {
      assertThat(context.getBean(InsightConfig.class)).isNotNull();
      assertThat(context.getEnvironment().getProperty("config.file"))
          .isEqualTo(missingConfig.getCanonicalPath());
      assertThat(context.getEnvironment().getProperty("config.file.implicitDefault")).isEqualTo("true");
      assertThat(ApplicationLifecycle.getConfigFile()).isEqualTo(missingConfig.getCanonicalFile());
    }
  }

  @Test
  public void shouldFailFastWhenExplicitConfigFileIsMissing() throws Exception {
    File missingConfig = new File(tempFolder.getRoot(), "missing-explicit.yml");
    SpringApplicationBuilder builder = new SpringApplicationBuilder(TestDropwizardBootstrapApplication.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=false",
            "spring.main.allow-bean-definition-overriding=true");

    DropwizardConfigBootstrap.configure(builder, missingConfig.getAbsolutePath(), InsightConfig.class, false);

    assertThatThrownBy(builder::run)
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(missingConfig.getCanonicalPath());
  }

  @Test
  public void shouldLoadExistingExplicitConfigFileViaSpringApplication() throws Exception {
    File config = tempFolder.newFile("explicit-config.yml");
    Files.writeString(config.toPath(), "hdsUrl: https://example.com/hds\n");

    SpringApplication application = new SpringApplication(TestDropwizardBootstrapApplication.class);
    application.setDefaultProperties(Map.of(
        "spring.main.web-application-type", "none",
        "spring.main.lazy-initialization", "false",
        "spring.main.allow-bean-definition-overriding", "true"));

    DropwizardConfigBootstrap.configure(application, config.getAbsolutePath(), InsightConfig.class, false);

    try (ConfigurableApplicationContext context = application.run()) {
      InsightConfig insightConfig = context.getBean(InsightConfig.class);

      assertThat(insightConfig.getHdsUrl()).isEqualTo("https://example.com/hds");
      assertThat(context.getEnvironment().getProperty("config.file")).isEqualTo(config.getCanonicalPath());
      assertThat(context.getEnvironment().getProperty("config.file.implicitDefault")).isEqualTo("false");
      assertThat(ApplicationLifecycle.getConfigFile()).isEqualTo(config.getCanonicalFile());
    }
  }

  @Test
  public void shouldFailFastDuringBootstrapWhenMultipleApplicationConnectorsAreConfigured() throws Exception {
    File config = tempFolder.newFile("multiple-application-connectors.yml");
    Files.writeString(config.toPath(),
        "server:\n" +
            "  applicationConnectors:\n" +
            "    - type: http\n" +
            "      port: 8070\n" +
            "    - type: https\n" +
            "      port: 8443\n");

    SpringApplication application = new SpringApplication(TestDropwizardBootstrapApplication.class);
    application.setDefaultProperties(Map.of(
        "spring.main.web-application-type", "none",
        "spring.main.lazy-initialization", "false",
        "spring.main.allow-bean-definition-overriding", "true"));

    DropwizardConfigBootstrap.configure(application, config.getAbsolutePath(), InsightConfig.class, false);

    assertThatThrownBy(application::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("applicationConnectors")
        .hasMessageContaining("multiple connectors are not supported");
  }

  @Test
  public void shouldFailFastDuringBootstrapWhenMultipleAdminConnectorsAreConfigured() throws Exception {
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
            "      port: 8444\n");

    SpringApplication application = new SpringApplication(TestDropwizardBootstrapApplication.class);
    application.setDefaultProperties(Map.of(
        "spring.main.web-application-type", "none",
        "spring.main.lazy-initialization", "false",
        "spring.main.allow-bean-definition-overriding", "true"));

    DropwizardConfigBootstrap.configure(application, config.getAbsolutePath(), InsightConfig.class, false);

    assertThatThrownBy(application::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("adminConnectors")
        .hasMessageContaining("multiple connectors are not supported");
  }

  @Test
  public void shouldBindPostgresOverridesWhenConfigDevTemplateIsCopiedAndAppended() throws Exception {
    File config = tempFolder.newFile("config-iq-postgres.yml");
    String template = new String(
        new ClassPathResource("config-dev.yml").getInputStream().readAllBytes(),
        StandardCharsets.UTF_8);
    Files.writeString(config.toPath(),
        template + """

            database:
              type: postgresql
              hostname: localhost
              port: 5432
              name: postgres
              username: postgres
              password: postgres
            licenseFile: /tmp/sonatype.lic
            """);

    SpringApplication application = new SpringApplication(TestDropwizardBootstrapApplication.class);
    application.setDefaultProperties(Map.of(
        "spring.main.web-application-type", "none",
        "spring.main.lazy-initialization", "false",
        "spring.main.allow-bean-definition-overriding", "true"));

    DropwizardConfigBootstrap.configure(application, config.getAbsolutePath(), InsightConfig.class, false);

    try (ConfigurableApplicationContext context = application.run()) {
      InsightConfig insightConfig = context.getBean(InsightConfig.class);

      assertThat(insightConfig.isDatabaseEmbedded()).isFalse();
      assertThat(insightConfig.getDatabase()).isNotNull();
      assertThat(insightConfig.getDatabase().getType()).isEqualTo("postgresql");
      assertThat(insightConfig.getDatabase().getHostname()).isEqualTo("localhost");
      assertThat(insightConfig.getDatabase().getPort()).isEqualTo(5432);
      assertThat(insightConfig.getDatabase().getName()).isEqualTo("postgres");
      assertThat(insightConfig.getLicenseFile()).isEqualTo("/tmp/sonatype.lic");
      assertThat(DatabaseConfigProviderFactory.createDatabaseConfigProvider(insightConfig)
          .getDatabaseConfig(DatabaseName.ods)
          .getUrl()).isEqualTo("jdbc:postgresql://localhost:5432/postgres");
    }
  }

  @Test
  public void shouldRegisterQuartzConcurrencyListenerOnceForExplicitConfigLaunch() throws Exception {
    File config = tempFolder.newFile("explicit-config.yml");
    Files.writeString(config.toPath(), "hdsUrl: https://example.com/hds\n");

    AtomicReference<String[]> quartzConcurrencyListenerBeanNames = new AtomicReference<>();

    SpringApplicationBuilder builder = new SpringApplicationBuilder(ExplicitSchedulerBootstrapApplication.class)
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=true");
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      quartzConcurrencyListenerBeanNames.set(
          beanFactory.getBeanNamesForType(QuartzConcurrencyListener.class, false, false));
      throw new StopAfterBeanCapture();
    }));

    DropwizardConfigBootstrap.configure(builder, config.getAbsolutePath(), InsightConfig.class, false);

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(quartzConcurrencyListenerBeanNames.get()).containsExactly("quartzConcurrencyListener");
  }

  @SpringBootConfiguration
  @Import(DropwizardConfigConfiguration.class)
  static class TestDropwizardBootstrapApplication
  {
  }

  @SpringBootConfiguration
  @Import({
    DropwizardConfigConfiguration.class,
    NamedBeanRegistrationConfiguration.class,
    ScheduledConfiguration.class
  })
  static class ExplicitSchedulerBootstrapApplication
  {
  }

  private static class StopAfterBeanCapture
      extends RuntimeException
  {
  }
}
