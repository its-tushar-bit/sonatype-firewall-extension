/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

public class ApplicationSummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationSummaryService service;

  @Inject
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Test
  public void testGetApplications_Authorized_NullGoal() {
    grantReadPermission(app.getId());
    ApplicationSummaryList list = service.getApplications(null /* goal */);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(1));
  }

  @Test
  public void testGetApplications_Authorized_EVALUATE_COMPONENT() {
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_COMPONENT);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(1));
  }

  @Test
  public void testGetApplications_Authorized_EVALUATE_APPLICATION() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_APPLICATION);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(1));
  }

  @Test
  public void testGetApplications_Authorized_SUMMARIZE_EVALUATION() {
    grantPermission(app.getId(), Permission.READ);
    ApplicationSummaryList list = service.getApplications(Goal.SUMMARIZE_EVALUATION);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(1));
  }

  @Test
  public void testGetApplications_Unauthorized_NullGoal() {
    login();
    ApplicationSummaryList list = service.getApplications(null /* goal */);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(0));
  }

  @Test
  public void testGetApplications_Unauthorized_EVALUATE_APPLICATION() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_APPLICATION);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(0));
  }

  @Test
  public void testGetApplications_Unauthorized_EVALUATE_COMPONENT() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.EVALUATE_COMPONENT);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(0));
  }

  @Test
  public void testGetApplications_Unauthorized_SUMMARIZE_EVALUATION() {
    login();
    ApplicationSummaryList list = service.getApplications(Goal.SUMMARIZE_EVALUATION);
    assertThat(list, is(notNullValue()));
    assertThat(list.getApplicationSummaries(), hasSize(0));
  }

  @Test
  public void testIsApplicationAllowed_Authorized_EVALUATE_APPLICATION() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    assertThat(service.isApplicationAllowed(app.getPublicId(), Goal.EVALUATE_APPLICATION), is(true));
  }

  @Test
  public void testIsApplicationAllowed_Unauthorized_EVALUATE_APPLICATION() {
    login();
    assertThat(service.isApplicationAllowed(app.getPublicId(), Goal.EVALUATE_APPLICATION), is(false));
  }

  @Test
  public void testIsApplicationAllowed_NullGoal() {
    login();
    try {
      service.isApplicationAllowed(app.getPublicId(), null /* goal */);
      fail("Expected exception");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("A goal must be specified"));
    }
  }

  @Test
  public void testIsApplicationAllowed_ApplicationDoesNotExist_EVALUATE_APPLICATION() {
    login();
    String appPublicId = "NoSuchAppPublicId";
    tempEntity.registerAppPublicId(appPublicId);

    // If the application does not exist, then "access" is allowed only if automatic app creation is enabled.
    automaticApplicationsConfigurationDAO.setEnabled(false);
    assertThat(service.isApplicationAllowed(appPublicId, Goal.EVALUATE_APPLICATION), is(false));

    automaticApplicationsConfigurationDAO.setEnabled(true);
    assertThat(service.isApplicationAllowed(appPublicId, Goal.EVALUATE_APPLICATION), is(true));
  }
}
