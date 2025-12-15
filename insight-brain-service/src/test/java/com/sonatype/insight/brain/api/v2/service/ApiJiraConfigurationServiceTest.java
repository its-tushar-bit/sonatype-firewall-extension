/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Binder;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService.BAD_CONFIG_ERROR_MSG;
import static com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService.NO_CONFIG_ERROR_MSG;
import static com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService.NO_PASSWORD_ERROR_MSG;
import static com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService.NO_USERNAME_PASSWORD_PAIR;
import static com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO.NOT_FOUND_ERROR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ApiJiraConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiJiraConfigurationService service;

  @Inject
  private JiraConfigurationDAO dao;

  @Inject
  private PasswordHandler passwordHandler;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private JiraConfigurationListener mockJiraConfigurationListener;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);

    // Use Multibinder to add test listeners to the Set<ConfigurationListener>
    com.google.inject.multibindings.Multibinder<JiraConfigurationListener> listenerBinder =
        com.google.inject.multibindings.Multibinder.newSetBinder(binder, JiraConfigurationListener.class);
    listenerBinder.addBinding().toInstance(mockJiraConfigurationListener);

    super.configure(binder);
  }

  @Test
  public void testGetConfiguration_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.getConfiguration())
        .withMessageContaining(NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration() {
    JiraConfiguration config = tempEntity.newJiraConfiguration();
    assertThat(service.getConfiguration()).usingRecursiveComparison().ignoringFields("password").isEqualTo(config);
  }

  @Test
  public void testSetConfiguration_NullConfig() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(null))
        .withMessageContaining(NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_BadConfig() {
    ObjectNode badConfig = new ObjectMapper().createObjectNode();
    badConfig.putArray("url");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(badConfig))
        .withMessageContaining(BAD_CONFIG_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_New_OnlyUsername() {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.username = "username";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> service.setConfiguration(asTreeIgnoreNull(dto)))
        .withMessageContaining(NO_USERNAME_PASSWORD_PAIR);
  }

  @Test
  public void testSetConfiguration_New_OnlyPassword() {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.password = "password".toCharArray();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> service.setConfiguration(asTreeIgnoreNull(dto)))
        .withMessageContaining(NO_USERNAME_PASSWORD_PAIR);
  }

  @Test
  public void testSetConfiguration_New_OnlyUrl() {
    ApiJiraConfigurationService spy = spy(service);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";

    spy.setConfiguration(asTreeIgnoreNull(dto));

    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(dto);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_New() {
    ApiJiraConfigurationService spy = spy(service);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.username = "username";
    dto.password = "password".toCharArray();
    dto.customFields = Maps.newHashMap("field", "value");

    spy.setConfiguration(asTreeIgnoreNull(dto));

    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison().ignoringExpectedNullFields().ignoringFields("password")
        .isEqualTo(dto);
    assertThat(passwordHandler.decryptPassword(jiraConfiguration.getPassword())).isEqualTo(dto.password);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update_Url_NeedPassword() {
    JiraConfiguration existing = tempEntity.newJiraConfiguration();
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = existing.getUrl() + "2";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> service.setConfiguration(asTreeIgnoreNull(dto)))
        .withMessageContaining(NO_PASSWORD_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_Update_Url() {
    ApiJiraConfigurationService spy = spy(service);
    JiraConfiguration existing = tempEntity.newJiraConfiguration("http://url", null, null, null);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = existing.getUrl() + "2";

    spy.setConfiguration(asTreeIgnoreNull(dto));

    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).ignoringFields("url")
        .isEqualTo(existing);
    assertThat(jiraConfiguration.getUrl()).isEqualTo(dto.url);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update_NoCredentials_To_OnlyUsername() {
    tempEntity.newJiraConfiguration("http://url", null, null, null);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.username = "username";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> service.setConfiguration(asTreeIgnoreNull(dto)))
        .withMessageContaining(NO_USERNAME_PASSWORD_PAIR);
  }

  @Test
  public void testSetConfiguration_Update_NoCredentials_To_OnlyPassword() {
    tempEntity.newJiraConfiguration("http://url", null, null, null);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.password = "password".toCharArray();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> service.setConfiguration(asTreeIgnoreNull(dto)))
        .withMessageContaining(NO_USERNAME_PASSWORD_PAIR);
  }

  @Test
  public void testSetConfiguration_Update_Credentials_To_OnlyUsername() {
    tempEntity.newJiraConfiguration("http://url", "username", "password".toCharArray(), null);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> service.setConfiguration(asTree(dto, "url", "username", "customFields")))
        .withMessageContaining(NO_USERNAME_PASSWORD_PAIR);
  }

  @Test
  public void testSetConfiguration_Update_Credentials_To_OnlyPassword() {
    tempEntity.newJiraConfiguration("http://url", "username", "password".toCharArray(), null);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
            () -> service.setConfiguration(asTree(dto, "url", "password", "customFields")))
        .withMessageContaining(NO_USERNAME_PASSWORD_PAIR);
  }

  @Test
  public void testSetConfiguration_Update_Credentials_To_NoCredentials() {
    ApiJiraConfigurationService spy = spy(service);
    JiraConfiguration existing = tempEntity.newJiraConfiguration();
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();

    spy.setConfiguration(asTree(dto, "url", "customFields"));

    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("username")
        .ignoringFields("password")
        .isEqualTo(existing);
    assertThat(jiraConfiguration.getUsername()).isNull();
    assertThat(jiraConfiguration.getPassword()).isNull();
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update_UsernamePassword() {
    ApiJiraConfigurationService spy = spy(service);
    JiraConfiguration existing = tempEntity.newJiraConfiguration("http://url", null, null, null);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.username = "username";
    dto.password = "password".toCharArray();

    spy.setConfiguration(asTreeIgnoreNull(dto));

    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("username")
        .ignoringFields("password")
        .isEqualTo(existing);
    assertThat(jiraConfiguration.getUsername()).isEqualTo(dto.username);
    assertThat(passwordHandler.decryptPassword(jiraConfiguration.getPassword())).isEqualTo(dto.password);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update_CustomFields() {
    ApiJiraConfigurationService spy = spy(service);
    JiraConfiguration existing = tempEntity.newJiraConfiguration("http://url", null, null, null);
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.customFields = Maps.newHashMap("field", "value");

    spy.setConfiguration(asTreeIgnoreNull(dto));

    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("customFields", "customFieldsJson")
        .isEqualTo(existing);
    assertThat(jiraConfiguration.getCustomFields()).isEqualTo(dto.customFields);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update_All() {
    ApiJiraConfigurationService spy = spy(service);
    JiraConfiguration existing = tempEntity.newJiraConfiguration();
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = existing.getUrl() + "2";
    dto.username = existing.getUsername() + "2";
    dto.password = (String.valueOf(existing.getPassword()) + "2").toCharArray();
    Map<String, Object> newCustomFields = new HashMap<>(existing.getCustomFields());
    newCustomFields.put("other", "value");
    dto.customFields = newCustomFields;

    spy.setConfiguration(asTreeIgnoreNull(dto));

    JiraConfiguration jiraConfiguration = dao.get();
    assertThat(jiraConfiguration).usingRecursiveComparison().ignoringExpectedNullFields().ignoringFields("password")
        .isEqualTo(dto);
    assertThat(passwordHandler.decryptPassword(jiraConfiguration.getPassword())).isEqualTo(dto.password);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testDeleteConfiguration_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.deleteConfiguration())
        .withMessageContaining(NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration() {
    ApiJiraConfigurationService spy = spy(service);
    tempEntity.newJiraConfiguration();

    spy.deleteConfiguration();

    assertThat(dao.get()).isNull();
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testUpdateAllClusterNodesFromConfiguration() {
    ApiJiraConfigurationService spy = spy(service);

    spy.updateAllClusterNodesFromConfiguration();

    verify(spy).applyJiraConfigurationToClients();
    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(spy);
  }

  @Test
  public void testApplyJiraConfigurationToClients() {
    service.applyJiraConfigurationToClients();

    verify(mockJiraConfigurationListener).jiraConfigurationChanged();
  }

  @Test
  public void testExecute() throws Exception {
    ApiJiraConfigurationService spy = spy(service);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spy).applyJiraConfigurationToClients();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spy.execute(mock(JobExecutionContext.class));
    }

    verify(spy).applyJiraConfigurationToClients();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ApiJiraConfigurationService.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  private JsonNode asTreeIgnoreNull(ApiJiraConfigurationDTO dto) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(Include.NON_NULL);
    return objectMapper.valueToTree(dto);
  }

  private JsonNode asTree(ApiJiraConfigurationDTO dto, String... excludes) {
    JsonMapper jsonMapper = JsonMapper.builder().disable(MapperFeature.USE_ANNOTATIONS).build();
    ObjectNode objectNode = jsonMapper.valueToTree(dto);
    for (String exclude : excludes) {
      objectNode.remove(exclude);
    }
    return objectNode;
  }
}
