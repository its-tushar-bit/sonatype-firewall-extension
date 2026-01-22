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
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestCommentingConfigMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private PullRequestCommentingConfigMigrator pullRequestCommentingConfigMigrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Test
  public void testMigrate_noRootOrgSourceControl() {
    // given: no migration has occurred yet and no root org source control
    MigrationTracker existingTracker = migrationTrackerDAO.getById(PullRequestCommentingConfigMigrator.MIGRATION_ID);
    assertThat(existingTracker).isNull();

    // when:
    pullRequestCommentingConfigMigrator.migrate();

    // then: migration still occurred
    existingTracker = migrationTrackerDAO.getById(PullRequestCommentingConfigMigrator.MIGRATION_ID);
    assertThat(existingTracker).isNotNull();
  }

  @Test
  public void testMigrate_migrateSuccessful() {
    // given: a root org and no existing migration for pr commenting
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, "token", GITLAB);
    MigrationTracker existingTracker = migrationTrackerDAO.getById(PullRequestCommentingConfigMigrator.MIGRATION_ID);
    assertThat(existingTracker).isNull();

    SourceControl rootSourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(rootSourceControl.getPullRequestCommentingEnabled()).isNull();

    // by default, insightConfig will return true for any 'feature' that is not otherwise defined;
    // disable PR commenting explicitly
    SystemConfigurationPropertyFeature.PR_COMMENTING.setEnabled(false);

    // when:
    pullRequestCommentingConfigMigrator.migrate();

    // then: tracker created and root org setup with PR commenting disabled
    MigrationTracker tracker = migrationTrackerDAO.getById(PullRequestCommentingConfigMigrator.MIGRATION_ID);
    assertThat(tracker).isNotNull();

    SourceControl sourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(sourceControl).isNotNull();
    assertThat(sourceControl.getPullRequestCommentingEnabled()).isFalse();

    // when: migrate is called again AFTER "config was flipped"
    SystemConfigurationPropertyFeature.PR_COMMENTING.setEnabled(true);

    pullRequestCommentingConfigMigrator.migrate();

    // then: flipped value was not re-migrated
    sourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(sourceControl.getPullRequestCommentingEnabled()).isFalse();
  }
}
