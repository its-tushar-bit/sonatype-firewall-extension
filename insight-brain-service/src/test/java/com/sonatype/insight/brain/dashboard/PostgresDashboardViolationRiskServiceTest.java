/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.sql.Connection;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.TemporaryTableHelperTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresTest
@Category(PostgresTestCategory.class)
public class PostgresDashboardViolationRiskServiceTest
    extends AbstractDashboardViolationRiskServiceTest
{
  @Inject
  private PostgresDashboardViolationRiskService dashboardViolationRiskService;

  @Override
  protected DashboardViolationRiskService getDashboardViolationRiskService() {
    return dashboardViolationRiskService;
  }

  @Test
  public void testGet_With65kApps() throws Exception {
    try (Connection connection = databaseContainerRule.getOperationalDataStore().getDataSource().getConnection()) {
      // insert 65k+ applications so that we are above the threshold for using a temporary table
      connection.createStatement().execute(TemporaryTableHelperTest.getInsertMaximumApplicationsSql(org1.getId()));

      // Duplicate a policy violation from one of the `setup()` test apps into our manually inserted apps
      String duplicatePolicyViolation = """
          INSERT INTO insight_brain_ods.policy_violation
          SELECT * FROM (
              SELECT  'pv-' || a.application_id,a.application_id,stage_type_id,policy_id,policy_name,threat_level,
                      threat_category,hash,component_id_format,component_id_coordinates_json,filename,
                      constraint_facts_json,action_type_id,open_time,waive_time,legacy_violation_time,
                      fix_time,policy_waiver_id,policy_waiver_comment,seen_by_primary_evaluation,
                      seen_by_monitoring_evaluation,legacy_violation_applied,reachability_status,auto_policy_waiver_id,
                      constraint_facts_id,is_remediated_by_version_change
              FROM    insight_brain_ods.policy_violation pv
              JOIN    insight_brain_ods.application a ON (1=1)
              WHERE   pv.owner_id = '%s'
          ) x
          WHERE application_id != '%s'""".formatted(app2.getId(), app2.getId());
      connection.createStatement().execute(duplicatePolicyViolation);

      // invoke the service method with a massive page size and verify all are returned
      DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
          .get(null, null, null, null, null, null, null, "-AGE,-THREAT_LEVEL",
              DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, Integer.MAX_VALUE);
      assertThat(result.dashboardResults).hasSize(65538); // 65,535 + 3 from `setup()`
      assertThat(result.hasNextPage).isEqualTo(false);

      // manually delete all test data (otherwise deletion via the TemporaryEntity tear-down will take forever)
      connection.createStatement()
          .execute("DELETE FROM insight_brain_ods.policy_violation WHERE policy_violation_id LIKE 'pv-%'");
      connection.createStatement().execute(TemporaryTableHelperTest.getCleanupApplicationsSql());
    }
  }
}
