/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

public class ApplicationSummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationSummaryService service;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    // Need to mock this for telemetry requests, otherwise the real client takes a while to timeout.
    binder.bind(HdsClient.class).toInstance(mock(HdsClient.class));
  }

  @Test
  public void testGetApplications_Authorized_NullGoal() {
    grantReadPermission(app.getId());
    ApplicationSummaryList list = service.getApplications(null /* goal */, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplications_Authorized_EVALUATE_COMPONENT() {
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_COMPONENT, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplications_Authorized_EVALUATE_APPLICATION() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_APPLICATION, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplications_Authorized_SUMMARIZE_EVALUATION() {
    grantPermission(app.getId(), Permission.READ);
    ApplicationSummaryList list = service.getApplications(Goal.SUMMARIZE_EVALUATION, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplications_Authorized_VIEW_CIP() {
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);
    ApplicationSummaryList list = service.getApplications(Goal.VIEW_CIP, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplications_Unauthorized_NullGoal() {
    login();
    ApplicationSummaryList list = service.getApplications(null /* goal */, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplications_Unauthorized_EVALUATE_APPLICATION() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_APPLICATION, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplications_Unauthorized_EVALUATE_COMPONENT() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_COMPONENT, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplications_Unauthorized_SUMMARIZE_EVALUATION() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.SUMMARIZE_EVALUATION, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplications_Unauthorized_VIEW_CIP() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.VIEW_CIP, null, null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplicationsByOrganization_Authorized_NullGoal() {
    createOrganizationStructure("B", 2);
    Application app0 = tempEntity.newApplication("x", "x", org.getId());
    Application app1 = tempEntity.newApplication("y", "y", org.getId());
    grantReadPermission(app0.getId());
    grantReadPermission(app1.getId());

    ApplicationSummaryList list = service.getApplications(null /* goal */, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId(), app1.getId());
  }

  @Test
  public void testGetApplicationsByOrganization_Authorized_EVALUATE_COMPONENT() {
    createOrganizationStructure("B", 2);
    Application app0 = tempEntity.newApplication("x", "x", org.getId());
    grantPermission(app0.getId(), Permission.EVALUATE_COMPONENT);

    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_COMPONENT, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId)
            .containsExactly(app0.getId());
  }

  @Test
  public void testGetApplicationsByOrganization_Authorized_EVALUATE_APPLICATION() {
    createOrganizationStructure("B", 2);
    Application app0 = tempEntity.newApplication("x", "x", org.getId());
    grantPermission(app0.getId(), Permission.EVALUATE_APPLICATION);

    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_APPLICATION, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId());
  }

  @Test
  public void testGetApplicationsByOrganization_Authorized_SUMMARIZE_EVALUATION() {
    createOrganizationStructure("B", 2);
    Application app0 = tempEntity.newApplication("x", "x", org.getId());
    grantPermission(app.getId(), Permission.READ);
    grantPermission(app0.getId(), Permission.READ);

    ApplicationSummaryList list = service.getApplications(Goal.SUMMARIZE_EVALUATION, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app.getId(), app0.getId());
  }

  @Test
  public void testGetApplicationsByOrganization_Authorized_VIEW_CIP() {
    createOrganizationStructure("B", 2);
    Application app0 = tempEntity.newApplication("x", "x", org.getId());
    grantPermission(app0.getId(), Permission.EVALUATE_COMPONENT);

    ApplicationSummaryList list = service.getApplications(Goal.VIEW_CIP, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).extracting(ApplicationSummary::getId)
        .containsExactly(app0.getId());
  }

  @Test
  public void testGetApplicationsByOrganization_Unauthorized_NullGoal() {
    login();
    ApplicationSummaryList list = service.getApplications(null /* goal */, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplicationsByOrganization_Unauthorized_EVALUATE_APPLICATION() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_APPLICATION, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplicationsByOrganization_Unauthorized_EVALUATE_COMPONENT() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_COMPONENT, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplicationsByOrganization_Unauthorized_SUMMARIZE_EVALUATION() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.SUMMARIZE_EVALUATION, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  @Test
  public void testGetApplicationsByOrganization_Unauthorized_VIEW_CIP() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.VIEW_CIP, org.getId(), null);
    assertThat(list).isNotNull();
    assertThat(list.getApplicationSummaries()).isEmpty();
  }

  private void createOrganizationStructure(String orgName, int numberOfApps) {
    Organization org1 = tempEntity.newOrganization(orgName);
    tempEntity.newApplications(org1.getId(), numberOfApps);
  }

  @Test
  public void testVerifyOrCreateApplication_Authorized_EVALUATE_APPLICATION() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    assertThat(
        service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_APPLICATION, "ua/1.0")).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplication_Unauthorized_EVALUATE_APPLICATION() {
    login();
    assertThat(
        service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_APPLICATION, "ua/1.0")).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplication_Authorized_EVALUATE_COMPONENT() {
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);
    assertThat(service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_COMPONENT, "ua/1.0")).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplication_Unauthorized_EVALUATE_COMPONENT() {
    login();
    assertThat(service.verifyOrCreateApplication(app.getPublicId(), null, Goal.EVALUATE_COMPONENT, "ua/1.0")).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplication_Authorized_VIEW_CIP() {
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);
    assertThat(service.verifyOrCreateApplication(app.getPublicId(), null, Goal.VIEW_CIP, "ua/1.0")).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplication_Unauthorized_VIEW_CIP() {
    login();
    assertThat(service.verifyOrCreateApplication(app.getPublicId(),null, Goal.VIEW_CIP, "ua/1.0")).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplication_Authorized_SUMMARIZE_EVALUATION() {
    grantPermission(app.getId(), Permission.READ);
    assertThat(
        service.verifyOrCreateApplication(app.getPublicId(), null, Goal.SUMMARIZE_EVALUATION, "ua/1.0")).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplication_Unauthorized_SUMMARIZE_EVALUATION() {
    login();
    assertThat(
        service.verifyOrCreateApplication(app.getPublicId(), null, Goal.SUMMARIZE_EVALUATION, "ua/1.0")).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplication_NullGoal() {
    login();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.verifyOrCreateApplication(app.getPublicId(), null, null, "test_client_user_agent"))
        .withMessage("A goal must be specified");
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_NoAppPermission_EVALUATE_APPLICATION() {
    login();
    String appPublicId = "NoSuchAppPublicId";

    // If the application does not exist, it will be created if automatic app creation is enabled and we have permission
    // to evaluate applications for its configured organization.
    automaticApplicationsConfigurationDAO.setEnabled(false);
    assertThat(service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent")).isFalse();
    assertThat(applicationDAO.getByPublicId(appPublicId)).isNull();

    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(tempEntity.newOrganization().getId());
    assertThat(service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent")).isFalse();
    assertThat(applicationDAO.getByPublicId(appPublicId)).isNull();
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_HasAppPermission_EVALUATE_APPLICATION() {
    login();
    String appPublicId = "NoSuchAppPublicId";

    // We grant the required permission to this organization, which will be inherited by all its applications.
    Organization org = tempEntity.newOrganization();
    grantPermission(org.getId(), Permission.EVALUATE_APPLICATION);

    // If the application does not exist, then "access" is allowed only if automatic app creation is enabled.
    // We should then be able to access it in this scenario because we have permission via the organization.
    automaticApplicationsConfigurationDAO.setEnabled(false);
    assertThat(service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent")).isFalse();
    assertThat(applicationDAO.getByPublicId(appPublicId)).isNull();

    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    assertThat(service.verifyOrCreateApplication(appPublicId, null, Goal.EVALUATE_APPLICATION,
        "test_client_user_agent")).isTrue();
    assertThat(applicationDAO.getByPublicId(appPublicId)).isNotNull();
  }
}
