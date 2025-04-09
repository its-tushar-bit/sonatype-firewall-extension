/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlPullRequestResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetPullRequestStatus_Pending() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH, SourceControlPullRequestResource.STATUS_PATH)
        .parameter(event.getId())
        .get();

    assertResponseStatus(200, response);
    JsonNode dto = new ObjectMapper().readTree(response.getBodyText());
    assertThat(dto).isNotNull();
    assertThat(dto.path("status").asText()).isEqualTo("PULL_REQUEST_CREATION_PENDING");
  }

  @Test
  public void testGetPullRequestStatus_Success() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("https://github.com/sonatype/insight-brain/pull/13397");
    lookup(SourceControlEventDAO.class).update(event);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH, SourceControlPullRequestResource.STATUS_PATH)
        .parameter(event.getId())
        .get();

    assertResponseStatus(200, response);
    JsonNode dto = new ObjectMapper().readTree(response.getBodyText());
    assertThat(dto).isNotNull();
    assertThat(dto.path("status").asText()).isEqualTo("PULL_REQUEST");
    assertThat(dto.path("url").asText()).isEqualTo(event.getEventStatusDetails());
  }

  @Test
  public void testGetPullRequestStatus_Failure() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("Some error");
    lookup(SourceControlEventDAO.class).update(event);

    HttpResponse response = restRequest()
        .path(SourceControlPullRequestResource.RESOURCE_PATH, SourceControlPullRequestResource.STATUS_PATH)
        .parameter(event.getId())
        .get();

    assertResponseStatus(200, response);
    JsonNode dto = new ObjectMapper().readTree(response.getBodyText());
    assertThat(dto).isNotNull();
    assertThat(dto.path("status").asText()).isEqualTo("PULL_REQUEST_CREATION_FAILED");
    assertThat(dto.path("reason").asText()).isEqualTo(event.getEventStatusDetails());
  }

  private SourceControlEvent createRemediationEvent(final Application app) {
    SourceControlEvent event = new SourceControlEvent().forRemediationPullRequest().setApplicationId(app.getId());
    lookup(SourceControlEventDAO.class).insert(event);
    return event;
  }
}
