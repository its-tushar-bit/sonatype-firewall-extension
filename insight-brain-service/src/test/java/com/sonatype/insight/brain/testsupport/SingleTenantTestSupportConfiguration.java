/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testsupport;

import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicenseDetailsCache;
import com.sonatype.insight.brain.scheduler.QuartzConcurrencyListener;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService;
import com.sonatype.insight.brain.scheduler.QuartzTriggerListener;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.test.productlicense.ProductLicenseConfig;
import com.sonatype.insight.test.productlicense.ProductLicenseSigner;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Explicit single-tenant test support beans that were previously ambiently available in the legacy harness.
 */
@TestConfiguration
public class SingleTenantTestSupportConfiguration
{
  @Bean
  public ProductLicenseConfig productLicenseConfig() {
    ProductLicenseConfig config = new ProductLicenseConfig();
    config.setKeyStorePath(requireTestResource("/productlicense/licensing-keystore-hds.p12"));
    config.setKeyStoreAliasGroup("licensing-key-test");
    return config;
  }

  @Bean
  public ProductLicenseSigner productLicenseSigner(final AutowireCapableBeanFactory beanFactory) {
    return beanFactory.createBean(ProductLicenseSigner.class);
  }

  @Bean
  @Primary
  public TestLicenseFingerprinter testLicenseFingerprinter() {
    return new TestLicenseFingerprinter();
  }

  @Bean
  @Primary
  public TestProductLicenseManager testProductLicenseManager() {
    return new TestProductLicenseManager();
  }

  @Bean
  @Primary
  public TestProductLicense testProductLicense(final TestProductLicenseManager testProductLicenseManager) {
    return new TestProductLicense(testProductLicenseManager);
  }

  @Bean
  @Primary
  public TestProductLicenseDetailsCache testProductLicenseDetailsCache(final ProductLicenseDAO productLicenseDAO) {
    return new TestProductLicenseDetailsCache(productLicenseDAO);
  }

  @Bean
  public PolicyEvaluationHelper policyEvaluationHelper() {
    return new PolicyEvaluationHelper();
  }

  @Bean
  @Primary
  public TestQuartzJobStoreTx testQuartzJobStoreTx(
      final TestProductLicense testProductLicense,
      final InsightConfig insightConfig,
      final OperationalDataStore operationalDataStore) throws InvalidConfigurationException
  {
    return new TestQuartzJobStoreTx(testProductLicense, insightConfig, operationalDataStore);
  }

  @Bean
  @Primary
  public TestTaskScheduler testTaskScheduler(
      final TestQuartzJobStoreTx testQuartzJobStoreTx,
      final JobFactory jobFactory,
      final QuartzTriggerListener quartzTriggerListener,
      final QuartzConcurrencyListener quartzConcurrencyListener,
      final OperationalDataStore operationalDataStore,
      final ShutdownHandler shutdownHandler,
      final QuartzJobSchedulingService quartzJobSchedulingService)
  {
    return new TestTaskScheduler(
        testQuartzJobStoreTx,
        jobFactory,
        quartzTriggerListener,
        quartzConcurrencyListener,
        operationalDataStore,
        shutdownHandler,
        quartzJobSchedulingService);
  }

  private static String requireTestResource(final String resourcePath) {
    var resource = SingleTenantTestSupportConfiguration.class.getResource(resourcePath);
    if (resource == null) {
      throw new IllegalStateException("Missing test resource: " + resourcePath);
    }
    return resource.getPath();
  }
}
