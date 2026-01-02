/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Category(SlowTest.class)
public class ApiSourceControlEventServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private ApiSourceControlEventService apiSourceControlEventService;

  @Test
  public void testGetSourceControlEventByOrganization_EventComplete() {
    long now = System.currentTimeMillis();
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID,
        "sourceScan",
        new Date(now - 5000),
        "sourceCommit");
    SourceControlEvent event = tempEntity.newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    sourceControlEventDAO.markEventInProgress(event.getId());
    sourceControlEventDAO.markEventHasError(event.getId(), "error message", new RuntimeException());
    filter.setCreatedOnOrAfter(new Date(now - 1000));
    filter.setAscending(true);
    filter.setLimit(10);
    filter.setOffset(0);
    event = sourceControlEventDAO.getById(event.getId());

    List<ApiSourceControlEventDTO> result = apiSourceControlEventService
        .getApiSourceControlEventData(OwnerType.ORGANIZATION, application.getOrganizationId(), filter);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getApplicationId()).isEqualTo(event.getApplicationId());
    assertThat(result.get(0).getId()).isEqualTo(event.getId());
    assertThat(result.get(0).getPriority()).isEqualTo(event.getEventPriority());
    assertThat(result.get(0).getStartTime()).isEqualTo(event.getStartTime());
    assertThat(result.get(0).getType()).isEqualTo(event.getEventType());
    assertThat(result.get(0).getStatus()).isEqualTo(event.getEventStatus());
    assertThat(result.get(0).getUser()).isEqualTo(event.getScmUsername());
    assertThat(result.get(0).getTimeExecuting())
        .isEqualTo(event.getCompleteTime().getTime() - event.getStartTime().getTime());
    assertThat(result.get(0).getTimeWaiting())
        .isEqualTo(event.getStartTime().getTime() - event.getCreateTime().getTime());
  }

  @Test
  public void testGetSourceControlEventByApplication_EventComplete() {
    long now = System.currentTimeMillis();
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID,
        "sourceScan",
        new Date(now - 5000),
        "sourceCommit");
    SourceControlEvent event = tempEntity.newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    sourceControlEventDAO.markEventInProgress(event.getId());
    sourceControlEventDAO.markEventHasError(event.getId(), "error message", new RuntimeException());
    filter.setCreatedOnOrAfter(new Date(now - 1000));
    filter.setAscending(true);
    filter.setLimit(10);
    filter.setOffset(0);
    event = sourceControlEventDAO.getById(event.getId());

    List<ApiSourceControlEventDTO> result = apiSourceControlEventService
        .getApiSourceControlEventData(OwnerType.APPLICATION, application.getId(), filter);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getApplicationId()).isEqualTo(event.getApplicationId());
    assertThat(result.get(0).getId()).isEqualTo(event.getId());
    assertThat(result.get(0).getPriority()).isEqualTo(event.getEventPriority());
    assertThat(result.get(0).getStartTime()).isEqualTo(event.getStartTime());
    assertThat(result.get(0).getType()).isEqualTo(event.getEventType());
    assertThat(result.get(0).getStatus()).isEqualTo(event.getEventStatus());
    assertThat(result.get(0).getUser()).isEqualTo(event.getScmUsername());
    assertThat(result.get(0).getTimeExecuting())
        .isEqualTo(event.getCompleteTime().getTime() - event.getStartTime().getTime());
    assertThat(result.get(0).getTimeWaiting())
        .isEqualTo(event.getStartTime().getTime() - event.getCreateTime().getTime());
  }

  @Test
  public void testGetSourceControlEventByApplication_FilterByDateEmptyResults() {
    long now = System.currentTimeMillis();
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID,
        "sourceScan",
        new Date(now - 5000),
        "sourceCommit");
    SourceControlEvent sourceControlEvent = tempEntity.newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    filter.setCreatedOnOrAfter(new Date(sourceControlEvent.getCreateTime().getTime() + 1000));
    filter.setAscending(true);
    filter.setLimit(10);
    filter.setOffset(0);

    List<ApiSourceControlEventDTO> result = apiSourceControlEventService
        .getApiSourceControlEventData(OwnerType.APPLICATION, application.getId(), filter);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetSourceControlEventByApplication_BadRequestLimitLessThanOne() {
    long now = System.currentTimeMillis();
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID,
        "sourceScan",
        new Date(now - 5000),
        "sourceCommit");
    tempEntity.newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    filter.setCreatedOnOrAfter(new Date(now + 1000));
    filter.setAscending(true);
    filter.setLimit(0);
    filter.setOffset(0);

    assertThrows("Filter limit cannot be less than 1",
        BadRequestException.class,
        () -> apiSourceControlEventService
        .getApiSourceControlEventData(OwnerType.APPLICATION, application.getId(), filter));
  }

  @Test
  public void testGetSourceControlEventByApplication_BadRequestOffsetLessThanZero() {
    long now = System.currentTimeMillis();
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        BuildStageType.ID,
        "sourceScan",
        new Date(now - 5000),
        "sourceCommit");
    tempEntity.newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    filter.setCreatedOnOrAfter(new Date(now + 1000));
    filter.setAscending(true);
    filter.setLimit(10);
    filter.setOffset(-1);

    assertThrows("Flter offset cannot be less than 0",
        BadRequestException.class,
        () -> apiSourceControlEventService
        .getApiSourceControlEventData(OwnerType.APPLICATION, application.getId(), filter));
  }
}
