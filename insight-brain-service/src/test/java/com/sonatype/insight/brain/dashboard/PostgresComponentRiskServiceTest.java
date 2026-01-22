/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.sql.Connection;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryTableHelperTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(PostgresTestCategory.class)
@PostgresTest
public class PostgresComponentRiskServiceTest
    extends AbstractComponentRiskServiceTest
{
  @Inject
  protected PostgresComponentRiskService componentRiskService;

  @Override
  protected DashboardComponentRiskService getComponentRiskService() {
    return componentRiskService;
  }

  // The tests are in the parent class

  @Test
  @Category(SlowTest.class)
  public void testGet_With65kApps() throws Exception {
    Organization organization = tempEntity.newOrganization();

    // insert 65k+ applications so that we are above the threshold for using a temporary table
    try (Connection connection = databaseContainerRule.getOperationalDataStore().getDataSource().getConnection()) {
      String sql = TemporaryTableHelperTest.getInsertMaximumApplicationsSql(organization.getId());
      connection.createStatement().execute(sql);

      // Duplicate a policy violation from one of the `setup()` test apps into our manually inserted apps
      // Use a unique value for the primary key as well as a unique hash to get a result for each app
      String duplicatePolicyViolation = """
          INSERT INTO insight_brain_ods.policy_violation
          SELECT * FROM (
              SELECT  'pv-' || a.application_id,a.application_id,stage_type_id,policy_id,policy_name,threat_level,
                      threat_category,
                      'h-' || substring(a.application_id, 1, 18) as hash,
                      component_id_format,component_id_coordinates_json,filename,
                      constraint_facts_json,action_type_id,open_time,waive_time,legacy_violation_time,
                      fix_time,policy_waiver_id,policy_waiver_comment,seen_by_primary_evaluation,
                      seen_by_monitoring_evaluation,legacy_violation_applied,reachability_status,auto_policy_waiver_id,
                      constraint_facts_id
              FROM    insight_brain_ods.policy_violation pv
              JOIN    insight_brain_ods.application a ON (1=1)
              WHERE   pv.application_id = '%s'
          ) x
          WHERE application_id NOT IN ('%s','%s')""".formatted(app2.getId(), app1.getId(), app2.getId());
      connection.createStatement().execute(duplicatePolicyViolation);

      // invoke the service method with a massive page size and verify all are returned
      DashboardResultsDTO<ComponentRiskDTO> result =
          getComponentRiskService().getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0,
              Integer.MAX_VALUE);
      assertThat(result.dashboardResults).hasSize(65536); // 65,535 + 1 unique component hash from `setup()`
      assertThat(result.hasNextPage).isEqualTo(false);

      // manually delete all test apps (otherwise deletion via the TemporaryEntity tear-down will take forever)
      connection.createStatement().execute(TemporaryTableHelperTest.getCleanupApplicationsSql());
    }
  }
}
