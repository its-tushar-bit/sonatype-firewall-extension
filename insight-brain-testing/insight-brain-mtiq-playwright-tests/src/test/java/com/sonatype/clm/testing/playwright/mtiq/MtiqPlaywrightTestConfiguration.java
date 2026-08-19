/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.mtiq;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ApiSupplier;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.jira.JiraService;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.TestMultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.service.SpringMultiTenantTestInsightBrainService;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Merges the single-tenant Playwright overrides with the MTIQ integration-test overrides
 * ({@code AbstractMultiTenantBaseIntegrationTest.MtiqTestConfiguration}).
 *
 * <p>
 * Extracted from {@link AbstractMtiqUiTest} to keep the base test class focused on test
 * lifecycle. The static test infrastructure fields (license manager, jira mock, tenant util, ...)
 * still live on {@link AbstractMtiqUiTest}; the {@code @Bean} methods below reference them by
 * qualified name.
 */
@org.springframework.context.annotation.Configuration
class MtiqPlaywrightTestConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(MtiqPlaywrightTestConfiguration.class);

  /**
   * Ambient test {@code @Configuration} classes on the classpath override production
   * {@code @Import}ed {@code @Bean}s (e.g. {@code objectMapper}, {@code insightConfig});
   * {@code ExplicitTestConfigurationSupport} strips them but doesn't restore imports. This
   * post-processor walks the production imports on
   * {@link SpringMultiTenantTestInsightBrainService} and re-registers any missing beans,
   * preserving {@code @Primary} and re-supplying {@code @Value} placeholders.
   */
  @Bean
  static BeanDefinitionRegistryPostProcessor restoreDisplacedProductionBeans() {
    return new BeanDefinitionRegistryPostProcessor()
    {
      @Override
      public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        if (registry.containsBeanDefinition("productLicense")) {
          registry.removeBeanDefinition("productLicense");
        }
        removeLeakedAdminCompatibilityBeans(registry);
      }

      @Override
      public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry registry) {
          restoreMissingImportedProductionBeans(registry);
        }
      }
    };
  }

  /**
   * {@code AdminCompatibilityConfiguration} is a {@code ManagementContextConfiguration(CHILD)}
   * meant for the admin child context, but the test classpath drags it into the main context. Its
   * {@code adminIndexServlet} maps to "/" and steals the DispatcherServlet default, hiding the SPA.
   * Strip its bean definitions from the main context; the admin child context still loads its own copy.
   */
  private static void removeLeakedAdminCompatibilityBeans(final BeanDefinitionRegistry registry) {
    for (String beanName : registry.getBeanDefinitionNames()) {
      BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
      String resourceDescription = beanDefinition.getResourceDescription();
      String factoryBeanName = beanDefinition.getFactoryBeanName();
      boolean fromAdminConfig =
          (resourceDescription != null && resourceDescription.contains("AdminCompatibilityConfiguration"))
              || (factoryBeanName != null && factoryBeanName.toLowerCase(java.util.Locale.ROOT)
                  .contains("admincompatibilityconfiguration"));
      if (fromAdminConfig) {
        log.warn("[MTIQ-RESTORE] removing leaked admin-context bean '{}' (source={}) from main context",
            beanName, resourceDescription);
        registry.removeBeanDefinition(beanName);
      }
    }
    // adminIndexServlet is the one mapped to "/" that shadows the SPA; remove by name as a fallback
    // in case its definition does not carry an AdminCompatibilityConfiguration resource description.
    if (registry.containsBeanDefinition("adminIndexServlet")) {
      log.warn("[MTIQ-RESTORE] removing leaked 'adminIndexServlet' from main context");
      registry.removeBeanDefinition("adminIndexServlet");
    }
  }

  private static void restoreMissingImportedProductionBeans(final BeanDefinitionRegistry registry) {
    Import importAnnotation = SpringMultiTenantTestInsightBrainService.class.getAnnotation(Import.class);
    if (importAnnotation == null) {
      log.warn("[MTIQ-RESTORE] No @Import found on SpringMultiTenantTestInsightBrainService");
      return;
    }
    logSearchIndexClientState(registry, "restorer-entry");
    for (Class<?> configurationClass : importAnnotation.value()) {
      restoreMissingBeansFromConfiguration(registry, configurationClass);
    }
    logSearchIndexClientState(registry, "restorer-exit");
  }

  private static void logSearchIndexClientState(final BeanDefinitionRegistry registry, final String phase) {
    if (!registry.containsBeanDefinition("searchIndexClient")) {
      log.debug("[MTIQ-RESTORE] {}: searchIndexClient ABSENT", phase);
      return;
    }
    BeanDefinition bd = registry.getBeanDefinition("searchIndexClient");
    log.debug("[MTIQ-RESTORE] {}: searchIndexClient present source={} factoryBean={} factoryMethod={} ambient={}",
        phase, bd.getResourceDescription(), bd.getFactoryBeanName(), bd.getFactoryMethodName(),
        isAmbientTestOverride(bd));
  }

  private static void restoreMissingBeansFromConfiguration(
      final BeanDefinitionRegistry registry,
      final Class<?> configurationClass)
  {
    String configurationBeanName = findConfigurationBeanName(registry, configurationClass);
    for (Method method : configurationClass.getDeclaredMethods()) {
      Bean beanAnnotation = method.getAnnotation(Bean.class);
      if (beanAnnotation == null || hasConditionalAnnotation(method)) {
        continue;
      }
      String beanName = resolveBeanName(beanAnnotation, method);
      if (registry.containsBeanDefinition(beanName)) {
        // The bean is present. Keep it if it is the production definition or an intentional test
        // override (from MtiqPlaywrightTestConfiguration/TestDatabaseConfiguration). Only replace
        // when an ambient test @Configuration (e.g. LuceneSearchServiceTest's nested
        // @TestConfiguration) has shadowed the production @Bean and survived cleanup.
        if (!isAmbientTestOverride(registry.getBeanDefinition(beanName))) {
          continue;
        }
        log.warn("[MTIQ-RESTORE] replacing ambient test override of bean '{}' with production {}#{}",
            beanName, configurationClass.getSimpleName(), method.getName());
      }
      boolean isStatic = Modifier.isStatic(method.getModifiers());
      if (!isStatic && configurationBeanName == null) {
        // Instance @Bean method whose owning @Configuration bean is absent: cannot rebuild it.
        continue;
      }
      RootBeanDefinition definition = new RootBeanDefinition();
      if (isStatic) {
        definition.setBeanClassName(configurationClass.getName());
      }
      else {
        definition.setFactoryBeanName(configurationBeanName);
      }
      definition.setAutowireCandidate(true);
      // Production @Bean definitions (ConfigurationClassBeanDefinition) use AUTOWIRE_CONSTRUCTOR so
      // Spring autowires factory-method parameters. A rebuilt RootBeanDefinition defaults to
      // AUTOWIRE_NO, which leaves the parameters unresolved ("Ambiguous argument values").
      definition.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
      if (method.isAnnotationPresent(Primary.class)) {
        definition.setPrimary(true);
      }
      if (isUniqueMethodName(configurationClass, method.getName())) {
        // Mirror ConfigurationClassBeanDefinitionReader: marking the factory method unique lets
        // Spring bind and autowire its parameters directly (honoring @Value/@Autowired/@Qualifier),
        // which a plain factoryMethodName does not (it forces a candidate search that fails to
        // autowire with "Ambiguous argument values").
        definition.setUniqueFactoryMethodName(method.getName());
      }
      else {
        // Overloaded @Bean method: cannot mark unique. Disambiguate by supplying the @Value
        // arguments explicitly so Spring can match the intended overload by argument count/type.
        definition.setFactoryMethodName(method.getName());
        ConstructorArgumentValues valueArguments = buildValueArguments(method);
        if (valueArguments != null) {
          definition.setConstructorArgumentValues(valueArguments);
        }
      }
      registry.registerBeanDefinition(beanName, definition);
    }
  }

  private static String findConfigurationBeanName(
      final BeanDefinitionRegistry registry,
      final Class<?> configurationClass)
  {
    // The @Configuration bean itself has no factory method; skip its @Bean factory-method
    // definitions (whose beanClassName is also the declaring @Configuration class, e.g. static
    // @Bean methods). The class name may be CGLIB-enhanced (Foo$$SpringCGLIB$$0) once
    // ConfigurationClassPostProcessor has run, so match that form too.
    String className = configurationClass.getName();
    for (String beanName : registry.getBeanDefinitionNames()) {
      BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
      if (beanDefinition.getFactoryMethodName() != null) {
        continue;
      }
      String beanClassName = beanDefinition.getBeanClassName();
      if (beanClassName != null
          && (beanClassName.equals(className) || beanClassName.startsWith(className + "$$")))
      {
        return beanName;
      }
    }
    String fallback = Introspector.decapitalize(configurationClass.getSimpleName());
    if (registry.containsBeanDefinition(fallback)
        && registry.getBeanDefinition(fallback).getFactoryMethodName() == null)
    {
      return fallback;
    }
    return null;
  }

  /**
   * A bean definition originating from a test class (name ends {@code Test}/{@code IT}, or from
   * {@code -tests.jar} / {@code test-classes}) other than this harness's own configurations.
   */
  private static boolean isAmbientTestOverride(final BeanDefinition beanDefinition) {
    String source = beanDefinitionSourceClassName(beanDefinition);
    if (source == null) {
      return false;
    }
    if (source.contains("AbstractMtiqUiTest")
        || source.contains("MtiqPlaywrightTestConfiguration")
        || source.contains("TestDatabaseConfiguration"))
    {
      return false;
    }
    String resourceDescription = beanDefinition.getResourceDescription();
    if (resourceDescription != null
        && (resourceDescription.contains("-tests.jar") || resourceDescription.contains("/test-classes/")))
    {
      return true;
    }
    return source.matches(".*(Test|IT)([.$].*)?$");
  }

  private static String beanDefinitionSourceClassName(final BeanDefinition beanDefinition) {
    String beanClassName = beanDefinition.getBeanClassName();
    if (beanClassName != null) {
      return beanClassName;
    }
    String resourceDescription = beanDefinition.getResourceDescription();
    if (resourceDescription == null) {
      return null;
    }
    int open = resourceDescription.indexOf('[');
    int close = resourceDescription.lastIndexOf(".class]");
    if (open < 0 || close < 0 || close <= open) {
      return null;
    }
    return resourceDescription.substring(open + 1, close).replace('/', '.');
  }

  private static String resolveBeanName(final Bean beanAnnotation, final Method method) {
    for (String candidate : beanAnnotation.name()) {
      if (!candidate.isEmpty()) {
        return candidate;
      }
    }
    for (String candidate : beanAnnotation.value()) {
      if (!candidate.isEmpty()) {
        return candidate;
      }
    }
    return method.getName();
  }

  private static boolean isUniqueMethodName(final Class<?> configurationClass, final String methodName) {
    int count = 0;
    for (Method candidate : configurationClass.getDeclaredMethods()) {
      if (candidate.getName().equals(methodName)) {
        count++;
      }
    }
    return count == 1;
  }

  private static boolean hasConditionalAnnotation(final Method method) {
    for (Annotation annotation : method.getAnnotations()) {
      String annotationName = annotation.annotationType().getName();
      if (annotationName.startsWith("org.springframework.context.annotation.Conditional")
          || annotationName.equals("org.springframework.context.annotation.Profile")
          || annotationName.startsWith("org.springframework.boot.autoconfigure.condition."))
      {
        return true;
      }
    }
    return false;
  }

  private static ConstructorArgumentValues buildValueArguments(final Method method) {
    ConstructorArgumentValues arguments = new ConstructorArgumentValues();
    Parameter[] parameters = method.getParameters();
    boolean hasValueParameter = false;
    for (int index = 0; index < parameters.length; index++) {
      Value valueAnnotation = parameters[index].getAnnotation(Value.class);
      if (valueAnnotation != null) {
        arguments.addIndexedArgumentValue(index, valueAnnotation.value());
        hasValueParameter = true;
      }
    }
    return hasValueParameter ? arguments : null;
  }

  // The single-tenant CoreConfiguration MetricRegistry/HealthCheckRegistry beans are not present
  // in the embedded MTIQ context; supply them here, matching the MTIQ Spring config tests
  // (e.g. MtiqAdminFilterConfigurationTest).
  @Bean
  public com.codahale.metrics.MetricRegistry metricRegistry() {
    return new com.codahale.metrics.MetricRegistry();
  }

  @Bean
  public com.codahale.metrics.health.HealthCheckRegistry healthCheckRegistry() {
    return new com.codahale.metrics.health.HealthCheckRegistry();
  }

  @Bean
  public org.sonatype.licensing.product.ProductLicenseManager productLicenseManager() {
    return AbstractMtiqUiTest.productLicenseManager;
  }

  @Bean
  public org.sonatype.licensing.product.util.LicenseFingerprinter licenseFingerprinter() {
    return AbstractMtiqUiTest.licenseFingerprinter;
  }

  @Bean
  @Primary
  public JiraService jiraService() {
    return AbstractMtiqUiTest.jiraService;
  }

  @Bean
  @Primary
  public DeveloperEnablementService developerEnablementService() {
    return AbstractMtiqUiTest.mockDeveloperEnablementService;
  }

  @Bean
  @Primary
  public MultiTenantAuth0ApiSupplier multiTenantAuth0ApiSupplier() {
    return AbstractMtiqUiTest.auth0ApiSupplier;
  }

  @Bean
  @Primary
  public EncryptionKeyStore encryptionKeyStore() {
    return new TestMultiTenantEncryptionKeyStore();
  }

  /** Supplies the type-injected {@code MultiTenantEncryptionKeyStore} so the AWS-backed one is never built. */
  @Bean
  public com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore multiTenantEncryptionKeyStore() {
    TestMultiTenantEncryptionKeyStore delegate = new TestMultiTenantEncryptionKeyStore();
    com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore keyStore =
        mock(com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore.class);
    lenient().when(keyStore.getKey()).thenAnswer(invocation -> delegate.getKey());
    return keyStore;
  }

  /** Mocked so the real AWS SDK client (which fails without an AWS region) is never built. */
  @Bean
  @Primary
  public com.sonatype.insight.brain.clients.AwsSecretsManagerClient awsSecretsManagerClient() {
    return mock(com.sonatype.insight.brain.clients.AwsSecretsManagerClient.class);
  }

  @Bean
  @Primary
  public TenantUtil tenantUtil() {
    return AbstractMtiqUiTest.tenantUtil;
  }

  @Bean
  @Primary
  public com.sonatype.insight.brain.shutdown.ShutdownHandler shutdownHandler() {
    return spy(new com.sonatype.insight.brain.shutdown.TestShutdownHandler());
  }

  // NOTE: BaseUrl and BaseUrlFilter are production @Named components; do NOT override them here.
  // A forced BaseUrlConfiguration("http://localhost/", true) makes the server redirect to a
  // portless http://localhost/ (connection refused in the browser). The real BaseUrl derives from
  // the per-tenant BASE_URL configured in beforeTest() (the reverse-proxy URL, with port).

  @Bean
  @Primary
  public MultiTenantJwkProvider multiTenantJwkProvider() {
    return mock(MultiTenantJwkProvider.class);
  }

  @Bean
  @Primary
  public JwtHttpAuthorizationFilter jwtHttpAuthorizationFilter(MultiTenantJwkProvider multiTenantJwkProvider) {
    return new JwtHttpAuthorizationFilter(multiTenantJwkProvider);
  }
}
