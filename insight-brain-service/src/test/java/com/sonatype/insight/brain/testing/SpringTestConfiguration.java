/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
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
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchIndexRule;
import com.sonatype.insight.brain.security.SecurityAopConfiguration;
import com.sonatype.insight.brain.security.oauth2.JWTGenerator;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.spring.config.CoreConfiguration;
import com.sonatype.insight.brain.spring.config.LicensingConfiguration;
import com.sonatype.insight.brain.spring.config.PersistenceConfiguration;
import com.sonatype.insight.brain.spring.config.ScheduledConfiguration;
import com.sonatype.insight.brain.spring.config.SearchConfiguration;
import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import com.sonatype.insight.brain.testsupport.SingleTenantTestSupportConfiguration;
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.test.SpringTestExecutionContext;
import jakarta.inject.Named;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Spring test configuration for database-backed injected tests.
 */
@TestConfiguration
@ComponentScan(
    basePackages = {
      "com.sonatype.insight.brain",
      "com.sonatype.insight.jaxrs"
    },
    useDefaultFilters = false,
    includeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = Named.class)
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.CUSTOM,
          classes = ExcludeTestClassPathTypeFilter.class),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = ".*(Test|IT)([.$].*)?$"),
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = TestConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = TestComponent.class)
    })
@Import({
  CoreConfiguration.class,
  SecurityAopConfiguration.class,
  ScheduledConfiguration.class,
  SearchConfiguration.class,
  SecurityConfiguration.class,
  LicensingConfiguration.class,
  PersistenceConfiguration.class,
  SingleTenantTestSupportConfiguration.class
})
public class SpringTestConfiguration
{
  private static DatabaseContainerRule databaseContainerRule() {
    DatabaseContainerRule rule = DatabaseContainerRule.getInstance(SpringBrainInjectedTest.class);
    rule.ensureInitializedForSpringContext();
    return rule;
  }

  private static SearchIndexRule searchIndexRule() {
    SearchIndexRule rule = SearchIndexRule.getInstance(SpringBrainInjectedTest.class);
    rule.ensureInitializedForSpringContext();
    return rule;
  }

  private static final Set<String> EAGER_INIT_BEANS = Set.of(
      "authorizeAspect", "authzFilterAspect", "hasFeatureAspect", "anonymousWithFeatureAspect");

  @Bean
  public static BeanFactoryPostProcessor lazyInitBeanFactoryPostProcessor() {
    return beanFactory -> {
      for (String beanName : beanFactory.getBeanDefinitionNames()) {
        if (!EAGER_INIT_BEANS.contains(beanName)) {
          beanFactory.getBeanDefinition(beanName).setLazyInit(true);
        }
      }
    };
  }

  @Bean
  @Primary
  public InsightConfig insightConfig() {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(uniqueWorkDirectory().getAbsolutePath());

    SearchConfig searchConfig = resolveSearchConfigForCurrentTest();
    if (searchConfig != null) {
      insightConfig.setSearchConfig(searchConfig);
    }

    return insightConfig;
  }

  @Bean
  @Primary
  public DatabaseContainer databaseContainer() {
    return databaseContainerRule().getDatabaseContainer();
  }

  @Bean
  @Primary
  public DatabaseProvisioner databaseProvisioner() {
    return databaseContainer().getDatabaseProvisioner();
  }

  @Bean
  @Primary
  public DataSourceProvider dataSourceProvider() {
    return databaseContainer().getDataSourceProvider();
  }

  @Bean
  @Primary
  public OperationalDataStore operationalDataStore() {
    return databaseContainerRule().getOperationalDataStore();
  }

  @Bean
  @Primary
  public AggregationDataStore aggregationDataStore() {
    return databaseContainerRule().getAggregationDataStore();
  }

  @Bean
  @Primary
  public DataMartDataStore dataMartDataStore() {
    return databaseContainerRule().getDataMartDataStore();
  }

  @Bean
  @Primary
  public ThirdPartyScansDataStore thirdPartyScansDataStore() {
    return databaseContainerRule().getThirdPartyScansDataStore();
  }

  @Bean
  @Primary
  public ClusterLockManager clusterLockManager(
      final OperationalDataStore operationalDataStore,
      final PostgresAdvisoryLockDAO postgresAdvisoryLockDAO)
  {
    if (DatabaseUtil.isDatabaseEmbedded(operationalDataStore.getDatabaseConfig())) {
      return new H2ClusterLockManager();
    }
    return new PostgresClusterLockManager(operationalDataStore, postgresAdvisoryLockDAO);
  }

  @Bean(name = "oAuth2ConfigurationDAO")
  @Primary
  public OAuth2ConfigurationDAO oAuth2ConfigurationDAO(final OperationalDataStore operationalDataStore) {
    return new OAuth2ConfigurationDAO(operationalDataStore);
  }

  @Bean
  public JWTGenerator jwtGenerator() {
    return new JWTGenerator();
  }

  @Bean
  public ScanReader scanReader() {
    return new ScanReader();
  }

  /**
   * Workaround binding for licensing.access.file.
   * This is referenced by sonatype-licensing but not actually used.
   */
  @Bean
  @Qualifier("licensing.access.file")
  public File licensingAccessFile() {
    return new File("workaround");
  }

  static SearchConfig resolveSearchConfigForCurrentTest() {
    SearchConfig searchConfig = searchIndexRule().getSearchConfig();
    if (searchConfig != null) {
      return searchConfig;
    }
    return resolveSearchConfigFromCustomizeConfig();
  }

  private static SearchConfig resolveSearchConfigFromCustomizeConfig() {
    Class<?> springTestClass = SpringTestExecutionContext.getCurrentTestClass();
    if (springTestClass == null) {
      return null;
    }

    Method customizeConfig = findCustomizeConfigMethod(springTestClass);
    if (customizeConfig == null || customizeConfig.getDeclaringClass().equals(AbstractComponentTest.class)) {
      return null;
    }

    try {
      Object testInstance = SpringTestExecutionContext.getCurrentTestInstance();
      if (testInstance == null || !springTestClass.isInstance(testInstance)) {
        return null;
      }
      InsightConfig probeConfig = new InsightConfig();
      customizeConfig.setAccessible(true);
      customizeConfig.invoke(testInstance, probeConfig);
      return probeConfig.getSearchConfig();
    }
    catch (InvocationTargetException e) {
      throw new IllegalStateException(
          "Failed to resolve search config from customizeConfig for " + springTestClass.getName(),
          e.getTargetException());
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Failed to resolve search config from customizeConfig for " + springTestClass.getName(),
          e);
    }
  }

  private static Method findCustomizeConfigMethod(Class<?> type) {
    Class<?> current = type;
    while (current != null) {
      try {
        return current.getDeclaredMethod("customizeConfig", InsightConfig.class);
      }
      catch (NoSuchMethodException ignored) {
        current = current.getSuperclass();
      }
    }
    return null;
  }

  private static File uniqueWorkDirectory() {
    String forkId = System.getProperty("test.forkId", "");
    return new File("target/test-component-work" + forkId + "-" + UUID.randomUUID());
  }
}
