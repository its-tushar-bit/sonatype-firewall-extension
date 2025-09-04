/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class CopyStorageTask
    extends Task
    implements InsightJob, AllTenantsJob
{
  private static final Logger log = LoggerFactory.getLogger(CopyStorageTask.class);

  private static final String PATH = "copyStorage";

  private static final String TENANT_SLUG_REGEX_PARAMETER = "tenant";

  private static final String FROM_PARAMETER = "from";

  private static final String TO_PARAMETER = "to";

  private static final String JOB_NAME = "CopyStorageTask";

  private final TaskScheduler taskScheduler;

  private final CopyStorageService copyStorageService;

  private final TenantUtil tenantUtil;

  @Inject
  public CopyStorageTask(
      final TaskScheduler taskScheduler,
      final CopyStorageService copyStorageService,
      final TenantUtil tenantUtil)
  {
    super(PATH);
    this.taskScheduler = taskScheduler;
    this.copyStorageService = copyStorageService;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    boolean isMultiTenant = tenantUtil.isMultiTenant();
    String from = getFirstNotNull(map, FROM_PARAMETER).toUpperCase(Locale.ROOT);
    String to = getFirstNotNull(map, TO_PARAMETER).toUpperCase(Locale.ROOT);

    // Although CopyStorageService does these checks internally,
    // we repeat them here to avoid scheduling the job if we don't need to
    String tenantSlugRegex = null;
    if (isMultiTenant) {
      tenantSlugRegex = getTenantSlugRegex(map);
      Pattern.compile(tenantSlugRegex);
    }
    DataStoreType fromDataStoreType = DataStoreType.valueOf(from);
    DataStoreType toDataStoreType = DataStoreType.valueOf(to);
    copyStorageService.checkSupported(fromDataStoreType);
    copyStorageService.checkSupported(toDataStoreType);
    copyStorageService.checkPrimaryStorageIsTarget(toDataStoreType);
    copyStorageService.checkFromAndToAreDifferent(fromDataStoreType, toDataStoreType);

    Map<String, String> parameters = new HashMap<>();
    if (isMultiTenant) {
      parameters.put(TENANT_SLUG_REGEX_PARAMETER, tenantSlugRegex);
    }
    parameters.put(FROM_PARAMETER, from);
    parameters.put(TO_PARAMETER, to);

    taskScheduler.scheduleOneTimeTask(this, parameters);
  }

  private String getTenantSlugRegex(final Map<String, List<String>> map) {
    String tenantSlugRegex = getFirst(map, TENANT_SLUG_REGEX_PARAMETER);
    if (tenantSlugRegex != null) {
      return tenantSlugRegex;
    }
    Tenant tenant = TenantThreadLocal.getTenant();
    if (Tenant.GLOBAL_TENANT.equals(tenant)) {
      return ".*";
    }
    return tenant.tenantSlug;
  }

  private String getFirst(final Map<String, List<String>> map, final String key) {
    return map.getOrDefault(key, List.of()).stream()
        .findFirst()
        .orElse(null);
  }

  private String getFirstNotNull(final Map<String, List<String>> map, final String key) {
    String value = getFirst(map, key);
    if (value == null) {
      throw new BadRequestException("Missing required query parameter '" + key + "'.");
    }
    return value;
  }

  @Override
  public void executeForTenant(final JobExecutionContext context, final Tenant tenant) {
    if (tenantUtil.isMultiTenant()) {
      executeForMultiTenantMode(context, tenant);
    }
    else {
      executeForSingleTenantMode(context);
    }
  }

  private void executeForMultiTenantMode(final JobExecutionContext context, final Tenant tenant) {
    String tenantSlugRegex = context.getMergedJobDataMap().getString(TENANT_SLUG_REGEX_PARAMETER);
    Pattern tenantSlugPattern = Pattern.compile(tenantSlugRegex);

    if (tenantSlugPattern.matcher(tenant.tenantSlug).matches()) {
      log.info("Running '{}' job for tenant '{}' as their slug matches '{}'.", JOB_NAME, tenant.tenantSlug,
          tenantSlugRegex);
      doExecute(context);
    }
    else {
      log.info("Skipping '{}' job for tenant '{}' as their slug does not match '{}'.", JOB_NAME, tenant.tenantSlug,
          tenantSlugRegex);
    }
    log.info("Completed '{}' job for tenant '{}'.", JOB_NAME, tenant.tenantSlug);
  }

  private void executeForSingleTenantMode(final JobExecutionContext context) {
    log.info("Running '{}' job.", JOB_NAME);
    doExecute(context);
    log.info("Completed '{}' job.", JOB_NAME);
  }

  private void doExecute(final JobExecutionContext context) {
    String from = context.getMergedJobDataMap().getString(FROM_PARAMETER);
    String to = context.getMergedJobDataMap().getString(TO_PARAMETER);

    DataStoreType fromDataStoreType = DataStoreType.valueOf(from);
    DataStoreType toDataStoreType = DataStoreType.valueOf(to);

    copyStorageService.execute(fromDataStoreType, toDataStoreType);
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }
}
