/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class CopyStorageTask
    extends AdminTask
    implements InsightJob, MtiqBatchJob, ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(CopyStorageTask.class);

  public static final String PATH = "copyStorage";

  private static final String FROM_PARAMETER = "from";

  private static final String TO_PARAMETER = "to";

  private static final String JOB_NAME = "CopyStorageTask";

  private final TaskScheduler taskScheduler;

  private final CopyStorageService copyStorageService;

  private final ApiConfigurationService apiConfigurationService;

  private final TenantThreadPoolExecutor tenantThreadPoolExecutor;

  @Inject
  public CopyStorageTask(
      final TaskScheduler taskScheduler,
      final CopyStorageService copyStorageService,
      final ApiConfigurationService apiConfigurationService,
      final ShutdownHandler shutdownHandler)
  {
    super(PATH);
    this.taskScheduler = taskScheduler;
    this.copyStorageService = copyStorageService;
    this.apiConfigurationService = apiConfigurationService;
    CopyStorageConfig copyStorageConfig = (CopyStorageConfig) apiConfigurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.COPY_STORAGE_CONFIG);
    int tenantThreads = copyStorageConfig.maxTenantThreads();
    tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
        tenantThreads,
        tenantThreads,
        5L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("CopyStorageTask-%d").build(),
        new AbortPolicy(),
        "copy_storage_task",
        getClass().getSimpleName())
    {
      @Override
      public void shutdown() {
        super.shutdown();
        getQueue().clear();
      }
    };
    tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(tenantThreadPoolExecutor);
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    String from = getFirstNotNull(parameters, FROM_PARAMETER).toUpperCase(Locale.ROOT);
    String to = getFirstNotNull(parameters, TO_PARAMETER).toUpperCase(Locale.ROOT);

    DataStoreType fromDataStoreType = DataStoreType.valueOf(from);
    DataStoreType toDataStoreType = DataStoreType.valueOf(to);
    copyStorageService.checkSupported(fromDataStoreType);
    copyStorageService.checkSupported(toDataStoreType);
    copyStorageService.checkPrimaryStorageIsTarget(toDataStoreType);
    copyStorageService.checkFromAndToAreDifferent(fromDataStoreType, toDataStoreType);

    Map<String, String> jobParameters = new HashMap<>();
    jobParameters.put(FROM_PARAMETER, from);
    jobParameters.put(TO_PARAMETER, to);

    taskScheduler.scheduleOneTimeTask(this, jobParameters);
  }

  private String getFirstNotNull(final Map<String, List<String>> parameters, final String key) {
    String value = getFirst(parameters, key);
    if (value == null) {
      throw new BadRequestException("Missing required query parameter '" + key + "'.");
    }
    return value;
  }

  private String getFirst(final Map<String, List<String>> parameters, final String key) {
    return parameters.getOrDefault(key, List.of())
        .stream()
        .findFirst()
        .orElse(null);
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    tenantThreadPoolExecutor.submit(() -> doExecute(context));
  }

  private void doExecute(final JobExecutionContext context) {
    log.info("Running '{}' job.", JOB_NAME);
    try {
      String from = context.getMergedJobDataMap().getString(FROM_PARAMETER);
      String to = context.getMergedJobDataMap().getString(TO_PARAMETER);

      DataStoreType fromDataStoreType = DataStoreType.valueOf(from);
      DataStoreType toDataStoreType = DataStoreType.valueOf(to);

      copyStorageService.execute(fromDataStoreType, toDataStoreType);
      log.info("Completed '{}' job.", JOB_NAME);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.COPY_STORAGE_CONFIG)) {
      CopyStorageConfig copyStorageConfig = (CopyStorageConfig) apiConfigurationService.getConfigurationNoAuthz(
          SystemConfigurationProperty.COPY_STORAGE_CONFIG);
      int maxTenantThreads = copyStorageConfig.maxTenantThreads();
      int currentCore = tenantThreadPoolExecutor.getCorePoolSize();
      int currentMax = tenantThreadPoolExecutor.getMaximumPoolSize();
      if (currentCore != maxTenantThreads || currentMax != maxTenantThreads) {
        if (maxTenantThreads > currentMax) {
          tenantThreadPoolExecutor.setMaximumPoolSize(maxTenantThreads);
          tenantThreadPoolExecutor.setCorePoolSize(maxTenantThreads);
        }
        else {
          tenantThreadPoolExecutor.setCorePoolSize(maxTenantThreads);
          tenantThreadPoolExecutor.setMaximumPoolSize(maxTenantThreads);
        }
        log.debug("Updated 'tenantLimit' to {}.", maxTenantThreads);
      }
    }
  }

}
