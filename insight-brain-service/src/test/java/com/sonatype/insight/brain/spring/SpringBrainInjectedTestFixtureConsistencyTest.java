/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.testing.SpringBrainInjectedTest;
import jakarta.inject.Inject;
import org.junit.Test;

@H2DiskTest
public class SpringBrainInjectedTestFixtureConsistencyTest
    extends SpringBrainInjectedTest
{
  @Inject
  private DatabaseContainer databaseContainer;

  @Inject
  private OperationalDataStore operationalDataStore;

  @Inject
  private AggregationDataStore aggregationDataStore;

  @Inject
  private DataMartDataStore dataMartDataStore;

  @Inject
  private ThirdPartyScansDataStore thirdPartyScansDataStore;

  @Test
  public void shouldShareDatabaseFixtureBetweenSpringBeansAndJUnitRules() {
    assertThat(databaseContainer).isSameAs(databaseContainerRule.getDatabaseContainer());
    assertThat(operationalDataStore).isSameAs(databaseContainerRule.getOperationalDataStore());
    assertThat(aggregationDataStore).isSameAs(databaseContainerRule.getAggregationDataStore());
    assertThat(dataMartDataStore).isSameAs(databaseContainerRule.getDataMartDataStore());
    assertThat(thirdPartyScansDataStore).isSameAs(databaseContainerRule.getThirdPartyScansDataStore());

    Organization organization = tempEntity.newOrganization();
    OrganizationDAO organizationDAO = daoFactory.createOrganizationDAO();

    assertThat(organizationDAO.getById(organization.getId()))
        .extracting(Organization::getId)
        .isEqualTo(organization.getId());
  }
}
