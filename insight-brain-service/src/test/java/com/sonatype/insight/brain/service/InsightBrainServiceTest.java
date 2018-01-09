/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class InsightBrainServiceTest
    extends AbstractBrainServiceTest
{
  @Test
  @ManualServerInit
  public void testCreateSampleData_Enabled() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
        config.setCreateSampleData(true);
      }
    });

    Organization sampleOrg = new OrganizationDAO().getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    tempEntity.register(sampleOrg);
    Application sampleApp = new ApplicationDAO().getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    tempEntity.register(sampleApp);

    assertThat(sampleOrg, is(notNullValue()));
    assertThat(sampleApp, is(notNullValue()));
  }

  @Test
  public void testCreateSampleData_Disabled() {
    // The creation of the sample data is disabled by default.
    Organization sampleOrg = new OrganizationDAO().getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    assertThat(sampleOrg, is(nullValue()));
    Application sampleApp = new ApplicationDAO().getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    assertThat(sampleApp, is(nullValue()));
  }
}
