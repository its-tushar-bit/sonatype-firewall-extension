/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DevelopmentPrioritiesReportServiceTest
    extends AbstractComponentTest
{
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final String GIVEN_SOME_PUBLIC_APP_ID = "any-app-id";

  private static final String GIVEN_SOME_SCAN_ID = "any-scan-id";

  @Inject
  private ApplicationDAO applicationDAO;

  @Mock
  private ReportService reportService;

  @Mock
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  private DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  @Before
  public void setup() throws IOException {
    developmentPrioritiesReportService = new DevelopmentPrioritiesReportService(
        applicationDAO,
        reportService,
        apiReportDataServiceV2);
  }

  @Test
  public void testGetDependencyInformation_shouldThrowNotFoundExceptionGivenIOException() throws IOException {
    when(apiReportDataServiceV2.getRawData(anyString(), anyString())).thenThrow(new IOException());

    final String expectedErrorMessage = "Could not find the requested report for prioritization.";
    assertThatThrownBy(() ->
        developmentPrioritiesReportService.getDependencyInformation(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID))
        .withFailMessage(expectedErrorMessage)
        .isInstanceOf(NotFoundException.class);

    verify(apiReportDataServiceV2).getRawData(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);
  }

  @Test
  public void testGetPolicyThreats_shouldThrowNotFoundExceptionGivenNoReportFile() {
    final Organization org = tempEntity.newOrganization();
    final Application application =  tempEntity.newApplication(GIVEN_SOME_PUBLIC_APP_ID, org.getPublicId());
    final String givenAppIdReturned = application.getId();

    final String expectedErrorMessage = "Could not find the requested report for prioritization.";

    assertThatThrownBy(() ->
        developmentPrioritiesReportService.getPolicyThreatsNoAuth(GIVEN_SOME_PUBLIC_APP_ID, givenAppIdReturned))
        .withFailMessage(expectedErrorMessage)
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testGetPolicyThreats_shouldThrowNotFoundExceptionGivenNoReportEntryForPolicyThreatFounds() {
    final Organization org = tempEntity.newOrganization();
    final Application application =  tempEntity.newApplication(GIVEN_SOME_PUBLIC_APP_ID, org.getPublicId());
    final String givenAppIdReturned = application.getId();

    final String expectedErrorMessage = "Could not find the requested report for prioritization.";

    final File givenFileReturnedFromReportService = new File(Report.POLICY_THREATS);
    when(reportService.getReport(anyString(), anyString())).thenReturn(givenFileReturnedFromReportService);

    try (MockedStatic<Report> dataBaseUtil = mockStatic(Report.class)) {
      when(Report.getEntry(any(), any())).thenReturn(null);

      assertThatThrownBy(() ->
          developmentPrioritiesReportService.getPolicyThreatsNoAuth(GIVEN_SOME_PUBLIC_APP_ID, givenAppIdReturned))
          .withFailMessage(expectedErrorMessage)
          .isInstanceOf(NotFoundException.class);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testGetPolicyThreats_shouldReturnPolicyThreatsGivenPolicyThreatFileInReport() throws IOException {
    final Organization org = tempEntity.newOrganization();
    final Application application =  tempEntity.newApplication(GIVEN_SOME_PUBLIC_APP_ID, org.getPublicId());
    final PolicyThreats givenPolicyThreatsStoredForReport = createPolicyThreat();

    final ReportEntry givenReportEntryReturned =
        new ReportEntry(Report.POLICY_THREATS, 1L, objectMapper.writeValueAsBytes(givenPolicyThreatsStoredForReport));

    final File givenFileReturnedFromReportService = new File(Report.POLICY_THREATS);
    when(reportService.getReport(anyString(), anyString())).thenReturn(givenFileReturnedFromReportService);

    try (MockedStatic<Report> dataBaseUtil = mockStatic(Report.class)) {
      when(Report.getEntry(any(), any())).thenReturn(givenReportEntryReturned);

      final PolicyThreats result = developmentPrioritiesReportService
          .getPolicyThreatsNoAuth(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

      verify(reportService).getReport(application.getId(), GIVEN_SOME_SCAN_ID);

      assertThat(result.aaData.size()).isEqualTo(1);
      assertThat(result.aaData.get(0).componentIdentifier)
          .isEqualTo(givenPolicyThreatsStoredForReport.aaData.get(0).componentIdentifier);

    }
  }

  private PolicyThreats createPolicyThreat() {
    final PolicyThreats policyThreats = new PolicyThreats();

    final Component component = createComponent();

    final PolicyViolation policyViolation = createPolicyViolation();

    component.activeViolations.add(policyViolation);
    component.allViolations.add(policyViolation);

    policyThreats.aaData.add(component);

    return policyThreats;
  }

  private PolicyViolation createPolicyViolation() {
    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.policyThreatLevel = 9;
    policyViolation.policyId = "some-policy-id";
    policyViolation.policyViolationId = "some-violation-id";

    return policyViolation;
  }

  private Component createComponent() {
    final Component component = new Component();
    component.hash = "aaa";
    final Map<String, String > coordinate = new HashMap<>();
    coordinate.put("extension", "jar");
    coordinate.put("groupId", "com.sonatype");
    coordinate.put("artifactId", "test");
    coordinate.put("version", "1.1.1");

    component.componentIdentifier = new ComponentIdentifier("maven", coordinate);

    return component;
  }
}
