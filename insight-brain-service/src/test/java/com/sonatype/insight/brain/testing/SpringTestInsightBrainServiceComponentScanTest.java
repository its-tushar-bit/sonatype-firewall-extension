/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesResource;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.TestInsightBrainService;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.spring.DropwizardConfigBootstrap;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public class SpringTestInsightBrainServiceComponentScanTest
{
  private static final String POLICY_EVALUATION_TEST_CONFIGURATION =
      "com.sonatype.insight.brain.policy.evaluator.AbstractPolicyEvaluationTest$PolicyEvaluationTestConfiguration";

  private static final String TASK_SCHEDULER_TEST_CONFIGURATION =
      "com.sonatype.insight.brain.scheduler.TaskSchedulerTest$TaskSchedulerTestConfiguration";

  private static final String DATA_STORE_TEST_MODULE =
      "com.sonatype.insight.brain.testing.DataStoreTestModule";

  private static final String API_CONFIGURATION_TEST_LISTENER =
      "com.sonatype.insight.brain.api.v2.service.ApiConfigurationServiceTest$TestConfigurationListener";

  @Test
  public void shouldExcludeNestedTestConfigurationsFromAmbientComponentScan() throws ClassNotFoundException {
    Set<String> candidateClassNames = findCandidates(SpringTestInsightBrainService.class);

    assertThat(candidateClassNames)
        .contains("com.sonatype.insight.brain.validation.DefaultSourceControlSshValidator");
    assertDoesNotContainEquivalentClassName(candidateClassNames, POLICY_EVALUATION_TEST_CONFIGURATION);
    assertDoesNotContainEquivalentClassName(candidateClassNames, TASK_SCHEDULER_TEST_CONFIGURATION);
    assertDoesNotContainEquivalentClassName(candidateClassNames, DATA_STORE_TEST_MODULE);
  }

  @Test
  public void shouldOnlyRegisterDefaultSourceControlSshValidatorInLazyIntegrationContext() {
    AtomicReference<String[]> beanNames = new AtomicReference<>();

    SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
        .sources(TestHelperModule.class,
            AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=true",
            "spring.main.allow-bean-definition-overriding=true",
            "server.port=0",
            "management.server.port=0",
            "sonatype.work.dir=target/test-brain-scan");
    builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
        TestHelperModule.class,
        AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      beanNames.set(beanFactory.getBeanNamesForType(SourceControlSshValidator.class, false, false));
      throw new StopAfterBeanCapture();
    }));
    DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(beanNames.get()).containsExactly("defaultSourceControlSshValidator");
  }

  @Test
  public void shouldRegisterRepresentativeNamedBeansInLazyIntegrationContext() {
    AtomicReference<List<String>> apiConfigFeaturesServiceBeanNames = new AtomicReference<>();
    AtomicReference<List<String>> apiConfigFeaturesResourceBeanNames = new AtomicReference<>();
    AtomicReference<List<String>> samlConfigurationServiceBeanNames = new AtomicReference<>();

    SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
        .sources(TestHelperModule.class,
            AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=true",
            "spring.main.allow-bean-definition-overriding=true",
            "server.port=0",
            "management.server.port=0",
            "sonatype.work.dir=target/test-brain-named-scan");
    builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
        TestHelperModule.class,
        AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      apiConfigFeaturesServiceBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(ApiConfigFeaturesService.class, false, false)));
      apiConfigFeaturesResourceBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(ApiConfigFeaturesResource.class, false, false)));
      samlConfigurationServiceBeanNames.set(Arrays.asList(
          beanFactory.getBeanNamesForType(SamlConfigurationService.class, false, false)));
      throw new StopAfterBeanCapture();
    }));
    DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(apiConfigFeaturesServiceBeanNames.get()).containsExactly("apiConfigFeaturesService");
    assertThat(apiConfigFeaturesResourceBeanNames.get()).containsExactly("apiConfigFeaturesResource");
    assertThat(samlConfigurationServiceBeanNames.get()).containsExactly("samlConfigurationService");
  }

  @Test
  public void shouldRegisterNestedNamedTestBeansInLazyIntegrationContext() {
    AtomicReference<Boolean> testConfigurationListenerPresent = new AtomicReference<>(false);

    SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
        .sources(TestHelperModule.class,
            AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=true",
            "spring.main.allow-bean-definition-overriding=true",
            "server.port=0",
            "management.server.port=0",
            "sonatype.work.dir=target/test-brain-nested-named-scan");
    builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
        TestHelperModule.class,
        AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      testConfigurationListenerPresent.set(beanFactory.containsBeanDefinition(API_CONFIGURATION_TEST_LISTENER));
      throw new StopAfterBeanCapture();
    }));
    DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(testConfigurationListenerPresent.get()).isTrue();
  }

  @Test
  public void shouldExposeOnlyOnePrimaryProductLicenseBeanInLazyIntegrationContext() {
    AtomicReference<List<String>> productLicenseBeanNames = new AtomicReference<>();
    AtomicReference<Map<String, Boolean>> productLicensePrimaries = new AtomicReference<>();

    SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
        .sources(TestHelperModule.class,
            AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
        .profiles("test")
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.lazy-initialization=true",
            "spring.main.allow-bean-definition-overriding=true",
            "server.port=0",
            "management.server.port=0",
            "sonatype.work.dir=target/test-brain-product-license-scan");
    builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
        TestHelperModule.class,
        AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
    builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
      String[] beanNames = beanFactory.getBeanNamesForType(ProductLicense.class, false, false);
      productLicenseBeanNames.set(Arrays.asList(beanNames));
      Map<String, Boolean> primaries = new LinkedHashMap<>();
      for (String beanName : beanNames) {
        primaries.put(beanName, beanFactory.getBeanDefinition(beanName).isPrimary());
      }
      productLicensePrimaries.set(primaries);
      throw new StopAfterBeanCapture();
    }));
    DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

    assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    assertThat(productLicenseBeanNames.get()).doesNotContain("testProductLicense");
    assertThat(productLicensePrimaries.get())
        .containsEntry("productLicense", true)
        .containsEntry("defaultProductLicense", false);
  }

  @Test
  public void shouldExposeSamlConfigurationServiceInServletIntegrationContext() {
    DatabaseContainer databaseContainer = mock(DatabaseContainer.class);
    when(databaseContainer.getOperationalDataStore()).thenReturn(mock(OperationalDataStore.class));
    when(databaseContainer.getAggregationDataStore()).thenReturn(mock(AggregationDataStore.class));
    when(databaseContainer.getDataMartDataStore()).thenReturn(mock(DataMartDataStore.class));
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(mock(ThirdPartyScansDataStore.class));

    AtomicReference<Map<String, String>> samlConfigurationServiceBeans = new AtomicReference<>();

    TestDatabaseContainerHolder.set(databaseContainer);
    try {
      SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
          .sources(TestHelperModule.class,
              AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
          .profiles("test")
          .properties(
              "spring.main.lazy-initialization=true",
              "spring.main.allow-bean-definition-overriding=true",
              "server.port=0",
              "management.server.port=0",
              "sonatype.work.dir=target/test-brain-servlet-saml-scan");
      builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
          TestHelperModule.class,
          AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
      builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        samlConfigurationServiceBeans.set(describeBeans(beanFactory, SamlConfigurationService.class));
        throw new StopAfterBeanCapture();
      }));
      DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

      assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    }
    finally {
      TestDatabaseContainerHolder.clear();
    }

    assertThat(samlConfigurationServiceBeans.get())
        .as("SamlConfigurationService beans: %s", samlConfigurationServiceBeans.get())
        .containsOnlyKeys("samlConfigurationService");
  }

  @Test
  public void shouldNotExposeAmbientPrimaryTestBeansInServletIntegrationContext() {
    DatabaseContainer databaseContainer = mock(DatabaseContainer.class);
    when(databaseContainer.getOperationalDataStore()).thenReturn(mock(OperationalDataStore.class));
    when(databaseContainer.getAggregationDataStore()).thenReturn(mock(AggregationDataStore.class));
    when(databaseContainer.getDataMartDataStore()).thenReturn(mock(DataMartDataStore.class));
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(mock(ThirdPartyScansDataStore.class));

    AtomicReference<Map<String, String>> productLicenseBeans = new AtomicReference<>();
    AtomicReference<Map<String, String>> shutdownHandlerBeans = new AtomicReference<>();
    AtomicReference<Set<String>> strayConfigurationBeans = new AtomicReference<>();

    TestDatabaseContainerHolder.set(databaseContainer);
    try {
      SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
          .sources(TestHelperModule.class,
              AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
          .profiles("test")
          .properties(
              "spring.main.lazy-initialization=true",
              "spring.main.allow-bean-definition-overriding=true",
              "server.port=0",
              "management.server.port=0",
              "sonatype.work.dir=target/test-brain-servlet-primary-scan");
      builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
          TestHelperModule.class,
          AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
      builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        productLicenseBeans.set(describeBeans(beanFactory, ProductLicense.class));
        shutdownHandlerBeans.set(describeBeans(beanFactory, ShutdownHandler.class));
        strayConfigurationBeans.set(Arrays.stream(beanFactory.getBeanDefinitionNames())
            .filter(beanName -> beanName.equals("springTestConfiguration")
                || beanName.equals("singleTenantTestSupportConfiguration")
                || beanName.endsWith("PolicyEvaluateServiceTestConfiguration")
                || beanName.endsWith("ApiComponentEvaluationServiceV2TestConfiguration")
                || beanName.endsWith("TaskSchedulerTestConfiguration"))
            .collect(Collectors.toSet()));
        throw new StopAfterBeanCapture();
      }));
      DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

      assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    }
    finally {
      TestDatabaseContainerHolder.clear();
    }

    assertThat(productLicenseBeans.get())
        .as("ProductLicense beans: %s", productLicenseBeans.get())
        .doesNotContainKey("testProductLicense");
    assertThat(shutdownHandlerBeans.get())
        .as("ShutdownHandler beans: %s", shutdownHandlerBeans.get())
        .containsOnlyKeys("shutdownHandler");
    assertThat(strayConfigurationBeans.get())
        .as("Stray configuration beans: %s", strayConfigurationBeans.get())
        .isEmpty();
  }

  @Test
  public void shouldRemoveAmbientDataStoreTestModuleBeanDefinitionsBeforeRefresh() {
    DatabaseContainer databaseContainer = mock(DatabaseContainer.class);
    when(databaseContainer.getOperationalDataStore()).thenReturn(mock(OperationalDataStore.class));
    when(databaseContainer.getAggregationDataStore()).thenReturn(mock(AggregationDataStore.class));
    when(databaseContainer.getDataMartDataStore()).thenReturn(mock(DataMartDataStore.class));
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(mock(ThirdPartyScansDataStore.class));

    AtomicReference<Set<String>> beanNames = new AtomicReference<>();

    TestDatabaseContainerHolder.set(databaseContainer);
    try {
      SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
          .sources(TestHelperModule.class,
              AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
          .profiles("test")
          .properties(
              "spring.main.web-application-type=none",
              "spring.main.lazy-initialization=true",
              "spring.main.allow-bean-definition-overriding=true",
              "server.port=0",
              "management.server.port=0",
              "sonatype.work.dir=target/test-brain-lock-scan");
      builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
          TestHelperModule.class,
          AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
      builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        beanNames.set(Arrays.stream(beanFactory.getBeanDefinitionNames())
            .filter(beanName -> {
              BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
              String resourceDescription = beanDefinition.getResourceDescription();
              return resourceDescription != null && resourceDescription.contains("DataStoreTestModule");
            })
            .collect(Collectors.toSet()));
        throw new StopAfterBeanCapture();
      }));
      DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

      assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    }
    finally {
      TestDatabaseContainerHolder.clear();
    }

    assertThat(beanNames.get()).isEmpty();
  }

  @Test
  public void shouldRestoreClusterLockManagerDefinitionAfterRemovingAmbientDataStoreOverrides() {
    DatabaseContainer databaseContainer = mock(DatabaseContainer.class);
    when(databaseContainer.getOperationalDataStore()).thenReturn(mock(OperationalDataStore.class));
    when(databaseContainer.getAggregationDataStore()).thenReturn(mock(AggregationDataStore.class));
    when(databaseContainer.getDataMartDataStore()).thenReturn(mock(DataMartDataStore.class));
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(mock(ThirdPartyScansDataStore.class));

    AtomicReference<Boolean> clusterLockManagerPresent = new AtomicReference<>(false);

    TestDatabaseContainerHolder.set(databaseContainer);
    try {
      SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
          .sources(TestHelperModule.class,
              AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
          .profiles("test")
          .properties(
              "spring.main.web-application-type=none",
              "spring.main.lazy-initialization=true",
              "spring.main.allow-bean-definition-overriding=true",
              "server.port=0",
              "management.server.port=0",
              "sonatype.work.dir=target/test-brain-lock-restore");
      builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
          TestHelperModule.class,
          AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
      builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        clusterLockManagerPresent.set(beanFactory.containsBeanDefinition("clusterLockManager"));
        throw new StopAfterBeanCapture();
      }));
      DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

      assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    }
    finally {
      TestDatabaseContainerHolder.clear();
    }

    assertThat(clusterLockManagerPresent.get()).isTrue();
  }

  @Test
  public void shouldRemovePlainTestConfigurationBeanDefinitionsBeforeRefresh() {
    DatabaseContainer databaseContainer = mock(DatabaseContainer.class);
    when(databaseContainer.getOperationalDataStore()).thenReturn(mock(OperationalDataStore.class));
    when(databaseContainer.getAggregationDataStore()).thenReturn(mock(AggregationDataStore.class));
    when(databaseContainer.getDataMartDataStore()).thenReturn(mock(DataMartDataStore.class));
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(mock(ThirdPartyScansDataStore.class));

    AtomicReference<Set<String>> beanNames = new AtomicReference<>();

    TestDatabaseContainerHolder.set(databaseContainer);
    try {
      SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
          .sources(TestHelperModule.class,
              AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
          .profiles("test")
          .properties(
              "spring.main.web-application-type=none",
              "spring.main.lazy-initialization=true",
              "spring.main.allow-bean-definition-overriding=true",
              "server.port=0",
              "management.server.port=0",
              "sonatype.work.dir=target/test-brain-plain-config-scan");
      builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
          TestHelperModule.class,
          AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
      builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        beanNames.set(Arrays.stream(beanFactory.getBeanDefinitionNames())
            .filter(beanName -> {
              BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
              String resourceDescription = beanDefinition.getResourceDescription();
              return resourceDescription != null && resourceDescription.contains("RealmCachingDisabledTest")
                  && !beanName.endsWith(".RealmBeans");
            })
            .collect(Collectors.toSet()));
        throw new StopAfterBeanCapture();
      }));
      DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

      assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    }
    finally {
      TestDatabaseContainerHolder.clear();
    }

    assertThat(beanNames.get()).isEmpty();
  }

  @Test
  public void shouldRetainTestDatabaseOperationalDataStoreDefinitionWhenFilteringAmbientTestConfigurations() {
    DatabaseContainer databaseContainer = mock(DatabaseContainer.class);
    OperationalDataStore operationalDataStore = mock(OperationalDataStore.class);
    when(databaseContainer.getOperationalDataStore()).thenReturn(operationalDataStore);
    when(databaseContainer.getAggregationDataStore()).thenReturn(mock(AggregationDataStore.class));
    when(databaseContainer.getDataMartDataStore()).thenReturn(mock(DataMartDataStore.class));
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(mock(ThirdPartyScansDataStore.class));

    AtomicReference<String[]> beanNames = new AtomicReference<>();
    AtomicReference<String> beanSource = new AtomicReference<>();

    TestDatabaseContainerHolder.set(databaseContainer);
    try {
      SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class)
          .sources(TestHelperModule.class,
              AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)
          .profiles("test")
          .properties(
              "spring.main.web-application-type=none",
              "spring.main.lazy-initialization=true",
              "spring.main.allow-bean-definition-overriding=true",
              "server.port=0",
              "management.server.port=0",
              "sonatype.work.dir=target/test-brain-db-scan");
      builder.initializers(ExplicitTestConfigurationSupport.initializer(List.of(
          TestHelperModule.class,
          AbstractBaseIntegrationTest.BaseIntegrationTestConfigurationWithTestEncryptionKeyStore.class)));
      builder.initializers(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        beanNames.set(beanFactory.getBeanNamesForType(OperationalDataStore.class, false, false));
        if (beanFactory.containsBeanDefinition("operationalDataStore")) {
          beanSource.set(beanFactory.getBeanDefinition("operationalDataStore").getResourceDescription());
        }
        throw new StopAfterBeanCapture();
      }));
      DropwizardConfigBootstrap.configure(builder, TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH);

      assertThatThrownBy(builder::run).isInstanceOf(StopAfterBeanCapture.class);
    }
    finally {
      TestDatabaseContainerHolder.clear();
    }

    if (beanNames.get() != null && beanNames.get().length > 0) {
      assertThat(beanNames.get()).contains("operationalDataStore");
    }
    if (beanSource.get() != null) {
      assertThat(beanSource.get()).doesNotContain("TaskSchedulerTest");
    }
  }

  private <T> Map<String, String> describeBeans(
      org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory,
      Class<T> beanType)
  {
    Map<String, String> descriptions = new LinkedHashMap<>();
    for (String beanName : beanFactory.getBeanNamesForType(beanType, false, false)) {
      BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
      descriptions.put(beanName,
          "primary=" + beanDefinition.isPrimary() + ", resource=" + beanDefinition.getResourceDescription()
              + ", beanClass=" + beanDefinition.getBeanClassName());
    }
    return descriptions;
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

  private void assertDoesNotContainEquivalentClassName(Set<String> candidateClassNames, String binaryClassName) {
    assertThat(candidateClassNames)
        .doesNotContain(binaryClassName)
        .doesNotContain(binaryClassName.replace('$', '.'));
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

  private static class StopAfterBeanCapture
      extends RuntimeException
  {
  }
}
