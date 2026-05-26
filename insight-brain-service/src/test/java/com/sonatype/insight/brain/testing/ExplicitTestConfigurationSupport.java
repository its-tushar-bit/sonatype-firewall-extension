/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.H2ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.PostgresAdvisoryLockDAO;
import com.sonatype.insight.brain.dataaccess.lock.PostgresClusterLockManager;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import jakarta.inject.Named;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

public final class ExplicitTestConfigurationSupport
{
  private static final Pattern CLASS_RESOURCE_DESCRIPTION = Pattern.compile(".*\\[(.+)\\.class\\].*");

  private static final Pattern NESTED_TEST_CONFIGURATION_CLASS = Pattern.compile(".*(Test|IT)([$.]).+");

  private static final Pattern TEST_SUPPORT_CLASS = Pattern.compile(".*\\.Test[^.$]*(?:[.$].*)?$");

  private static final Set<String> AMBIENT_TEST_SUPPORT_BEAN_NAMES = Set.of(
      "dataStoreTestModule",
      "testProductLicense",
      "testProductLicenseManager",
      "testProductLicenseDetailsCache",
      "testQuartzJobStoreTx",
      "testTaskScheduler");

  private static final String NAMED_BEAN_BASE_PACKAGE = "com.sonatype.insight.brain";

  private ExplicitTestConfigurationSupport() {
    // utility class
  }

  public static ApplicationContextInitializer<ConfigurableApplicationContext> initializer(
      List<Class<?>> explicitTestConfigurations)
  {
    Set<String> allowedConfigurationClassNames = collectAllowedConfigurationClassNames(explicitTestConfigurations);

    // Cleanup must run in BOTH post-processor phases - do not collapse into a single guarded
    // execution. postProcessBeanDefinitionRegistry runs before ConfigurationClassPostProcessor,
    // so @TestConfiguration classes discovered during @ComponentScan/@Import processing register
    // their @Bean methods only after the first pass. The second pass in postProcessBeanFactory
    // catches those late-registered ambient beans (e.g. testProductLicense from
    // SingleTenantTestSupportConfiguration) and restores any production @Named beans they
    // displaced.
    return context -> context.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor()
    {
      @Override
      public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        if (!(registry instanceof ConfigurableListableBeanFactory beanFactory)) {
          return;
        }

        Set<String> removedBeanNames =
            removeAmbientTestBeanDefinitions(beanFactory, registry, allowedConfigurationClassNames);

        restoreTestDatabaseBeanDefinitions(beanFactory, registry, removedBeanNames);
        restoreNamedBeanDefinitions(registry);
        restoreNestedNamedTestBeanDefinitions(registry);
      }

      @Override
      public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
          return;
        }

        Set<String> removedBeanNames =
            removeAmbientTestBeanDefinitions(beanFactory, registry, allowedConfigurationClassNames);
        if (removedBeanNames.isEmpty()) {
          return;
        }

        restoreTestDatabaseBeanDefinitions(beanFactory, registry, removedBeanNames);
        restoreNamedBeanDefinitions(registry);
        restoreNestedNamedTestBeanDefinitions(registry);
      }
    });
  }

  private static Set<String> removeAmbientTestBeanDefinitions(
      ConfigurableListableBeanFactory beanFactory,
      BeanDefinitionRegistry registry,
      Set<String> allowedConfigurationClassNames)
  {
    Set<String> removedBeanNames = new LinkedHashSet<>();
    for (String beanName : Arrays.asList(beanFactory.getBeanDefinitionNames())) {
      if (!registry.containsBeanDefinition(beanName)) {
        continue;
      }

      BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
      if (shouldRemoveAmbientTestBeanDefinition(beanName, beanDefinition, allowedConfigurationClassNames)) {
        registry.removeBeanDefinition(beanName);
        removedBeanNames.add(beanName);
      }
    }
    return removedBeanNames;
  }

  private static Set<String> collectAllowedConfigurationClassNames(List<Class<?>> explicitTestConfigurations) {
    Set<String> allowedClassNames = new LinkedHashSet<>();

    for (Class<?> configurationClass : explicitTestConfigurations) {
      for (Class<?> candidate = configurationClass; candidate != null && candidate != Object.class; candidate =
          candidate.getSuperclass())
      {
        if (candidate.isAnnotationPresent(TestConfiguration.class)
            || candidate.isAnnotationPresent(Configuration.class))
        {
          allowedClassNames.add(candidate.getName());
          allowedClassNames.add(candidate.getName().replace('$', '.'));
        }
      }
    }

    return allowedClassNames;
  }

  private static void restoreTestDatabaseBeanDefinitions(
      BeanFactory beanFactory,
      BeanDefinitionRegistry registry,
      Set<String> removedBeanNames)
  {
    ensureTestDatabaseBeanDefinition(registry, "databaseContainer", DatabaseContainer.class,
        databaseContainer -> databaseContainer);
    ensureTestDatabaseBeanDefinition(registry, "databaseProvisioner", DatabaseProvisioner.class,
        DatabaseContainer::getDatabaseProvisioner);
    ensureTestDatabaseBeanDefinition(registry, "dataSourceProvider", DataSourceProvider.class,
        DatabaseContainer::getDataSourceProvider);
    ensureTestDatabaseBeanDefinition(registry, "operationalDataStore", OperationalDataStore.class,
        DatabaseContainer::getOperationalDataStore);
    ensureTestDatabaseBeanDefinition(registry, "aggregationDataStore", AggregationDataStore.class,
        DatabaseContainer::getAggregationDataStore);
    ensureTestDatabaseBeanDefinition(registry, "dataMartDataStore", DataMartDataStore.class,
        DatabaseContainer::getDataMartDataStore);
    ensureTestDatabaseBeanDefinition(registry, "thirdPartyScansDataStore", ThirdPartyScansDataStore.class,
        DatabaseContainer::getThirdPartyScansDataStore);
    ensureClusterLockManagerBeanDefinition(beanFactory, registry);
  }

  private static void restoreNamedBeanDefinitions(BeanDefinitionRegistry registry) {
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Named.class));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*(Test|IT)([.$].*)?$")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(TEST_SUPPORT_CLASS));
    scanner.addExcludeFilter(new AnnotationTypeFilter(IqOnlyEndpoint.class));
    scanner.addExcludeFilter(new AnnotationTypeFilter(TestConfiguration.class));
    scanner.addExcludeFilter(new AnnotationTypeFilter(TestComponent.class));
    scanner.addExcludeFilter(new ExcludeTestClassPathTypeFilter());
    scanner.scan(NAMED_BEAN_BASE_PACKAGE);
  }

  private static void restoreNestedNamedTestBeanDefinitions(BeanDefinitionRegistry registry) {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Named.class));
    scanner.addExcludeFilter(new AnnotationTypeFilter(IqOnlyEndpoint.class));
    scanner.addExcludeFilter(new AnnotationTypeFilter(TestConfiguration.class));
    scanner.addExcludeFilter(new AnnotationTypeFilter(TestComponent.class));

    for (BeanDefinition beanDefinition : scanner.findCandidateComponents(NAMED_BEAN_BASE_PACKAGE)) {
      Class<?> candidate = loadClass(beanDefinition.getBeanClassName());
      if (!shouldRestoreNestedNamedTestBean(candidate)) {
        continue;
      }

      String beanName = candidate.getName();
      if (registry.containsBeanDefinition(beanName)) {
        continue;
      }

      RootBeanDefinition nestedBeanDefinition = new RootBeanDefinition(candidate);
      nestedBeanDefinition.setLazyInit(true);
      registry.registerBeanDefinition(beanName, nestedBeanDefinition);
    }
  }

  private static void ensureClusterLockManagerBeanDefinition(
      BeanFactory beanFactory,
      BeanDefinitionRegistry registry)
  {
    if (registry.containsBeanDefinition("clusterLockManager")) {
      return;
    }

    RootBeanDefinition beanDefinition = new RootBeanDefinition(ClusterLockManager.class);
    beanDefinition.setInstanceSupplier(() -> createClusterLockManager(beanFactory));
    beanDefinition.setPrimary(true);
    registry.registerBeanDefinition("clusterLockManager", beanDefinition);
  }

  private static ClusterLockManager createClusterLockManager(BeanFactory beanFactory) {
    DatabaseContainer container = TestDatabaseContainerHolder.get();
    if (container == null) {
      throw new IllegalStateException(
          "No DatabaseContainer in TestDatabaseContainerHolder when resolving ClusterLockManager");
    }
    OperationalDataStore operationalDataStore = container.getOperationalDataStore();
    if (DatabaseUtil.isDatabaseEmbedded(operationalDataStore.getDatabaseConfig())) {
      return new H2ClusterLockManager();
    }
    return new PostgresClusterLockManager(operationalDataStore, beanFactory.getBean(PostgresAdvisoryLockDAO.class));
  }

  private static <T> void ensureTestDatabaseBeanDefinition(
      BeanDefinitionRegistry registry,
      String beanName,
      Class<T> beanType,
      Function<DatabaseContainer, T> supplier)
  {
    if (registry.containsBeanDefinition(beanName)) {
      return;
    }

    RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
    beanDefinition.setInstanceSupplier(() -> supplier.apply(TestDatabaseContainerHolder.get()));
    beanDefinition.setPrimary(true);
    registry.registerBeanDefinition(beanName, beanDefinition);
  }

  private static boolean shouldRemoveAmbientTestBeanDefinition(
      String beanName,
      BeanDefinition beanDefinition,
      Set<String> allowedConfigurationClassNames)
  {
    String originatingClassName = getOriginatingClassName(beanDefinition.getResourceDescription());
    String candidateClassName = getCandidateClassName(beanDefinition, originatingClassName);
    if (candidateClassName == null || allowedConfigurationClassNames.contains(candidateClassName)) {
      return false;
    }

    return AMBIENT_TEST_SUPPORT_BEAN_NAMES.contains(beanName)
        || isAmbientSupportConfigurationClass(candidateClassName)
        || isNestedTestConfigurationClass(candidateClassName)
        || isTestConfigurationClass(candidateClassName)
        || isAmbientTestSupportClass(candidateClassName);
  }

  private static String getCandidateClassName(BeanDefinition beanDefinition, String originatingClassName) {
    String beanClassName = beanDefinition.getBeanClassName();
    return beanClassName != null ? beanClassName : originatingClassName;
  }

  private static String getOriginatingClassName(String resourceDescription) {
    if (resourceDescription == null) {
      return null;
    }

    Matcher matcher = CLASS_RESOURCE_DESCRIPTION.matcher(resourceDescription);
    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1).replace('/', '.');
  }

  private static boolean isTestConfigurationClass(String className) {
    return matchesLoadableClass(className, candidate -> candidate.isAnnotationPresent(TestConfiguration.class));
  }

  private static boolean isAmbientSupportConfigurationClass(String className) {
    return isEquivalentClassName(className, DataStoreTestModule.class) ||
        isEquivalentClassName(className, TestDatabaseConfiguration.class);
  }

  private static boolean isNestedTestConfigurationClass(String className) {
    return matchesLoadableClass(className,
        candidate -> candidate.isAnnotationPresent(Configuration.class)
            && NESTED_TEST_CONFIGURATION_CLASS.matcher(className).matches());
  }

  // Convention: non-@Configuration classes in com.sonatype whose simple name starts with "Test"
  // are treated as ambient test support (e.g. TestHelper, TestFactory) and excluded from bean
  // registration. Production classes are unaffected because this filter only runs during test
  // context construction on test-classpath candidates.
  private static boolean isAmbientTestSupportClass(String className) {
    return matchesLoadableClass(className,
        candidate -> !candidate.isAnnotationPresent(Configuration.class)
            && candidate.getSimpleName().startsWith("Test"));
  }

  private static boolean shouldRestoreNestedNamedTestBean(Class<?> candidate) {
    if (candidate == null) {
      return false;
    }

    try {
      return candidate.isMemberClass()
          && candidate.isAnnotationPresent(Named.class)
          && !candidate.isAnnotationPresent(Configuration.class)
          && !candidate.isAnnotationPresent(TestConfiguration.class)
          && !candidate.isAnnotationPresent(TestComponent.class)
          && candidate.getEnclosingClass() != null
          && candidate.getEnclosingClass().getSimpleName().matches(".*(Test|IT)");
    }
    catch (LinkageError e) {
      return false;
    }
  }

  private static boolean matchesLoadableClass(String className, Predicate<Class<?>> predicate) {
    Class<?> candidate = loadClass(className);
    if (candidate == null) {
      return false;
    }

    try {
      return predicate.test(candidate);
    }
    catch (LinkageError e) {
      return false;
    }
  }

  private static boolean isEquivalentClassName(String className, Class<?> type) {
    return type.getName().equals(className) || type.getName().replace('$', '.').equals(className);
  }

  private static Class<?> loadClass(String className) {
    String candidateName = className;
    while (true) {
      try {
        return Class.forName(candidateName, false, ExplicitTestConfigurationSupport.class.getClassLoader());
      }
      catch (ClassNotFoundException e) {
        int lastDot = candidateName.lastIndexOf('.');
        if (lastDot < 0) {
          return null;
        }
        candidateName = candidateName.substring(0, lastDot) + '$' + candidateName.substring(lastDot + 1);
      }
      catch (LinkageError e) {
        return null;
      }
    }
  }
}
