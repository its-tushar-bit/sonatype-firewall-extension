/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiMetricsReportingResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.audit.ApplicationAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.OrganizationAuditDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Reproduces the {@code AbstractAuditTest}/{@code AuditTestSupport} audit-log capture (via {@link LogOutput}) and
 * assertion helpers that the legacy {@code ApiMetricsReportingResourceV2AuditTest} inherited from its base class.
 */
@IqH2Test
class IqH2ApiMetricsReportingResourceV2AuditTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  private static final String SYSTEM_USER = MDCUsernameScope.SYSTEM;

  private static final LocalDate BEGIN_DATE = new LocalDate(2017, 11, 1);

  private IqTestContext ctx;

  @RegisterExtension
  private final LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  @BeforeEach
  void setupCommonFixture() {
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void tearDown() {
    logOutput.clear();
  }

  @Test
  void testGetMetrics_Json() throws Exception {
    testExportSuccessMetricsForContentType("application/json");
  }

  @Test
  void testGetMetrics_Csv() throws Exception {
    testExportSuccessMetricsForContentType("text/csv");
  }

  private void testExportSuccessMetricsForContentType(final String contentType) throws Exception {
    Organization org1 = ctx.tempEntity().newOrganization();
    Application app1 = ctx.tempEntity().newApplication(org1.getId());
    Organization org2 = ctx.tempEntity().newOrganization();
    Application app2 = ctx.tempEntity().newApplication(org2.getId());
    LocalDate today = new LocalDate();

    String unknownOrganizationId = "unknownOrganizationId";
    Set<String> queryOrdIds = new HashSet<>(asList(org1.getId(), unknownOrganizationId));
    String unknownApplicationId = "unknownApplicationId";
    Set<String> queryAppIds = new HashSet<>(asList(app1.getId(), app2.getId(), unknownApplicationId));

    metricsReportRequest(contentType).body(makeQueryDTO(queryOrdIds, queryAppIds)).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_SUCCESS_METRICS, null);
    assertCustomData(auditDTO, "beginDate", BEGIN_DATE.toString());
    assertCustomData(auditDTO, "endDate", today.toString());
    assertSelectedOrganizations(auditDTO, new OrganizationAuditDTO(unknownOrganizationId, null),
        new OrganizationAuditDTO(org1.getId(), org1));
    assertSelectedApplications(auditDTO, new ApplicationAuditDTO(unknownApplicationId, null),
        new ApplicationAuditDTO(app2.getId(), app2));
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
  }

  private HttpRequest metricsReportRequest(String acceptType) {
    return ctx.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2, ApiMetricsReportingResourceV2.PATH)
        .header("Accept", acceptType);
  }

  private ApiMetricsReportingQueryDTOV2 makeQueryDTO(Set<String> orgIds, Set<String> appIds) {
    return new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, BEGIN_DATE.toString("yyyy-MM"), null, appIds, orgIds);
  }

  // --- Ported AuditTestSupport helpers (kept self-contained; not on IqTestContext) ---

  private List<AuditDTO> awaitLogEntries(AuditEvent auditEvent, int count) {
    await("Expect audit event " + auditEvent).atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(getLogEntries(auditEvent)).hasSizeGreaterThanOrEqualTo(count));
    return getLogEntries(auditEvent);
  }

  private List<AuditDTO> getLogEntries(AuditEvent auditEvent) {
    return logOutput.getInfoMessages(AuditRecorder.toLoggerName(auditEvent.getDomain()))
        .stream()
        .map(IqH2ApiMetricsReportingResourceV2AuditTest::parseAuditLog)
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

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    return assertAuditLogs(auditEvent, 1, error, null).get(0);
  }

  private List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, int number, String error, String username) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, number);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error, username));
    return auditDTOs;
  }

  private void assertStandardData(AuditDTO auditDTO, AuditEvent auditEvent, String error, String username) {
    boolean systemEvent = SYSTEM_USER.equals(username);
    if (username == null) {
      username = "unauthorized".equals(error) ? unauthorizedUser.getUsername() : User.ADMIN_USERNAME;
    }
    assertThat(auditDTO.domain).isEqualTo(auditEvent.getDomain());
    assertThat(auditDTO.type).isEqualTo(auditEvent.getType());
    assertThat(auditDTO.error).isEqualTo(error);
    assertThat(auditDTO.timestamp).matches("2[0-9]{3}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[-+0-9Z.:]+");
    assertThat(auditDTO.requestMethod).isNull();
    assertThat(auditDTO.requestUri).isNull();
    assertThat(auditDTO.forwarded).isNull();
    if (systemEvent) {
      assertThat(auditDTO.remoteIpAddress).isNull();
      assertThat(auditDTO.userAgent).isNull();
    }
    else {
      assertThat(auditDTO.remoteIpAddress).isNotEmpty();
      assertThat(auditDTO.userAgent).isNotEmpty();
    }
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

  private void assertSelectedApplications(AuditDTO auditDTO, ApplicationAuditDTO... expected) {
    ApplicationAuditDTO[] actuals = JSON.convertValue(auditDTO.data.get("selectedApplications"),
        ApplicationAuditDTO[].class);
    assertThat(actuals).containsExactlyInAnyOrder(expected);
  }

  private void assertSelectedOrganizations(AuditDTO auditDTO, OrganizationAuditDTO... expected) {
    OrganizationAuditDTO[] actuals = JSON.convertValue(auditDTO.data.get("selectedOrganizations"),
        OrganizationAuditDTO[].class);
    assertThat(actuals).containsExactlyInAnyOrder(expected);
  }
}
