/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService.NO_DTO_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.NOT_FOUND_ERROR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ApiReverseProxyAuthenticationConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiReverseProxyAuthenticationConfigurationService service;

  @Inject
  private ReverseProxyAuthenticationConfigurationDAO dao;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private ReverseProxyAuthenticationConfigurationListener mockReverseProxyAuthenticationConfigurationListener;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    Multibinder<ReverseProxyAuthenticationConfigurationListener>
        multiBinder = newSetBinder(binder, ReverseProxyAuthenticationConfigurationListener.class);
    multiBinder.addBinding().toInstance(mockReverseProxyAuthenticationConfigurationListener);

    super.configure(binder);
  }

  @Test
  public void testGetConfiguration_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.getConfiguration())
        .withMessageContaining(NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration() {
    ReverseProxyAuthenticationConfiguration config = tempEntity.newReverseProxyAuthenticationConfiguration();
    assertThat(service.getConfiguration()).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(config);
  }

  @Test
  public void testSetConfiguration_NullDTO() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(null))
        .withMessageContaining(NO_DTO_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_New() {
    ApiReverseProxyAuthenticationConfigurationService spy = spy(service);
    ApiReverseProxyAuthenticationConfigurationDTO dto = new ApiReverseProxyAuthenticationConfigurationDTO();
    dto.enabled = true;
    dto.usernameHeader = "usernameHeader";
    dto.csrfProtectionDisabled = true;
    dto.logoutUrl = "logoutUrl";

    spy.setConfiguration(dto);

    assertThat(service.getConfiguration()).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(dto);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update() {
    ApiReverseProxyAuthenticationConfigurationService spy = spy(service);
    ReverseProxyAuthenticationConfiguration existing = tempEntity.newReverseProxyAuthenticationConfiguration();
    ApiReverseProxyAuthenticationConfigurationDTO dto = new ApiReverseProxyAuthenticationConfigurationDTO();
    dto.enabled = !existing.isEnabled();
    dto.usernameHeader = existing.getUsernameHeader() + "2";
    dto.csrfProtectionDisabled = !existing.isCsrfProtectionDisabled();
    dto.logoutUrl = existing.getLogoutUrl() + "2";

    spy.setConfiguration(dto);

    assertThat(service.getConfiguration()).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(dto);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testDeleteConfiguration_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.deleteConfiguration())
        .withMessageContaining(NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration() {
    ApiReverseProxyAuthenticationConfigurationService spy = spy(service);
    tempEntity.newReverseProxyAuthenticationConfiguration();

    spy.deleteConfiguration();

    assertThat(dao.get()).isNull();
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testUpdateAllClusterNodesFromConfiguration() {
    ApiReverseProxyAuthenticationConfigurationService spy = spy(service);

    spy.updateAllClusterNodesFromConfiguration();

    verify(spy).applyReverseProxyAuthenticationConfigurationToClients();
    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(spy);
  }

  @Test
  public void testApplyReverseProxyAuthenticationConfigurationToClients() {
    service.applyReverseProxyAuthenticationConfigurationToClients();

    verify(mockReverseProxyAuthenticationConfigurationListener).reverseProxyAuthenticationConfigurationChanged();
  }

  @Test
  public void testExecute() throws Exception {
    ApiReverseProxyAuthenticationConfigurationService spy = spy(service);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spy).applyReverseProxyAuthenticationConfigurationToClients();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spy.execute(mock(JobExecutionContext.class));
    }

    verify(spy).applyReverseProxyAuthenticationConfigurationToClients();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ApiReverseProxyAuthenticationConfigurationService.class).build()
        .isConcurrentExectionDisallowed()).isTrue();
  }
}
