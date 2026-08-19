/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class SampleDataCreatorTest
    extends AbstractComponentH2Test
{
  @Inject
  private SampleDataCreator sampleDataCreator;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Test
  public void testCreateSampleData() {
    sampleDataCreator.createSampleData();

    List<Organization> organizations = organizationDAO.getAll();
    assertThat(organizations).hasSize(2);

    // The last organization should be the Sandbox Organization
    Organization sampleOrganization = organizations.get(1);
    assertThat(sampleOrganization.getName()).isEqualTo(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);

    List<Application> applications = applicationDAO.getAll();
    assertThat(applications).hasSize(1);
    Application sampleApplication = applications.get(0);
    assertThat(sampleApplication.getName()).isEqualTo(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    assertThat(sampleApplication.getParentOwnerId()).isEqualTo(sampleOrganization.getId());
    assertThat(sampleApplication.getPublicId()).isEqualTo(SampleDataCreator.SAMPLE_APPLICATION_PUBLIC_ID);
  }
}
