/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class SampleDataCreatorTest
{
  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @After
  public void cleanup() {
    Application app = applicationDAO.getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    Organization org = organizationDAO.getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    if (app != null) {
      applicationDAO.delete(app);
    }
    if (org != null) {
      organizationDAO.delete(org);
    }
  }

  @Test
  public void testCreateSampleData() throws Exception {
    new SampleDataCreator().createSampleData();

    List<Organization> organizations = organizationDAO.getAll();
    assertThat(organizations, hasSize(2));

    // The last organization should be the Sandbox Organization
    Organization sampleOrganization = organizations.get(1);
    assertThat(sampleOrganization.getName(), is(SampleDataCreator.SAMPLE_ORGANIZATION_NAME));

    List<Application> applications = applicationDAO.getAll();
    assertThat(applications, hasSize(1));
    Application sampleApplication = applications.get(0);
    assertThat(sampleApplication.getName(), is(SampleDataCreator.SAMPLE_APPLICATION_NAME));
    assertThat(sampleApplication.getParentOwnerId(), is(sampleOrganization.getId()));
    assertThat(sampleApplication.getPublicId(), is(SampleDataCreator.SAMPLE_APPLICATION_PUBLIC_ID));
  }
}
