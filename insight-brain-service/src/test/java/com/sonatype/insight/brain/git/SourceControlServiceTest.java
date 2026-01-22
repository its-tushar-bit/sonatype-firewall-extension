/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlServiceTest
    extends AbstractComponentTest
{
  private Application app;

  private Organization org;

  @Inject
  private SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO;

  @Inject
  private SourceControlDefaultBranchCommitHistoryDAO commitHistoryDAO;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private SourceControlService sourceControlService;

  @Inject
  private InsightWork insightWork;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testOnRepositoryUrlUpdated() {
    // given : app source control with commit history and comment
    SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), "https://github.com/org/repo", null, null);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), "develop", "scan-id");
    tempEntity.newSourceControlDefaultBranchCommitHistory(app.getId(), "commit", new Date(), policyEvaluation.getId());
    tempEntity.newSourceControlPullRequestComment(app.getId(), 1, 1, 1, "hash", policyEvaluation.getId(),
        policyEvaluation.getId());
    Date pollDate = Date.from(ZonedDateTime.now().plusDays(2).toInstant());
    appSourceControl.setPullRequestPollTime(pollDate);
    sourceControlDAO.update(appSourceControl);

    File sourceControlDir = insightWork.getSourceControlDir(app.getId());
    sourceControlDir.mkdirs();
    // Sanity check
    assertThat(sourceControlDir).exists();

    // when : repo url updated event is processed
    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(app.getId())
        .setEventType(SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT);
    sourceControlService.onRepositoryUrlUpdated(sourceControlEvent);

    // then : verify git directory referenced, comments, history and app source control dir are deleted and poll time
    // updated
    assertThat(commitHistoryDAO.getByApplicationIdSortedByDateDesc(app.getId())).isEmpty();
    assertThat(sourceControlPullRequestCommentDAO.getByApplicationId(app.getId())).isEmpty();
    assertThat(sourceControlDir).doesNotExist();
    assertThat(sourceControlDAO.getByOwnerId(app.getId()).getPullRequestPollTime()).isBefore(pollDate);
  }
}
