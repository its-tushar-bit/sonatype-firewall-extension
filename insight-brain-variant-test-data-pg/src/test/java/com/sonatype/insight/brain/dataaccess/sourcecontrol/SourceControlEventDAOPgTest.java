/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link SourceControlEventDAOTest} (CLM-45228).
 */
@PostgresTest
public class SourceControlEventDAOPgTest
    extends AbstractDbDAOTest
{
  private SourceControlEventDAO sourceControlEventDAO;

  private Application app;

  private Application app2;

  private Date testStartTime;

  @Override
  @Before
  public void setup() {
    sourceControlEventDAO = daoFactory.createSourceControlEventDAO();

    app = tempEntity.newApplicationWithParent();
    app2 = tempEntity.newApplicationWithParent();
    testStartTime = toDate(LocalDateTime.now().minusSeconds(1));
  }

  @After
  public void cleanup() {
    sourceControlEventDAO.getAll().stream().forEach(sourceControlEventDAO::delete);
  }

  @Test
  public void testSelectEventsByCriteria_CreatedOnOrAfterFilterPostgres() {
    Organization tempOrganization = tempEntity.newOrganization();
    Application tempApplication = tempEntity.newApplication(tempOrganization.getId());
    long cutOffTimeMs = 100000;
    persistSourceControlEvent(0, tempApplication.getId());
    persistSourceControlEvent(0, tempApplication.getId());
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 100000, tempApplication.getId());
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 100000, tempApplication.getId());
    boolean ascending = true;
    Set<String> applicationIds = Collections.singleton(tempApplication.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs + 100000), ascending, 10, 0);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEvent.getId(), expectedEventTwo.getId());
  }

  private SourceControlEvent getNewSourceControlEvent(final String applicationId) {
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(applicationId, StageTypes.BUILD.getId(), "scanId2", false, false, false,
            testStartTime,
            "commitHash1235");

    return new SourceControlEvent()
        .setApplicationId(applicationId)
        .setCommitHash("abcdefg")
        .setEventType(SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT)
        .setPolicyEvaluationId(policyEvaluation.getId())
        .setBranchName("branch")
        .setPullRequestNumber(2)
        .setScmUsername("user")
        .setInitiator("webhook")
        .setCreateTime(testStartTime);
  }

  private Date toDate(final LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private SourceControlEvent persistSourceControlEvent(long createTime, String applicationId) {
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent(applicationId);
    sourceControlEvent.setEventStatus("complete")
        .setInstanceId("instance1")
        .setCreateTime(new Date(createTime));
    sourceControlEventDAO.insert(sourceControlEvent);
    return sourceControlEvent;
  }
}
