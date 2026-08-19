/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.migration.ScanFileCleaner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.util.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.PURGE_SCAN_FILES;

@Named
@Singleton
@DisallowConcurrentExecution
public class ApiConfigurationService
    implements InsightJob, GlobalTenantJob
{
  private static final Logger log = LoggerFactory.getLogger(ApiConfigurationService.class);

  @VisibleForTesting
  static final String INVALID_PROPERTY_NAME_ERROR_MSG = "Invalid property name %s.";

  @VisibleForTesting
  static final String INVALID_PROPERTY_VALUE_TYPE_ERROR_MSG =
      "Invalid property value type for %s, expected %s but got %s.";

  public static final String INVALID_SUCCESS_METRIC_STAGE_ID_ERROR_MSG =
      "Invalid value '%s' provided for %s. Allowed values are: '%s'";

  @VisibleForTesting
  static final String LEGACY_SUCCESS_METRICS_STAGE_ID = DevelopStageType.ID;

  @VisibleForTesting
  static final String NORMALIZED_SUCCESS_METRICS_STAGE_ID = SourceStageType.ID;

  // Visible for testing
  public static final String NO_PROPERTIES_ERROR_MSG = "No properties were specified.";

  // Visible for testing
  static final String TASK_NAME = "Configuration";

  // Visible for testing
  static final String TASK_PARAM_PROPERTIES_DELIMITER = ",";

  // Visible for testing
  static final String TASK_PARAM_PROPERTIES = "properties";

  private static final String CONFIG_APPLY_ERROR = "Error when applying config";

  // Visible for testing
  static final String SUPPORTED_VERSION_COUNT = "supported_version_count";

  // Visible for testing
  static final String ACTION = "action";

  @VisibleForTesting
  static final Set<String> MTIQ_CUSTOMER_WRITABLE_PROPERTIES = Set.of(
      SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE);

  @VisibleForTesting
  static final String MTIQ_PROPERTY_NOT_ALLOWED_ERROR_MSG =
      "Property '%s' is not configurable in SaaS.";

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final ObjectProvider<ConfigurationListener> configurationListeners;

  private final InsightConfig insightConfig;

  private final TaskScheduler taskScheduler;

  private final ProductLicense productLicense;

  private final ScanFileCleaner scanFileCleaner;

  private final PermissionService permissionService;

  private final StageTypeService stageTypeService;

  private final TelemetrySender telemetrySender;

  private final TenantUtil tenantUtil;

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public ApiConfigurationService(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      ObjectProvider<ConfigurationListener> configurationListeners,
      InsightConfig insightConfig,
      TaskScheduler taskScheduler,
      ProductLicense productLicense,
      @Lazy @Nullable ScanFileCleaner scanFileCleaner,
      PermissionService permissionService,
      StageTypeService stageTypeService,
      @Lazy @Nullable TelemetrySender telemetrySender,
      TenantUtil tenantUtil,
      RepositoryManagerDAO repositoryManagerDAO)
  {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.configurationListeners = configurationListeners;
    this.insightConfig = insightConfig;
    this.taskScheduler = taskScheduler;
    this.productLicense = productLicense;
    this.scanFileCleaner = scanFileCleaner;
    this.permissionService = permissionService;
    this.stageTypeService = stageTypeService;
    this.telemetrySender = telemetrySender;
    this.tenantUtil = tenantUtil;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  public Map<String, Object> getConfiguration(Set<String> propertyNames) {
    checkAuthenticated();
    checkPropertiesPermissions(propertyNames);
    return getConfigurationNoAuthz(propertyNames);
  }

  private static void checkAuthenticated() {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal == null) {
      throw new UnauthenticatedException("Anonymous access forbidden");
    }
  }

  private void checkPropertiesPermissions(final Set<String> propertyNames) {
    if (propertyNames == null) {
      return;
    }

    for (String propertyName : propertyNames) {
      // Skip permission check for public properties (authentication is still required via checkAuthenticated())
      if (ConfigurationProperty.PUBLIC_PROPERTIES.contains(propertyName)) {
        continue;
      }

      ConfigurationProperty property = ConfigurationProperty.getConfigurationPropertiesByName().get(propertyName);
      if (property != null && !checkPermissionForProperty(propertyName)) {
        throw new UnauthorizedException("Insufficient permissions");
      }
    }
  }

  private boolean checkPermissionForProperty(final String propertyName) {
    Set<Triple<OwnerType, String, Set<Permission>>> permissionsToCheck =
        ConfigurationProperty.additionalPermissionsPerProperty.getOrDefault(propertyName, new HashSet<>());

    // By default all properties can be accessed with the admin (global) CONFIGURE_SYSTEM permission
    permissionsToCheck.add(Triple.of(OwnerType.GLOBAL, null, Collections.singleton(Permission.CONFIGURE_SYSTEM)));

    // We are authorized if PermissionService.validatePermission returns a non-empty value for any triplet
    // since we pass it a singleton containing our desired permission on each iteration
    // (i.e. permissionA OR permissionB or ...)
    if (permissionsToCheck.stream()
        .anyMatch(
            permissionGroup -> !permissionService
                .validatePermission(SecurityUtils.getSubject(), permissionGroup.getLeft(),
                    permissionGroup.getMiddle(),
                    permissionGroup.getRight())
                .isEmpty()))
    {
      return true;
    }

    // For quarantinedItemCustomMessage, also allow users with EVALUATE_COMPONENT at any repository manager.
    // Nexus Repository integrates with Firewall using a user scoped to a repository manager, and needs
    // to read this property to display custom quarantine messages (NEXUS-51730).
    if (SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE.equals(propertyName)) {
      Object principal = SecurityUtils.getSubject().getPrincipal();
      if (!(principal instanceof UserPrincipal)) {
        return false;
      }
      Set<String> contextIds = permissionService.getContextIdsForUserWithPermission((UserPrincipal) principal,
          Permission.EVALUATE_COMPONENT);
      return !repositoryManagerDAO.getByIds(contextIds).isEmpty();
    }

    return false;
  }

  public Map<String, Object> getConfigurationNoAuthz(Set<String> propertyNames) {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      return getConfigurationNoAuthz(tx, propertyNames);
    }
  }

  public Object getConfigurationNoAuthz(String propertyName) {
    return getConfigurationNoAuthz(Collections.singleton(propertyName)).get(propertyName);
  }

  public Map<String, Object> getConfigurationNoAuthz(TransactionContext tx, Set<String> propertyNames) {
    if (CollectionUtils.isEmpty(propertyNames)) {
      throw new BadRequestException(NO_PROPERTIES_ERROR_MSG);
    }
    Map<String, Object> result = new HashMap<>();
    for (String propertyName : propertyNames) {
      validatePropertyName(propertyName);
      result.put(propertyName, ConfigurationProperty.PROPERTY_BY_NAME.get(propertyName)
          .getStringToValue()
          .apply(new Object[]{insightConfig}, systemConfigurationPropertyDAO.get(tx, propertyName)));
    }
    return result;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(Map<String, Object> properties) {
    if (tenantUtil.isMultiTenant() && properties != null) {
      validateMtiqWritableProperties(properties.keySet());
    }

    setConfigurationNoAuthz(properties);
  }

  public void setConfigurationNoAuthz(String propertyName, Object propertyValue) {
    setConfigurationNoAuthz(Collections.singletonMap(propertyName, propertyValue));
  }

  /**
   * This method (and its overloads) are intended to update configuration properties in the database, apply
   * configuration changes to clients, and propagate configuration changes to other nodes all without needing authz.
   */
  public void setConfigurationNoAuthz(Map<String, Object> properties) {
    setConfigurationInDatabaseNoAuthz(properties);
    updateAllClusterNodesFromConfiguration(properties.keySet());
  }

  public void setConfigurationInDatabaseNoAuthz(String propertyName, Object propertyValue) {
    setConfigurationInDatabaseNoAuthz(Collections.singletonMap(propertyName, propertyValue));
  }

  public void setConfigurationInDatabaseNoAuthz(Map<String, Object> properties) {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      setConfigurationInDatabaseNoAuthz(tx, properties);
      tx.commit();
    }
  }

  /**
   * This method (and its overloads) are only intended to update configuration properties in the database without
   * needing authz. They do not apply any client configuration changes or propagate changes to other nodes. This is
   * useful for migrators and tests.
   */
  public void setConfigurationInDatabaseNoAuthz(TransactionContext tx, Map<String, Object> properties) {
    if (CollectionUtils.isEmpty(properties)) {
      throw new BadRequestException(NO_PROPERTIES_ERROR_MSG);
    }
    if (properties.containsKey(SystemConfigurationProperty.ACCESS_ALLOWLIST) && !productLicense.hasFeature(
        LicensedFeature.IP_ALLOWLIST))
    {
      throw new InvalidLicenseException();
    }
    for (Entry<String, Object> property : properties.entrySet()) {
      String propertyName = property.getKey();
      Object normalizedPropertyValue = normalizePropertyValue(propertyName, property.getValue());

      validatePropertyName(propertyName);
      validatePropertyValue(propertyName, normalizedPropertyValue);

      // Check for existing value before setting (for telemetry action determination)
      String existingValue = systemConfigurationPropertyDAO.get(tx, propertyName);
      String action = (existingValue == null) ? "enable" : "update";

      AuditData.get().setData(propertyName, normalizedPropertyValue);
      systemConfigurationPropertyDAO.set(tx, propertyName,
          ConfigurationProperty.PROPERTY_BY_NAME.get(propertyName)
              .getValueToString()
              .apply(new Object[]{tx, existingValue}, normalizedPropertyValue));
      sendTelemetryForIntegrationsSupportedVersionCount(propertyName, normalizedPropertyValue, action);
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration(Set<String> propertyNames) {
    if (tenantUtil.isMultiTenant() && propertyNames != null) {
      validateMtiqWritableProperties(propertyNames);
    }

    deleteConfigurationNoAuthz(propertyNames);
  }

  public void deleteConfigurationNoAuthz(Set<String> propertyNames) {
    deleteConfigurationInDatabaseNoAuthz(propertyNames);
    updateAllClusterNodesFromConfiguration(propertyNames);
  }

  public void deleteConfigurationInDatabaseNoAuthz(String... propertyNames) {
    deleteConfigurationInDatabaseNoAuthz(Sets.newHashSet(propertyNames));
  }

  public void deleteConfigurationInDatabaseNoAuthz(Set<String> propertyNames) {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      deleteConfigurationInDatabaseNoAuthz(tx, propertyNames);
      tx.commit();
    }
  }

  public void deleteConfigurationInDatabaseNoAuthz(TransactionContext tx, Set<String> propertyNames) {
    if (CollectionUtils.isEmpty(propertyNames)) {
      throw new BadRequestException(NO_PROPERTIES_ERROR_MSG);
    }
    for (String propertyName : propertyNames) {
      validatePropertyName(propertyName);
      ConfigurationProperty property = ConfigurationProperty.PROPERTY_BY_NAME.get(propertyName);
      Object currentValue = property.getStringToValue()
          .apply(new Object[]{insightConfig}, systemConfigurationPropertyDAO.get(tx, propertyName));
      AuditData.get().setData(propertyName, currentValue);
      systemConfigurationPropertyDAO.set(tx, propertyName, null);
      scheduleTaskForConfigurationDeletion(propertyName);
      // Only send telemetry if there was actually a value to delete
      if (currentValue != null) {
        sendTelemetryForIntegrationsSupportedVersionCount(propertyName, currentValue, "disable");
      }
    }
  }

  private void scheduleTaskForConfigurationDeletion(String propertyName) {
    if (propertyName.equals(PURGE_SCAN_FILES) && scanFileCleaner != null) {
      taskScheduler.scheduleOneTimeTask(scanFileCleaner);
    }
  }

  public void applyConfigurationToClients(String... propertyNames) {
    applyConfigurationToClients(Sets.newHashSet(propertyNames));
  }

  public void applyConfigurationToClients(Set<String> propertyNames) {
    configurationListeners.orderedStream()
        .forEach(listener -> listener.configurationChanged(propertyNames));
  }

  public void updateAllClusterNodesFromConfiguration(Set<String> propertyNames) {
    applyConfigurationToClients(propertyNames);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TASK_PARAM_PROPERTIES, StringUtils.join(propertyNames, TASK_PARAM_PROPERTIES_DELIMITER));
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this, parameters);
  }

  private void validatePropertyName(String propertyName) {
    if (!ConfigurationProperty.PROPERTY_BY_NAME.containsKey(propertyName)) {
      throw new BadRequestException(String.format(INVALID_PROPERTY_NAME_ERROR_MSG, propertyName));
    }
  }

  private void validateMtiqWritableProperties(Set<String> propertyNames) {
    for (String name : propertyNames) {
      if (name == null) {
        throw new BadRequestException("Property name cannot be null");
      }
      if (!MTIQ_CUSTOMER_WRITABLE_PROPERTIES.contains(name)) {
        throw new BadRequestException(
            String.format(MTIQ_PROPERTY_NOT_ALLOWED_ERROR_MSG, name));
      }
    }
  }

  private void validatePropertyValue(String propertyName, Object propertyValue) {
    if (propertyValue == null) {
      return;
    }
    Class<?> expectedType = ConfigurationProperty.PROPERTY_BY_NAME.get(propertyName).getType();
    Class<?> actualType = propertyValue.getClass();
    Class<?> primitiveExpectedType = ClassUtils.wrapperToPrimitive(expectedType);
    Class<?> primitiveActualType = ClassUtils.wrapperToPrimitive(actualType);
    if (primitiveExpectedType != null && primitiveActualType != null) {
      expectedType = primitiveExpectedType;
      actualType = primitiveActualType;
    }
    if (!ClassUtils.isAssignable(actualType, expectedType)) {
      throw new BadRequestException(
          String.format(INVALID_PROPERTY_VALUE_TYPE_ERROR_MSG, propertyName, expectedType, actualType));
    }

    if (SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID.equals(propertyName)) {
      final var validStageIds = stageTypeService.getValidSuccessMetricsStageTypeIds();
      final var normalizedStageId = normalizeSuccessMetricsStageId((String) propertyValue);

      if (!validStageIds.contains(normalizedStageId)) {
        throw new BadRequestException(String.format(
            INVALID_SUCCESS_METRIC_STAGE_ID_ERROR_MSG,
            propertyValue,
            SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID,
            validStageIds));
      }
    }
  }

  private static Object normalizePropertyValue(final String propertyName, final Object propertyValue) {
    if (SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID.equals(propertyName) && propertyValue instanceof String) {
      return normalizeSuccessMetricsStageId((String) propertyValue);
    }
    return propertyValue;
  }

  public static String normalizeSuccessMetricsStageId(@Nullable final String stageTypeId) {
    if (LEGACY_SUCCESS_METRICS_STAGE_ID.equals(stageTypeId)) {
      return NORMALIZED_SUCCESS_METRICS_STAGE_ID;
    }
    return stageTypeId;
  }

  @VisibleForTesting
  void sendTelemetryForIntegrationsSupportedVersionCount(
      final String propertyName,
      final Object propertyValue,
      final String action)
  {
    if (!SystemConfigurationProperty.INTEGRATIONS_SUPPORTED_VERSION_COUNT.equals(propertyName)) {
      return;
    }
    try {
      if (telemetrySender == null) {
        return;
      }
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.INTEGRATIONS_SUPPORTED_VERSION_COUNT_USAGE);
      telemetryData.getAttributes().put(SUPPORTED_VERSION_COUNT, propertyValue);
      telemetryData.getAttributes().put(ACTION, action);
      telemetrySender.send(telemetryData);
    }
    catch (Exception e) {
      log.debug("Failed to send telemetry for integrations supported version count configuration", e);
    }
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    execute(() -> {
      if (context.getMergedJobDataMap().containsKey(TASK_PARAM_PROPERTIES)) {
        String[] properties = StringUtils.split(context.getMergedJobDataMap().getString(TASK_PARAM_PROPERTIES),
            TASK_PARAM_PROPERTIES_DELIMITER);
        systemConfigurationPropertyDAO.invalidateCache();
        applyConfigurationToClients(properties);
      }
    }, log, CONFIG_APPLY_ERROR);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
