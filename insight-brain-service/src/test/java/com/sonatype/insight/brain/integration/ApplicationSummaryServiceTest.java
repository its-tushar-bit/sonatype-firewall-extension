/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApplicationSummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationSummaryService service;

  @Test
  public void testGetApplications_SortedByCaseInsensitiveName_EVALUATE_APPLICATION() throws Exception {
    testGetApplications_SortedByCaseInsensitiveName(Goal.EVALUATE_APPLICATION);
  }

  @Test
  public void testGetApplications_SortedByCaseInsensitiveName_EVALUATE_COMPONENT() throws Exception {
    testGetApplications_SortedByCaseInsensitiveName(Goal.EVALUATE_COMPONENT);
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesExist() {
    Application app = tempEntity.newApplicationWithParent();

    boolean result = service.verifyOrCreateApplication(app.getPublicId(), Goal.EVALUATE_APPLICATION);

    assertThat(result, is(true));
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticApplicationCreationDisabled()
      throws Exception
  {
    String appPublicId = "NoSuchAppPublicID";
    tempEntity.registerAppPublicId(appPublicId);

    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setEnabled(false);

    boolean result = service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION);
    assertThat(result, is(false));
  }

  @Test
  public void testVerifyOrCreateApplication_ApplicationDoesNotExist_AutomaticApplicationCreationEnabled()
      throws Exception
  {
    String appPublicId = "NoSuchAppPublicID";
    tempEntity.registerAppPublicId(appPublicId);

    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    boolean result = service.verifyOrCreateApplication(appPublicId, Goal.EVALUATE_APPLICATION);
    assertThat(result, is(true));

    Application app = new ApplicationDAO().getByPublicIdNotNull(appPublicId);
    assertThat(app.getOrganizationId(), is(automaticApplicationsConfigurationDAO.getOrganizationId()));
  }

  private void testGetApplications_SortedByCaseInsensitiveName(Goal goal) throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("y", "AA");
    Application app0 = tempEntity.newApplicationWithParent("z", "a b");
    Application app2 = tempEntity.newApplicationWithParent("x", "c");

    ApplicationSummaryList applicationListDTO = service.getApplications(goal);
    assertThat(applicationListDTO, notNullValue());
    assertThat(applicationListDTO.getApplicationSummaries(), hasSize(3));
    assertThat(applicationListDTO.getApplicationSummaries().get(0).getId(), is(app0.getId()));
    assertThat(applicationListDTO.getApplicationSummaries().get(1).getId(), is(app1.getId()));
    assertThat(applicationListDTO.getApplicationSummaries().get(2).getId(), is(app2.getId()));
  }
}
