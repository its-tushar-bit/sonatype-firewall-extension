/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApplicationManagementServiceTest
    extends AbstractComponentTest
{
  private static final int RESULTS_PER_PAGE = 50;

  @Inject
  private ApplicationManagementService applicationManagementService;

  private Application app1;

  private Application app2;

  @Before
  public void before() {
    final Organization org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("Application 1", "app1", org.getId());
    app2 = tempEntity.newApplicationWithParent("app2", "Application 2");
  }

  @Test
  public void testGetApplicationManagementSummaries_PendingSourceControlEvaluations() {
    // given: app with source control evaluation event in the event queue
    tempEntity.newSourceControlEvaluationEvent(app1);

    // when: fetch summaries
    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.APP_NAME_DESC, 1, RESULTS_PER_PAGE);

    // then: app1 summary indicates an eval is pending
    assertThat(applicationManagementSummaries.stream()
        .filter(summary -> summary.getId().equals(app1.getId()))
        .findFirst().get()
        .getHasPendingSourceControlPolicyEvaluation()).isTrue();

    // and: app2's summary indicates no pending evaluations
    assertThat(applicationManagementSummaries.stream()
        .filter(summary -> summary.getId().equals(app2.getId()))
        .findFirst().get()
        .getHasPendingSourceControlPolicyEvaluation()).isFalse();
  }

  @Test
  public void testGetApplicationSummaries_MissingPageAndPageSize() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> applicationManagementService.getApplicationManagementSummaries(null, null, null, null))
        .withMessage("Request must include required query parameters page and pageSize.");
  }

  @Test
  public void testGetApplicationSummaries_MissingPageSize() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> applicationManagementService.getApplicationManagementSummaries(null, null, 1, null))
        .withMessage("Request must include required query parameters page and pageSize.");
  }

  @Test
  public void testGetApplicationSummaries_MissingPage() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> applicationManagementService
                .getApplicationManagementSummaries(null, null, null, RESULTS_PER_PAGE))
        .withMessage("Request must include required query parameters page and pageSize.");
  }

  @Test
  public void testGetApplicationSummaries() {
    List<Application> apps = new ArrayList<>();
    createAlphabeticalOrgsAndApps(new ArrayList<>(), apps);

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, RESULTS_PER_PAGE);

    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER));
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactlyElementsOf(apps.subList(0, RESULTS_PER_PAGE).stream().map(Application::getName)
            .collect(Collectors.toList()));
  }

  @Test
  public void testGetApplicationSummaries_DifferentPage() {
    List<Application> apps = new ArrayList<>();
    createAlphabeticalOrgsAndApps(new ArrayList<>(), apps);

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.APP_NAME_ASC, 2, RESULTS_PER_PAGE);

    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER));
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactly(apps.get(apps.size() - 1).getName());
  }

  @Test
  public void testGetApplicationSummaries_DifferentPageSize() {
    List<Application> apps = new ArrayList<>();
    createAlphabeticalOrgsAndApps(new ArrayList<>(), apps);

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, 1);

    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER));
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactly(apps.get(0).getName());
  }

  @Test
  public void testGetApplicationSummaries_NameFilter_App() {
    createAlphabeticalOrgsAndApps(new ArrayList<>(), new ArrayList<>());

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("appNameZ", ApplicationManagementSummaryOrder.APP_NAME_ASC, 1,
            RESULTS_PER_PAGE);

    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactly("appNameZ");
  }

  @Test
  public void testGetApplicationSummaries_NameFilter_Org() {
    createAlphabeticalOrgsAndApps(new ArrayList<>(), new ArrayList<>());

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("orgNameZ", ApplicationManagementSummaryOrder.APP_NAME_ASC, 1,
            RESULTS_PER_PAGE);

    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getOrganizationName)
        .containsExactly("orgNameZ");
  }

  @Test
  public void testGetApplicationSummaries_NameFilter_AppOrOrg() {
    createAlphabeticalOrgsAndApps(new ArrayList<>(), new ArrayList<>());
    String nameFilter = "NameaA";

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries(nameFilter, ApplicationManagementSummaryOrder.APP_NAME_ASC, 1,
            RESULTS_PER_PAGE);

    Condition<String> containsNameFilterCaseInsensitive = new Condition<>(
        (String name) -> name.toLowerCase(Locale.ENGLISH).contains(nameFilter.toLowerCase(Locale.ENGLISH)),
        "Contains name filter case insensitive");
    assertThat(applicationManagementSummaries).hasSize(2);
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getName)
        .haveExactly(1, containsNameFilterCaseInsensitive);
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getOrganizationName)
        .haveExactly(1, containsNameFilterCaseInsensitive);
  }

  @Test
  public void testGetApplicationSummaries_Order_AppNameAsc() {
    List<Application> apps = new ArrayList<>();
    createAlphabeticalOrgsAndApps(new ArrayList<>(), apps);

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.APP_NAME_ASC, 1,
            RESULTS_PER_PAGE + 1);

    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER));
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactlyElementsOf(apps.stream().map(Application::getName).collect(Collectors.toList()));
  }

  @Test
  public void testGetApplicationSummaries_Order_AppNameDesc() {
    List<Application> apps = new ArrayList<>();
    createAlphabeticalOrgsAndApps(new ArrayList<>(), apps);

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.APP_NAME_DESC, 1,
            RESULTS_PER_PAGE + 1);

    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER).reversed());
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getName)
        .containsExactlyElementsOf(apps.stream().map(Application::getName).collect(Collectors.toList()));
  }

  @Test
  public void testGetApplicationSummaries_Order_OrgNameAsc() {
    List<Organization> orgs = new ArrayList<>();
    createAlphabeticalOrgsAndApps(orgs, new ArrayList<>());

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.ORG_NAME_ASC, 1,
            RESULTS_PER_PAGE + 1);

    orgs.sort(Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER));
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getOrganizationName)
        .containsExactlyElementsOf(orgs.stream().map(Organization::getName).collect(Collectors.toList()));
  }

  @Test
  public void testGetApplicationSummaries_Order_OrgNameDesc() {
    List<Organization> orgs = new ArrayList<>();
    createAlphabeticalOrgsAndApps(orgs, new ArrayList<>());

    List<ApplicationManagementSummaryDTO> applicationManagementSummaries = applicationManagementService
        .getApplicationManagementSummaries("", ApplicationManagementSummaryOrder.ORG_NAME_DESC, 1,
            RESULTS_PER_PAGE + 1);

    orgs.sort(Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER).reversed());
    assertThat(applicationManagementSummaries).extracting(ApplicationManagementSummaryDTO::getOrganizationName)
        .containsExactlyElementsOf(orgs.stream().map(Organization::getName).collect(Collectors.toList()));
  }

  private void createAlphabeticalOrgsAndApps(List<Organization> orgs, List<Application> apps) {
    orgs.addAll(new OrganizationDAO().getAll().stream()
        .filter(org -> !org.getId().equals(Organization.ROOT_ORGANIZATION_ID)).collect(Collectors.toList()));
    apps.addAll(new ApplicationDAO().getAll());
    int currentSize = apps.size();
    for (int result = 0; result < RESULTS_PER_PAGE + 1 - currentSize; result++) {
      String orgSuffix = getAlphabeticalSequenceElement(result);
      String appSuffix = getAlphabeticalSequenceElement(result + 1);
      Organization org = tempEntity.newOrganization("orgName" + orgSuffix);
      orgs.add(org);
      apps.add(tempEntity.newApplication("appName" + appSuffix, "appPublicId" + appSuffix, org.getId()));
    }
  }

  private String getAlphabeticalSequenceElement(int i) {
    return i < 0 ? "" : getAlphabeticalSequenceElement((i / 26) - 1) + (char) (65 + i % 26);
  }
}
