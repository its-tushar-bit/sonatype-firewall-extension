/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import io.dropwizard.util.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiConfigurationService service;

  @Inject
  private SystemConfigurationPropertyDAO dao;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private ConfigurationListener mockConfigurationListener;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(ConfigurationListener.class).toInstance(mockConfigurationListener);
    super.configure(binder);
  }

  @Test
  public void testGetConfiguration_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getConfiguration(null)).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration_Empty() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getConfiguration(Collections.emptySet())).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration_InvalidPropertyName() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getConfiguration(Sets.of("invalidPropertyName"))).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_NAME_ERROR_MSG, "invalidPropertyName"));
  }

  @Test
  public void testGetConfiguration() {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    Map<String, Object> configuration = service.getConfiguration(
        Sets.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL));

    assertThat(configuration).hasSize(2).containsEntry(SystemConfigurationProperty.BASE_URL, "http://baseUrl/")
        .containsEntry(SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  public void testSetConfiguration_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(null)).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_Empty() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(Collections.emptyMap())).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_InvalidPropertyName() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("invalidPropertyName", null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(payload)).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_NAME_ERROR_MSG, "invalidPropertyName"));
  }

  @Test
  public void testSetConfiguration_InvalidPropertyValue() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(payload)).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_VALUE_TYPE_ERROR_MSG,
            SystemConfigurationProperty.BASE_URL, String.class, Boolean.class));
  }

  @Test
  public void testSetConfiguration_InvalidUrl() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, "invalid");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(payload))
        .withMessageContaining("Invalid URL: invalid/");
  }

  @Test
  public void testSetConfiguration_NullValues() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, null);
    payload.put(SystemConfigurationProperty.FORCE_BASE_URL, null);

    service.setConfiguration(payload);

    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  public void testSetConfiguration() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    payload.put(SystemConfigurationProperty.FORCE_BASE_URL, true);

    service.setConfiguration(payload);

    assertThat(dao.get(SystemConfigurationProperty.BASE_URL)).isEqualTo("http://baseUrl/");
    assertThat(dao.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo(String.valueOf(Boolean.TRUE));
  }

  @Test
  public void testDeleteConfiguration_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.deleteConfiguration(null)).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration_Empty() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.deleteConfiguration(Collections.emptySet())).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration_InvalidPropertyName() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.deleteConfiguration(Sets.of("invalidPropertyName"))).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_NAME_ERROR_MSG, "invalidPropertyName"));
  }

  @Test
  public void testDeleteConfiguration_NullValues() {
    service.deleteConfiguration(
        Sets.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL));

    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  public void testDeleteConfiguration() {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    service.deleteConfiguration(
        Sets.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL));

    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  public void testUpdateAllClusterNodesFromConfiguration() {
    ApiConfigurationService spy = spy(service);
    Set<String> propertyNames =
        Sets.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);

    spy.updateAllClusterNodesFromConfiguration(propertyNames);

    verify(spy).applyConfigurationToClients(propertyNames);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(ApiConfigurationService.TASK_PARAM_PROPERTIES,
        StringUtils.join(propertyNames, ApiConfigurationService.TASK_PARAM_PROPERTIES_DELIMITER));
    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(ApiConfigurationService.class,
        ApiConfigurationService.TASK_NAME, parameters);
  }

  @Test
  public void testApplyConfigurationToClients() {
    Set<String> propertyNames =
        Sets.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);

    service.applyConfigurationToClients(propertyNames);

    verify(mockConfigurationListener).configurationChanged(propertyNames);
  }

  @Test
  public void testExecute() throws Exception {
    Set<String> propertyNames =
        Sets.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(ApiConfigurationService.TASK_PARAM_PROPERTIES,
        StringUtils.join(propertyNames, ApiConfigurationService.TASK_PARAM_PROPERTIES_DELIMITER));
    JobDataMap jobDataMap = new JobDataMap(parameters);
    ApiConfigurationService spy = spy(service);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spy).applyConfigurationToClients(propertyNames);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spy.execute(mockJobExecutionContext);
    }

    verify(spy).applyConfigurationToClients(propertyNames);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ApiConfigurationService.class).build().isConcurrentExectionDisallowed()).isTrue();
  }
}
