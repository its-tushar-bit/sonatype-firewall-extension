/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ApiProxyServerConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiProxyServerConfigurationService proxyServerConfigurationService;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Mock
  private ProxyServerConfigurationListener proxyServerConfigurationListener;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Override
  public void configure(Binder binder) {
    Multibinder<ProxyServerConfigurationListener>
        multiBinder = newSetBinder(binder, ProxyServerConfigurationListener.class);
    multiBinder.addBinding().toInstance(proxyServerConfigurationListener);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Test
  public void testGetConfiguration() {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("servtest");
    proxyServerConfiguration.setPort(58285);
    proxyServerConfiguration.setUsername("smtpuser");
    proxyServerConfiguration.setPassword(passwordHandler.encryptPassword("smtppass".toCharArray()));
    proxyServerConfiguration.setExcludeHosts("host1, host2");
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = proxyServerConfigurationService.getConfiguration();
    assertThat(configurationDTO.hostname).isEqualTo(proxyServerConfiguration.getHostname());
    assertThat(configurationDTO.port).isEqualTo(proxyServerConfiguration.getPort());
    assertThat(configurationDTO.username).isEqualTo(proxyServerConfiguration.getUsername());
    assertThat(configurationDTO.password).isNull();
    assertThat(configurationDTO.passwordIsIncluded).isFalse();
    assertThat(configurationDTO.excludeHosts).isEqualTo(proxyServerConfiguration.getExcludeHostsList());
  }

  @Test
  public void testGetConfiguration_NoConfiguration() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> proxyServerConfigurationService.getConfiguration())
        .withMessageContaining("Proxy server not configured");
  }

  @Test
  public void testSetConfiguration_Insert_PasswordNotNull() {
    proxyServerConfigurationDAO.delete();
    char[] password = "smtppass".toCharArray();
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = password.clone();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(passwordHandler.decryptPassword(proxyServerConfiguration.getPassword())).isEqualTo(password);
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotNull() {
    char[] encryptedPassword = passwordHandler.encryptPassword("smtppass".toCharArray());
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setPassword(encryptedPassword);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    char[] password = "smtppass".toCharArray();
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = password.clone();
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(passwordHandler.decryptPassword(proxyServerConfiguration.getPassword())).isEqualTo(password);
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Insert_PasswordNull() {
    proxyServerConfigurationDAO.delete();
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = null;
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(proxyServerConfiguration.getPassword()).isNull();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Insert_PasswordEmpty() {
    proxyServerConfigurationDAO.delete();
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = new char[0];
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(proxyServerConfiguration.getPassword()).isNull();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Update_PasswordNull() {
    char[] encryptedPassword = passwordHandler.encryptPassword("smtppass".toCharArray());
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setPassword(encryptedPassword);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = null;
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(proxyServerConfiguration.getPassword()).isNull();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Update_PasswordEmpty() {
    char[] encryptedPassword = passwordHandler.encryptPassword("smtppass".toCharArray());
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setPassword(encryptedPassword);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = new char[0];
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Arrays.asList("host1", "host2");

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(proxyServerConfiguration.getPassword()).isNull();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("host1, host2");

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotIncluded_HostnameUnchanged_PortUnchanged() {
    char[] encryptedPassword = passwordHandler.encryptPassword("smtppass".toCharArray());
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setPassword(encryptedPassword);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = proxyServerConfiguration.getHostname();
    configurationDTO.port = proxyServerConfiguration.getPort();
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "mysecret".toCharArray();
    configurationDTO.passwordIsIncluded = false;

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    assertThat(configurationDTO.password).containsOnly('0');

    proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(proxyServerConfiguration.getPassword()).isEqualTo(encryptedPassword);

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotIncluded_HostnameChanged_PortUnchanged() {
    char[] encryptedPassword = passwordHandler.encryptPassword("smtppass".toCharArray());
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setPassword(encryptedPassword);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "othertest";
    configurationDTO.port = proxyServerConfiguration.getPort();
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "mysecret".toCharArray();
    configurationDTO.passwordIsIncluded = false;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> proxyServerConfigurationService.setConfiguration(configurationDTO))
        .withMessageContaining("The password must be provided when the hostname or port are updated");

    assertThat(configurationDTO.password).containsOnly('0');

    // Verify the stored configuration was not changed
    ProxyServerConfiguration storedProxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(storedProxyServerConfiguration.getHostname()).isEqualTo(proxyServerConfiguration.getHostname());
    assertThat(storedProxyServerConfiguration.getPort()).isEqualTo(proxyServerConfiguration.getPort());
    assertThat(storedProxyServerConfiguration.getUsername()).isEqualTo(proxyServerConfiguration.getUsername());
    assertThat(storedProxyServerConfiguration.getPassword()).isEqualTo(proxyServerConfiguration.getPassword());

    verifyNoInteractions(proxyServerConfigurationListener);
  }

  @Test
  public void testSetConfiguration_Update_InvalidHostname() {
    testInvalidHostname("invalid/host/name");
  }

  @Test
  public void testSetConfiguration_Update_NullHostname() {
    testInvalidHostname(null);
  }

  private void testInvalidHostname(String hostname) {
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = hostname;
    configurationDTO.port = 1;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "mysecret".toCharArray();
    configurationDTO.passwordIsIncluded = true;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> proxyServerConfigurationService.setConfiguration(configurationDTO))
        .withMessageContaining("Invalid hostname provided for the proxy server");
  }

  @Test
  public void testSetConfiguration_Update_PasswordNotIncluded_HostnameUnchanged_PortChanged() {
    char[] encryptedPassword = passwordHandler.encryptPassword("smtppass".toCharArray());
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setPassword(encryptedPassword);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = proxyServerConfiguration.getHostname();
    configurationDTO.port = 2;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = "mysecret".toCharArray();
    configurationDTO.passwordIsIncluded = false;

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> proxyServerConfigurationService.setConfiguration(configurationDTO))
        .withMessageContaining("The password must be provided when the hostname or port are updated");

    assertThat(configurationDTO.password).containsOnly('0');

    // Verify the stored configuration was not changed
    ProxyServerConfiguration storedProxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(storedProxyServerConfiguration.getHostname()).isEqualTo(proxyServerConfiguration.getHostname());
    assertThat(storedProxyServerConfiguration.getPort()).isEqualTo(proxyServerConfiguration.getPort());
    assertThat(storedProxyServerConfiguration.getUsername()).isEqualTo(proxyServerConfiguration.getUsername());
    assertThat(storedProxyServerConfiguration.getPassword()).isEqualTo(proxyServerConfiguration.getPassword());

    verifyNoInteractions(proxyServerConfigurationListener);
  }

  @Test
  public void testSetConfiguration_NoRequestDTO() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> proxyServerConfigurationService.setConfiguration(null))
        .withMessage("No proxy server configuration was provided.");
  }

  @Test
  public void testDeleteConfiguration() {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("servtest");
    proxyServerConfiguration.setPort(58285);
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    proxyServerConfigurationService.deleteConfiguration();

    assertThat(proxyServerConfigurationDAO.get()).isNull();

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testDeleteConfiguration_NoConfiguration() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> proxyServerConfigurationService.deleteConfiguration())
        .withMessageContaining("Proxy server not configured");

    verifyNoInteractions(proxyServerConfigurationListener);
  }

  @Test
  public void testSetConfiguration_Insert_ExcludeHostsNull() {
    proxyServerConfigurationDAO.delete();
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = null;
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = null;

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(proxyServerConfiguration.getPassword()).isNull();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isNull();

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Insert_ExcludeHostsEmpty() {
    proxyServerConfigurationDAO.delete();
    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "servtest";
    configurationDTO.port = 58285;
    configurationDTO.username = "smtpuser";
    configurationDTO.password = null;
    configurationDTO.passwordIsIncluded = true;
    configurationDTO.excludeHosts = Collections.emptyList();

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    ProxyServerConfiguration proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo(configurationDTO.hostname);
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(configurationDTO.port);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo(configurationDTO.username);
    assertThat(proxyServerConfiguration.getPassword()).isNull();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isNull();

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Update_ExcludeHostsNull() {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setExcludeHosts("host1, host2");
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "test";
    configurationDTO.port = 1;
    configurationDTO.excludeHosts = null;

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isNull();

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testSetConfiguration_Update_ExcludeHostsEmpty() {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("test");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setExcludeHosts("host1, host2");
    proxyServerConfigurationDAO.set(proxyServerConfiguration);

    ApiProxyServerConfigurationDTO configurationDTO = new ApiProxyServerConfigurationDTO();
    configurationDTO.hostname = "test";
    configurationDTO.port = 1;
    configurationDTO.excludeHosts = Collections.emptyList();

    proxyServerConfigurationService.setConfiguration(configurationDTO);

    proxyServerConfiguration = proxyServerConfigurationDAO.get();
    assertThat(proxyServerConfiguration.getExcludeHosts()).isNull();

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testExecute() {
    ApiProxyServerConfigurationService proxyServerConfigurationServiceSpy = spy(proxyServerConfigurationService);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(proxyServerConfigurationServiceSpy).applyProxyServerConfigurationToClients();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      proxyServerConfigurationServiceSpy.execute(mock(JobExecutionContext.class));
    }

    verify(proxyServerConfigurationServiceSpy).applyProxyServerConfigurationToClients();
  }

  @Test
  public void testUpdateAllClusterNodesFromConfiguration() {
    ApiProxyServerConfigurationService proxyServerConfigurationServiceSpy = spy(proxyServerConfigurationService);

    proxyServerConfigurationServiceSpy.updateAllClusterNodesFromConfiguration();

    verify(proxyServerConfigurationServiceSpy).applyProxyServerConfigurationToClients();
    verify(taskSchedulerMock).scheduleOneTimeTaskForAllOtherNodes(proxyServerConfigurationServiceSpy);
  }
}
