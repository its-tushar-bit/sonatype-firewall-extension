/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IQ Server on PostgreSQL — {@link ApiFirewallResource} audit logging. In the original resource's
 * package because {@code ApiFirewallResource}'s path constants are package-private. No base class; the
 * {@code AuditTestSupport}/{@code LogOutput} log-capture behaviour from the legacy {@code AbstractAuditTest}
 * is inlined here since it is not part of {@link IqTestContext}.
 */
@IqPostgresTest
class IqPostgresApiFirewallResourceAuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private RepositoryManagerDAO repositoryManagerDAO;

  private final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  private Level originalLevel;

  private User unauthorizedUser;

  @BeforeEach
  void setUp() {
    repositoryManagerDAO = ctx.lookup(RepositoryManagerDAO.class);

    Logger auditLogger = loggerContext.getLogger(AuditRecorder.BASE_LOGGER_NAME);
    originalLevel = auditLogger.getLevel();
    auditLogger.setLevel(Level.DEBUG);
    appender.list = Collections.synchronizedList(appender.list);
    appender.setContext(loggerContext);
    appender.start();
    auditLogger.detachAppender(appender);
    auditLogger.addAppender(appender);
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void tearDownLogCapture() {
    Logger auditLogger = loggerContext.getLogger(AuditRecorder.BASE_LOGGER_NAME);
    auditLogger.detachAppender(appender);
    auditLogger.setLevel(originalLevel);
    appender.stop();
  }

  @Test
  void testSetFirewallAutoUnquarantineConfig() throws Exception {
    // setup: add new dto to list
    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO dto = new ApiFirewallReleaseQuarantineConfigDTO();
    dto.autoReleaseQuarantineEnabled = true;
    dto.id = LicenseConditionType.ID;
    list.add(dto);

    // when: setting firewall auto unquarantine config
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
            ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH)
        .body(list)
        .put();
    ApiFirewallReleaseQuarantineConfigDTO[] dtos = response.getBody(ApiFirewallReleaseQuarantineConfigDTO[].class);
    ctx.assertResponseStatus(200, response);

    // then: expect returned dtos to be greater than zero
    assertThat(dtos).isNotNull().isNotEmpty();

    // then: expect audit log entries to be created
    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, 1).get(0);
    assertRepositoryContainerData(auditLog);
    assertCustomData(auditLog, "stageId", StageTypes.PROXY.getId());
  }

  @Test
  void testSetQuarantinedComponentViewAnonymousAccess() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
            ApiFirewallResource.QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS_SET)
        .parameter(false)
        .put();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_SECURITY_QUARANTINED_COMPONENT_VIEW_ANON_ACCESS, 1)
        .get(0);
    assertCustomData(auditLog, "enabled", false);
  }

  @Test
  void testConfigureRepositories() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    repository.setAuditEnabled(!repository.isAuditEnabled());
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    dto.repositories = Collections.singletonList(ApiRepositoryDTO.fromRepository(repository));

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORIES_CONFIGURATION_PATH)
        .parameter(repository.getRepositoryManagerId())
        .body(dto)
        .post();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_REPOSITORY, 1).get(0);
    assertCustomData(auditLog, "repositoryManagerId", repository.getRepositoryManagerId());
    assertRepositoryData(auditLog, repository);
  }

  @Test
  void testAddRepositoryManager() throws Exception {
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGERS_PATH)
            .body(apiRepositoryManagerDTO)
            .post();

    ctx.assertResponseStatus(200, response);
    apiRepositoryManagerDTO = response.getBody(ApiRepositoryManagerDTO.class);
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(apiRepositoryManagerDTO.id);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_REPOSITORY_MANAGER, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  void testAddRepositoryManager_Unauthorized() throws Exception {
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    ctx.restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGERS_PATH)
        .with(unauthorizedUser())
        .body(apiRepositoryManagerDTO)
        .post();

    assertAuditLog(AuditEvent.CREATE_REPOSITORY_MANAGER, "unauthorized");
  }

  @Test
  void testDeleteRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    ctx.restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGER_PATH)
        .parameter(repositoryManager.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_MANAGER, null /* error */);
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  void testDeleteRepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    ctx.restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGER_PATH)
        .parameter(repositoryManager.getId())
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.DELETE_REPOSITORY_MANAGER, "unauthorized");
  }

  // --- inlined AuditTestSupport/AbstractAuditTest behaviour (not part of IqTestContext) ----------

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLogs(auditEvent, 1, error).get(0);
  }

  private List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, number);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error));
    return auditDTOs;
  }

  private void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error) {
    String username = "unauthorized".equals(error) ? unauthorizedUser.getUsername() : User.ADMIN_USERNAME;
    assertThat(auditDTO.domain).isEqualTo(auditEvent.getDomain());
    assertThat(auditDTO.type).isEqualTo(auditEvent.getType());
    assertThat(auditDTO.error).isEqualTo(error);
    assertThat(auditDTO.timestamp).matches("2[0-9]{3}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[-+0-9Z.:]+");
    assertThat(auditDTO.requestMethod).isNull();
    assertThat(auditDTO.requestUri).isNull();
    assertThat(auditDTO.forwarded).isNull();
    assertThat(auditDTO.remoteIpAddress).isNotEmpty();
    assertThat(auditDTO.userAgent).isNotEmpty();
    assertThat(auditDTO.username).isEqualTo(username);
  }

  private void assertCustomData(AuditDTO auditDTO, String key, Object value) {
    if (value == null) {
      assertThat(auditDTO.data).doesNotContainKey(key);
    }
    else {
      assertThat(auditDTO.data).containsEntry(key, value);
    }
  }

  private void assertRepositoryContainerData(AuditDTO auditDTO) {
    assertThat(auditDTO.data).containsEntry("scope", "all-repositories");
  }

  private void assertRepositoryData(AuditDTO auditDTO, Repository repository) {
    assertCustomData(auditDTO, "repositoryId", repository.getId());
    assertCustomData(auditDTO, "repositoryPublicId", repository.getPublicId());
    assertCustomData(auditDTO, "format", repository.getFormat());
    assertCustomData(auditDTO, "type", repository.getRepositoryType().name());
    assertCustomData(auditDTO, "auditEnabled", repository.isAuditEnabled());
    assertCustomData(auditDTO, "quarantineEnabled", repository.isQuarantineEnabled());
    assertCustomData(auditDTO, "policyCompliantComponentSelectionEnabled",
        repository.isPolicyCompliantComponentSelectionEnabled());
    assertCustomData(auditDTO, "namespaceConfusionProtectionEnabled",
        repository.isNamespaceConfusionProtectionEnabled());
  }

  private void assertRepositoryManagerData(AuditDTO auditDTO, RepositoryManager repositoryManager) {
    assertCustomData(auditDTO, "repositoryManagerId", repositoryManager.getId());
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryManagerName", repositoryManager.getName());
  }

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    String loggerName = AuditRecorder.toLoggerName(auditEvent.getDomain());
    List<ILoggingEvent> events;
    synchronized (appender.list) {
      events = new ArrayList<>(appender.list);
    }
    return events.stream()
        .filter(event -> Level.INFO.equals(event.getLevel()) && loggerName.equals(event.getLoggerName()))
        .map(event -> parseAuditLog(event.getFormattedMessage()))
        .filter(dto -> auditEvent.getType().equals(dto.type))
        .collect(toCollection(ArrayList::new));
  }

  private static AuditDTO parseAuditLog(String auditLogEntry) {
    try {
      return JSON.readValue(auditLogEntry, AuditDTO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
