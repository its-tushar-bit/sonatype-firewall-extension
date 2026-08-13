/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class InternalSourceControlPolicyEvaluationsConfigMigratorTest
    extends AbstractComponentH2Test
{
  @Inject
  private InternalSourceControlPolicyEvaluationsConfigMigrator migrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Test
  public void testMigrate_noRootOrgSourceControl() {
    // given: no migration has occurred yet and no root org source control
    MigrationTracker existingTracker =
        migrationTrackerDAO.getById(InternalSourceControlPolicyEvaluationsConfigMigrator.MIGRATION_ID);
    assertThat(existingTracker).isNull();

    // when:
    migrator.migrate();

    // then: migration occurred
    existingTracker = migrationTrackerDAO.getById(InternalSourceControlPolicyEvaluationsConfigMigrator.MIGRATION_ID);
    assertThat(existingTracker).isNotNull();
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testMigrate() {
    // given: a root org and no existing migration
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, "token", GITLAB);
    MigrationTracker existingTracker =
        migrationTrackerDAO.getById(InternalSourceControlPolicyEvaluationsConfigMigrator.MIGRATION_ID);
    assertThat(existingTracker).isNull();

    SourceControl rootSourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(rootSourceControl.getSourceControlEvaluationsEnabled()).isNull();

    // by default, insightConfig will return true for any 'feature' that is not otherwise defined;
    // disable internal source control policy evaluations explicitly
    SystemConfigurationPropertyFeature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.setEnabled(false);

    // when:
    migrator.migrate();

    // then: tracker created and root org setup with flag disabled
    MigrationTracker tracker =
        migrationTrackerDAO.getById(InternalSourceControlPolicyEvaluationsConfigMigrator.MIGRATION_ID);
    assertThat(tracker).isNotNull();

    SourceControl sourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(sourceControl).isNotNull();
    assertThat(sourceControl.getSourceControlEvaluationsEnabled()).isFalse();

    // when: migrate is called again AFTER "config was flipped"
    SystemConfigurationPropertyFeature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.setEnabled(true);

    migrator.migrate();

    // then: flipped value was not re-migrated
    sourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(sourceControl.getSourceControlEvaluationsEnabled()).isFalse();
  }
}
