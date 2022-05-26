/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ApiConfigurationService
    implements Job
{
  private static final Logger log = LoggerFactory.getLogger(ApiConfigurationService.class);

  // Visible for testing
  static final String INVALID_PROPERTY_NAME_ERROR_MSG = "Invalid property name %s.";

  // Visible for testing
  static final String INVALID_PROPERTY_VALUE_TYPE_ERROR_MSG =
      "Invalid property value type for %s, expected %s but got %s.";

  // Visible for testing
  public static final String NO_PROPERTIES_ERROR_MSG = "No properties were specified.";

  // Visible for testing
  static final String TASK_NAME = "Configuration";

  // Visible for testing
  static final String TASK_PARAM_PROPERTIES_DELIMITER = ",";

  // Visible for testing
  static final String TASK_PARAM_PROPERTIES = "properties";

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final List<ConfigurationListener> configurationListeners;

  private final TaskScheduler taskScheduler;

  @Inject
  public ApiConfigurationService(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      List<ConfigurationListener> configurationListeners,
      TaskScheduler taskScheduler)
  {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.configurationListeners = configurationListeners;
    this.taskScheduler = taskScheduler;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public Map<String, Object> getConfiguration(Set<String> propertyNames) {
    return getConfigurationNoAuthz(propertyNames);
  }

  public Map<String, Object> getConfigurationNoAuthz(Set<String> propertyNames) {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      return getConfigurationNoAuthz(tx, propertyNames);
    }
  }

  public Map<String, Object> getConfigurationNoAuthz(TransactionContext tx, Set<String> propertyNames) {
    if (CollectionUtils.isEmpty(propertyNames)) {
      throw new BadRequestException(NO_PROPERTIES_ERROR_MSG);
    }
    Map<String, Object> result = new HashMap<>();
    for (String propertyName : propertyNames) {
      validatePropertyName(propertyName);
      result.put(propertyName, ConfigurationProperty.PROPERTY_BY_NAME.get(propertyName).getStringToValue()
          .apply(tx, systemConfigurationPropertyDAO.get(tx, propertyName)));
    }
    return result;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(Map<String, Object> properties) {
    setConfigurationNoAuthz(properties);
    updateAllClusterNodesFromConfiguration(properties.keySet());
  }

  public void setConfigurationNoAuthz(Map<String, Object> properties) {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      setConfigurationNoAuthz(tx, properties);
      tx.commit();
    }
  }

  public void setConfigurationNoAuthz(TransactionContext tx, Map<String, Object> properties) {
    if (CollectionUtils.isEmpty(properties)) {
      throw new BadRequestException(NO_PROPERTIES_ERROR_MSG);
    }
    for (Entry<String, Object> property : properties.entrySet()) {
      validatePropertyName(property.getKey());
      validatePropertyValue(property.getKey(), property.getValue());
      AuditData.get().setData(property.getKey(), property.getValue());
      systemConfigurationPropertyDAO.set(tx, property.getKey(),
          ConfigurationProperty.PROPERTY_BY_NAME.get(property.getKey()).getValueToString()
              .apply(tx, property.getValue()));
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration(Set<String> propertyNames) {
    deleteConfigurationNoAuthz(propertyNames);
    updateAllClusterNodesFromConfiguration(propertyNames);
  }

  public void deleteConfigurationNoAuthz(Set<String> propertyNames) {
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      deleteConfigurationNoAuthz(tx, propertyNames);
      tx.commit();
    }
  }

  public void deleteConfigurationNoAuthz(TransactionContext tx, Set<String> propertyNames) {
    if (CollectionUtils.isEmpty(propertyNames)) {
      throw new BadRequestException(NO_PROPERTIES_ERROR_MSG);
    }
    for (String propertyName : propertyNames) {
      validatePropertyName(propertyName);
      ConfigurationProperty property = ConfigurationProperty.PROPERTY_BY_NAME.get(propertyName);
      AuditData.get().setData(propertyName,
          property.getStringToValue().apply(tx, systemConfigurationPropertyDAO.get(tx, propertyName)));
      systemConfigurationPropertyDAO.set(tx, propertyName, null);
    }
  }

  public void applyConfigurationToClients(Set<String> propertyNames) {
    configurationListeners.forEach(configurationListener -> configurationListener.configurationChanged(propertyNames));
  }

  // Visible for testing
  void updateAllClusterNodesFromConfiguration(Set<String> propertyNames) {
    applyConfigurationToClients(propertyNames);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TASK_PARAM_PROPERTIES, StringUtils.join(propertyNames, TASK_PARAM_PROPERTIES_DELIMITER));
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(getClass(), TASK_NAME, parameters);
  }

  private void validatePropertyName(String propertyName) {
    if (!ConfigurationProperty.PROPERTY_BY_NAME.containsKey(propertyName)) {
      throw new BadRequestException(String.format(INVALID_PROPERTY_NAME_ERROR_MSG, propertyName));
    }
  }

  private void validatePropertyValue(String propertyName, Object propertyValue) {
    if (propertyValue == null) {
      return;
    }
    Class<?> expectedType = ConfigurationProperty.PROPERTY_BY_NAME.get(propertyName).getType();
    Class<?> actualType = propertyValue.getClass();
    if (!expectedType.isAssignableFrom(actualType)) {
      throw new BadRequestException(
          String.format(INVALID_PROPERTY_VALUE_TYPE_ERROR_MSG, propertyName, expectedType, actualType));
    }
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      if (context.getMergedJobDataMap().containsKey(TASK_PARAM_PROPERTIES)) {
        applyConfigurationToClients(Sets.newHashSet(
            StringUtils.split(context.getMergedJobDataMap().getString(TASK_PARAM_PROPERTIES),
                TASK_PARAM_PROPERTIES_DELIMITER)));
      }
    }
    catch (Exception e) {
      log.error("Error when applying config: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }
}
