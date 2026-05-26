/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.spring.DropwizardConfigBootstrap;
import com.sonatype.insight.brain.telemetry.TelemetryCollectorsProvider;
import com.sonatype.insight.brain.testing.ExplicitTestConfigurationSupport;
import com.sonatype.insight.brain.testing.TestDatabaseContainerHolder;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public class SpringMultiTenantTestInsightBrainServiceComponentScanTest
{
  private static final String MTIQ_USER_RESOURCE_TEST_CONFIGURATION =
      "com.sonatype.insight.brain.users.MtiqUserResourceTest$MtiqUserResourceTestConfig";

  private static final String DATA_STORE_TEST_MODULE =
      "com.sonatype.insight.brain.testing.DataStoreTestModule";

  private static final String AUDIT_LOG_PATH_INITIALIZER =
      "com.sonatype.insight.brain.logging.MultiTenantAuditLogConfiguration$AuditLogPathInitializer";

  private static final String IQ_ONLY_SUPPORT_RESOURCE =
      "com.sonatype.insight.brain.support.SupportResource";

  private static final AtomicReference<Boolean> supportResourceBeanPresent = new AtomicReference<>(true);

  private static final AtomicReference<String> supportResourceDefinition = new AtomicReference<>();

  @Test
  public void shouldExcludeNestedTestConfigurationsFromAmbientComponentScan() throws ClassNotFoundException {
    Set<String> candidateClassNames = findCandidates(SpringMultiTenantTestInsightBrainService.class);

    assertThat(candidateClassNames)
        .contains("com.sonatype.insight.brain.validation.MtiqSourceControlSshValidator")
        .doesNotContain(MTIQ_USER_RESOURCE_TEST_CONFIGURATION)
        .doesNotContain(MTIQ_USER_RESOURCE_TEST_CONFIGURATION.replace('$', '.'))
        .doesNotContain(DATA_STORE_TEST_MODULE)
        .doesNotContain(DATA_STORE_TEST_MODULE.replace('$', '.'))
        .doesNotContain(AUDIT_LOG_PATH_INITIALIZER)
        .doesNotContain(AUDIT_LOG_PATH_INITIALIZER.replace('$', '.'))
        .doesNotContain(IQ_ONLY_SUPPORT_RESOURCE);
  }

  @Test
  public void shouldNotRegisterIqOnlySupportBeansInMtiqContext() {
    supportResourceBeanPresent.set(true);
    supportResourceDefinition.set(null);

    List<Class<?>> explicitTestConfigurations = List.of(SupportBeanCaptureConfiguration.class);

    SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringMultiTenantTestInsightBrainService.class)
        .sources(explicitTestConfigurations.toArray(new Class<?>[0]))
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=true",
            "spring.main.allow-bean-definition-overriding=true",
            "server.port=0",
            "management.server.port=0",
            "sonatypeWork=target/test-mtiq-scan",
            "clusterDirectory=target/test-mtiq-scan-cluster",
            "sonatype.mtiq.enabled=true");
    builder.initializers(ExplicitTestConfigurationSupport.initializer(explicitTestConfigurations));
    DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(supportResourceBeanPresent.get()).as(supportResourceDefinition.get()).isFalse();
  }

  @Test
  public void shouldNotRegisterAmbientMtiqUserResourceTestConfiguration() {
    AtomicBoolean configBeanPresent = new AtomicBoolean(true);

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      configBeanPresent.set(beanFactory.containsBeanDefinition("mtiqUserResourceTestConfig"));
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(configBeanPresent.get()).isFalse();
  }

  @Test
  public void shouldRegisterAuthorizationCheckerBeanForSecurityAop() {
    AtomicReference<List<String>> authorizationCheckerBeanNames = new AtomicReference<>();

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      authorizationCheckerBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(AuthorizationChecker.class)));
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(authorizationCheckerBeanNames.get()).isNotEmpty();
  }

  @Test
  public void shouldPreferMultiTenantSchedulerBeans() {
    AtomicReference<List<String>> taskSchedulerBeanNames = new AtomicReference<>();
    AtomicReference<List<String>> quartzJobStoreBeanNames = new AtomicReference<>();
    AtomicReference<Boolean> multiTenantTaskSchedulerPrimary = new AtomicReference<>();
    AtomicReference<Boolean> multiTenantQuartzJobStorePrimary = new AtomicReference<>();

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      taskSchedulerBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(TaskScheduler.class)));
      quartzJobStoreBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(QuartzJobStoreTX.class)));
      multiTenantTaskSchedulerPrimary.set(beanFactory.getBeanDefinition("multiTenantTaskScheduler").isPrimary());
      multiTenantQuartzJobStorePrimary.set(beanFactory.getBeanDefinition("multiTenantQuartzJobStoreTX").isPrimary());
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(taskSchedulerBeanNames.get()).contains("taskScheduler", "multiTenantTaskScheduler");
    assertThat(quartzJobStoreBeanNames.get()).contains("quartzJobStoreTX", "multiTenantQuartzJobStoreTX");
    assertThat(multiTenantTaskSchedulerPrimary.get()).isTrue();
    assertThat(multiTenantQuartzJobStorePrimary.get()).isTrue();
  }

  @Test
  public void shouldPreferMultiTenantUserDirectoryBean() {
    AtomicReference<List<String>> userDirectoryBeanNames = new AtomicReference<>();
    AtomicReference<Boolean> multiTenantPrimary = new AtomicReference<>();
    AtomicReference<Boolean> singleTenantPrimary = new AtomicReference<>();

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      userDirectoryBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(UserDirectory.class)));
      multiTenantPrimary.set(beanFactory.getBeanDefinition("multiTenantUserDirectory").isPrimary());
      singleTenantPrimary.set(beanFactory.getBeanDefinition("userDirectory").isPrimary());
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(userDirectoryBeanNames.get()).contains("multiTenantUserDirectory", "userDirectory");
    assertThat(multiTenantPrimary.get()).isTrue();
    assertThat(singleTenantPrimary.get()).isFalse();
  }

  @Test
  public void shouldMarkMultiTenantSsoUserServiceAsPrimary() {
    AtomicReference<List<String>> ssoUserServiceBeanNames = new AtomicReference<>();
    AtomicReference<Boolean> multiTenantPrimary = new AtomicReference<>();
    AtomicReference<Boolean> singleTenantPrimary = new AtomicReference<>(false);

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      ssoUserServiceBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(SsoUserService.class)));
      multiTenantPrimary.set(beanFactory.getBeanDefinition("multiTenantSsoUserService").isPrimary());
      if (beanFactory.containsBeanDefinition("ssoUserService")) {
        singleTenantPrimary.set(beanFactory.getBeanDefinition("ssoUserService").isPrimary());
      }
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(ssoUserServiceBeanNames.get()).contains("multiTenantSsoUserService");
    assertThat(multiTenantPrimary.get()).isTrue();
    assertThat(singleTenantPrimary.get()).isFalse();
  }

  @Test
  public void shouldPreferMtiqFeaturesServiceBean() {
    AtomicReference<List<String>> featuresServiceBeanNames = new AtomicReference<>();
    AtomicReference<Boolean> multiTenantPrimary = new AtomicReference<>();
    AtomicReference<Boolean> singleTenantPrimary = new AtomicReference<>();

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      featuresServiceBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(FeaturesService.class)));
      multiTenantPrimary.set(beanFactory.getBeanDefinition("MTIQFeatureService").isPrimary());
      singleTenantPrimary.set(beanFactory.getBeanDefinition("featuresService").isPrimary());
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(featuresServiceBeanNames.get()).contains("MTIQFeatureService", "featuresService");
    assertThat(multiTenantPrimary.get()).isTrue();
    assertThat(singleTenantPrimary.get()).isFalse();
  }

  @Test
  public void shouldPreferMtiqSourceControlSshValidatorBean() {
    AtomicReference<List<String>> validatorBeanNames = new AtomicReference<>();
    AtomicReference<Boolean> mtiqPrimary = new AtomicReference<>();
    AtomicReference<Boolean> defaultPrimary = new AtomicReference<>(false);

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      validatorBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(SourceControlSshValidator.class)));
      mtiqPrimary.set(beanFactory.getBeanDefinition("mtiqSourceControlSshValidator").isPrimary());
      if (beanFactory.containsBeanDefinition("defaultSourceControlSshValidator")) {
        defaultPrimary.set(beanFactory.getBeanDefinition("defaultSourceControlSshValidator").isPrimary());
      }
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(validatorBeanNames.get())
        .contains("mtiqSourceControlSshValidator", "defaultSourceControlSshValidator");
    assertThat(mtiqPrimary.get()).isTrue();
    assertThat(defaultPrimary.get()).isFalse();
  }

  @Test
  public void shouldPreferMultiTenantTelemetryCollectorsProviderBean() {
    AtomicReference<List<String>> providerBeanNames = new AtomicReference<>();
    AtomicReference<Boolean> multiTenantPrimary = new AtomicReference<>();
    AtomicReference<Boolean> defaultPrimary = new AtomicReference<>(false);

    SpringApplicationBuilder builder = newBuilder();
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      providerBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(TelemetryCollectorsProvider.class)));
      multiTenantPrimary.set(beanFactory.getBeanDefinition("multiTenantTelemetryCollectorsProvider").isPrimary());
      if (beanFactory.containsBeanDefinition("defaultTelemetryCollectorsProvider")) {
        defaultPrimary.set(beanFactory.getBeanDefinition("defaultTelemetryCollectorsProvider").isPrimary());
      }
      throw new StopAfterBeanCapture();
    }));

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(providerBeanNames.get())
        .contains("multiTenantTelemetryCollectorsProvider", "defaultTelemetryCollectorsProvider");
    assertThat(multiTenantPrimary.get()).isTrue();
    assertThat(defaultPrimary.get()).isFalse();
  }

  @Test
  public void shouldRemoveSingleTenantProductLicenseTestBeanFromMtiqIntegrationContext() {
    DatabaseContainer databaseContainer = mock(DatabaseContainer.class);
    when(databaseContainer.getOperationalDataStore()).thenReturn(mock(OperationalDataStore.class));
    when(databaseContainer.getAggregationDataStore()).thenReturn(mock(AggregationDataStore.class));
    when(databaseContainer.getDataMartDataStore()).thenReturn(mock(DataMartDataStore.class));
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(mock(ThirdPartyScansDataStore.class));

    AtomicReference<List<String>> productLicenseBeanNames = new AtomicReference<>();
    AtomicReference<Boolean> productLicenseBeanPresent = new AtomicReference<>(true);

    TestDatabaseContainerHolder.set(databaseContainer);
    try {
      List<Class<?>> explicitTestConfigurations = MtiqTestHarness.getTestConfigurationClassesForBuilder();

      SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringMultiTenantTestInsightBrainService.class)
          .sources(explicitTestConfigurations.toArray(new Class<?>[0]))
          .profiles("test")
          .properties(
              "spring.main.web-application-type=none",
              "spring.main.lazy-initialization=true",
              "spring.main.allow-bean-definition-overriding=true",
              "server.port=0",
              "management.server.port=0",
              "sonatypeWork=target/test-mtiq-license-scan",
              "clusterDirectory=target/test-mtiq-license-scan-cluster",
              "sonatype.mtiq.enabled=true");
      builder.initializers(ExplicitTestConfigurationSupport.initializer(explicitTestConfigurations));
      builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        productLicenseBeanNames.set(Arrays.asList(beanFactory.getBeanNamesForType(ProductLicense.class, false, false)));
        productLicenseBeanPresent.set(beanFactory.containsBeanDefinition("productLicense"));
        throw new StopAfterBeanCapture();
      }));
      DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

      assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    }
    finally {
      TestDatabaseContainerHolder.clear();
    }

    assertThat(productLicenseBeanPresent.get()).isFalse();
    assertThat(productLicenseBeanNames.get()).contains("multiTenantProductLicense", "defaultProductLicense")
        .doesNotContain("productLicense");
  }

  @TestConfiguration
  static class SupportBeanCaptureConfiguration
  {
    @Bean
    static BeanFactoryPostProcessor supportBeanCapture() {
      return beanFactory -> {
        captureSupportBean(beanFactory, "supportResource", supportResourceBeanPresent, supportResourceDefinition);
        throw new StopAfterBeanCapture();
      };
    }

    private static void captureSupportBean(
        org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory,
        String beanName,
        AtomicReference<Boolean> beanPresent,
        AtomicReference<String> beanDefinitionDescription)
    {
      beanPresent.set(beanFactory.containsBeanDefinition(beanName));
      if (beanFactory.containsBeanDefinition(beanName)) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        beanDefinitionDescription
            .set(beanDefinition.getBeanClassName() + " @ " + beanDefinition.getResourceDescription());
      }
    }
  }

  private SpringApplicationBuilder newBuilder() {
    return SpringMultiTenantTestInsightBrainServiceTestSupport.newBuilder(
        "spring.main.web-application-type=none");
  }

  private Set<String> findCandidates(Class<?> applicationClass) throws ClassNotFoundException {
    ComponentScan componentScan = applicationClass.getAnnotation(ComponentScan.class);

    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
    applyExcludeFilters(scanner, componentScan);

    return Arrays.stream(componentScan.basePackages())
        .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
        .map(BeanDefinition::getBeanClassName)
        .collect(Collectors.toSet());
  }

  private void applyExcludeFilters(
      ClassPathScanningCandidateComponentProvider scanner,
      ComponentScan componentScan) throws ClassNotFoundException
  {
    for (ComponentScan.Filter filter : componentScan.excludeFilters()) {
      if (filter.type() == FilterType.ASSIGNABLE_TYPE) {
        for (Class<?> candidate : filter.classes()) {
          scanner.addExcludeFilter(new AssignableTypeFilter(candidate));
        }
      }
      else if (filter.type() == FilterType.ANNOTATION) {
        for (Class<?> candidate : filter.classes()) {
          @SuppressWarnings("unchecked")
          Class<Annotation> annotationType =
              (Class<Annotation>) candidate;
          scanner.addExcludeFilter(new AnnotationTypeFilter(annotationType));
        }
      }
      else if (filter.type() == FilterType.REGEX) {
        for (String pattern : filter.pattern()) {
          scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(pattern)));
        }
      }
      else if (filter.type() == FilterType.CUSTOM) {
        for (Class<?> candidate : filter.classes()) {
          try {
            scanner.addExcludeFilter((TypeFilter) candidate.getDeclaredConstructor().newInstance());
          }
          catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate custom filter " + candidate.getName(), e);
          }
        }
      }
    }
  }
}

final class SpringMultiTenantTestInsightBrainServiceTestSupport
{
  private SpringMultiTenantTestInsightBrainServiceTestSupport() {
  }

  static SpringApplicationBuilder newBuilder(String... additionalProperties) {
    String[] baseProperties = {
      "spring.main.lazy-initialization=true",
      "spring.main.allow-bean-definition-overriding=true",
      "server.port=0",
      "management.server.port=0",
      "sonatypeWork=target/test-mtiq-scan",
      "clusterDirectory=target/test-mtiq-scan-cluster",
      "sonatype.mtiq.enabled=true"
    };
    String[] mergedProperties = Arrays.copyOf(baseProperties, baseProperties.length + additionalProperties.length);
    System.arraycopy(additionalProperties, 0, mergedProperties, baseProperties.length, additionalProperties.length);

    SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringMultiTenantTestInsightBrainService.class)
        .profiles("test")
        .properties(mergedProperties);
    builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of()));
    DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);
    return builder;
  }
}

class StopAfterBeanCapture
    extends RuntimeException
{
}

class MtiqTestHarness
    extends AbstractMultiTenantBaseIntegrationTest
{
  static List<Class<?>> getTestConfigurationClassesForBuilder() {
    return new MtiqTestHarness().getTestConfigurationClasses();
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // no-op
  }
}
