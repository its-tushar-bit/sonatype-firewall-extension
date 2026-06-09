/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.ArrayList;
import java.util.Arrays;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ReferencePolicyBackfillMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private ReferencePolicyBackfillMigrator migrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Mock
  private ReferencePolicyFetcher referencePolicyFetcher;

  private static Policy makeHdsPolicy(String name, int threatLevel) {
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    Policy policy = new Policy();
    policy.setName(name);
    policy.setThreatLevel(threatLevel);
    policy.setConstraints(new ArrayList<>(Arrays.asList(constraint)));
    return policy;
  }

  private static PolicyExportResult buildExportResult(Policy... policies) {
    PolicyExportResult result = new PolicyExportResult();
    result.policies = new ArrayList<>(Arrays.asList(policies));
    return result;
  }

  @Test
  public void testMigrate_alreadyMigrated_isNoOp() {
    // given: tracker already present from a prior run
    migrationTrackerDAO.insert(new MigrationTracker(ReferencePolicyBackfillMigrator.MIGRATION_ID));

    // when:
    migrator.migrate();

    // then: still present and migrator did not throw
    MigrationTracker tracker =
        migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID);
    assertThat(tracker).isNotNull();
  }

  @Test
  public void testMigrate_freshInstall_setsTrackerAndDoesNotInsertPolicies() {
    // given: no tracker, no policies anywhere — policyDAO.getAll() returns empty by
    // default in AbstractComponentTest, exercising the fresh-install short-circuit
    // that defers to NewInstancePopulator.
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNull();

    // when:
    migrator.migrate();

    // then: tracker is set; root org still has no policies (we did not call HDS)
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();
    assertThat(policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).isEmpty();
  }

  @Test
  public void testMigrate_allTargetsPresent_setsTrackerWithoutCallingHds() {
    // given: both target policies already exist on root org by name
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious");
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNull();

    // when:
    migrator.migrate();

    // then: tracker is set; we did not need to call HDS at all (verified indirectly: no
    // additional policies appear, and the test's HDS mock was never primed)
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();
    assertThat(policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(2);
  }

  @Test
  public void testMigrate_oneTargetMissing_insertsItAndSetsTracker() {
    // given: Integrity-Rating exists locally, Security-Malicious does not; HDS has both
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");

    when(referencePolicyFetcher.getReferencePolicies()).thenReturn(buildExportResult(
        makeHdsPolicy("Security-Malicious", 10),
        makeHdsPolicy("Integrity-Rating", 8)));

    // when:
    migrator.migrate();

    // then: only the missing one was inserted; tracker is set
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious"))
        .isNotNull();
    assertThat(policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(2);
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();
  }

  @Test
  public void testMigrate_hdsFetchThrows_doesNotSetTracker() {
    // given: Security-Malicious missing locally, HDS throws on fetch
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");
    when(referencePolicyFetcher.getReferencePolicies())
        .thenThrow(new RuntimeException("simulated HDS outage"));

    // when:
    migrator.migrate();

    // then: tracker is NOT set; nothing inserted
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNull();
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious"))
        .isNull();
  }

  @Test
  public void testMigrate_hdsReturnsNull_doesNotSetTracker() {
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");
    when(referencePolicyFetcher.getReferencePolicies()).thenReturn(null);

    migrator.migrate();

    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNull();
  }

  @Test
  public void testMigrate_hdsMissingOneTarget_insertsTheOtherButDoesNotSetTracker() {
    // given: both targets missing locally; HDS only contains Security-Malicious
    when(referencePolicyFetcher.getReferencePolicies()).thenReturn(buildExportResult(
        makeHdsPolicy("Security-Malicious", 10)));

    // (Need at least one policy anywhere so the fresh-install short-circuit doesn't fire)
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Some-Existing-Policy");

    // when:
    migrator.migrate();

    // then: Security-Malicious was inserted, Integrity-Rating was not, tracker NOT set
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious"))
        .isNotNull();
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating"))
        .isNull();
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNull();
  }

  @Test
  public void testMigrate_rootEmptyButChildOrgHasPolicies_runsBackfill() {
    // given: root org has no policies, but a child org does (e.g., a deployment
    // where someone deleted all root-org policies but kept child orgs)
    Organization childOrg = tempEntity.newOrganization("Child Org for Backfill Test");
    tempEntity.newPolicy(childOrg.getId(), "Some-Child-Policy");

    when(referencePolicyFetcher.getReferencePolicies()).thenReturn(buildExportResult(
        makeHdsPolicy("Security-Malicious", 10),
        makeHdsPolicy("Integrity-Rating", 8)));

    // when:
    migrator.migrate();

    // then: BOTH targets are inserted at root, and the tracker is set
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious"))
        .isNotNull();
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating"))
        .isNotNull();
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();
  }

  @Test
  public void testMigrate_isIdempotentAfterSuccess() {
    // given: a successful first run
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");

    when(referencePolicyFetcher.getReferencePolicies()).thenReturn(buildExportResult(
        makeHdsPolicy("Security-Malicious", 10)));

    migrator.migrate();
    int policyCountAfterFirst = policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).size();
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();

    // when: migrate is called again
    migrator.migrate();

    // then: policy count is unchanged
    assertThat(policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID))
        .hasSize(policyCountAfterFirst);
  }

  @Test
  public void testMigrate_partialHds_convergesOnSecondBoot() {
    // given: both targets missing locally; HDS has only Security-Malicious on first boot
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Some-Existing-Policy");

    when(referencePolicyFetcher.getReferencePolicies())
        .thenReturn(buildExportResult(makeHdsPolicy("Security-Malicious", 10)));

    // when: first boot
    migrator.migrate();

    // then: Security-Malicious inserted, Integrity-Rating still missing, tracker NOT set
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious"))
        .isNotNull();
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating"))
        .isNull();
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNull();

    // given: HDS now returns both targets on second boot
    when(referencePolicyFetcher.getReferencePolicies()).thenReturn(buildExportResult(
        makeHdsPolicy("Security-Malicious", 10),
        makeHdsPolicy("Integrity-Rating", 8)));

    // when: second boot
    migrator.migrate();

    // then: Integrity-Rating now inserted, tracker set (convergence)
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating"))
        .isNotNull();
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();
  }

  @Test
  public void testMigrate_descendantOrgHasSameNamedPolicy_skipsAndSetsTracker() {
    // given: Security-Malicious missing at root; the customer manually created
    // Security-Malicious at a child org as a workaround (per CLM-37954 ticket).
    // Integrity-Rating is also missing but has no conflict.
    Organization childOrg = tempEntity.newOrganization("Customer-Workaround-Org");
    tempEntity.newPolicy(childOrg.getId(), "Security-Malicious");

    when(referencePolicyFetcher.getReferencePolicies()).thenReturn(buildExportResult(
        makeHdsPolicy("Integrity-Rating", 8)));

    // when:
    migrator.migrate();

    // then: Security-Malicious NOT inserted at root (would have hit the hierarchy
    // validator and rolled back the whole transaction); Integrity-Rating inserted;
    // tracker IS set because no further automated work is possible — the customer
    // must remove the descendant policy and manually clear the tracker to re-run.
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious"))
        .isNull();
    assertThat(policyDAO.getByOwnerIdAndName(childOrg.getId(), "Security-Malicious"))
        .isNotNull();
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating"))
        .isNotNull();
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();
  }

  @Test
  public void testMigrate_allTargetsHaveDescendantConflicts_setsTrackerWithoutHds() {
    // given: both target names exist on a child org; root has neither.
    Organization childOrg = tempEntity.newOrganization("Customer-Workaround-Org-Both");
    tempEntity.newPolicy(childOrg.getId(), "Security-Malicious");
    tempEntity.newPolicy(childOrg.getId(), "Integrity-Rating");

    // when:
    migrator.migrate();

    // then: nothing inserted at root; tracker IS set; HDS was never called.
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious"))
        .isNull();
    assertThat(policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating"))
        .isNull();
    assertThat(migrationTrackerDAO.getById(ReferencePolicyBackfillMigrator.MIGRATION_ID))
        .isNotNull();
  }
}
