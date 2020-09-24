/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.jaxrs.testing.HttpResponse;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.ApiManifestEvaluationResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiManifestEvaluationResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testManifestScan() throws Exception {
    // given an application
    Application app = tempEntity.newApplicationWithParent();

    // and a root-org source control definition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    // and app-level source control
    PasswordHandler pwHandler = this.getCLMServer().getInstance(PasswordHandler.class);
    tempEntity
        .newSourceControl(app.getId(), "http://github.com/my/repo.git", null,
            new String(pwHandler.encryptPassword("TOKEN".toCharArray())), null,
            null, true, "customBranch", null);

    // and we can query for Source Control events
    SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();

    // and events are empty
    assertThat(sourceControlEventDAO.getAll()).isEmpty();

    // when application manifest is scanned
    HttpResponse response = restRequest().path(RESOURCE_PATH).parameter(app.getId()).post();

    // the response contains status ID
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);
    @SuppressWarnings("unchecked")
    Map<String, String> responseMap = response.getBody(Map.class);
    assertThat(responseMap.containsKey("statusId")).isTrue();

    // and the event was published
    List<SourceControlEvent> allEvents = sourceControlEventDAO.getAll();
    assertThat(allEvents.size()).isEqualTo(1);

    // and it matches expected values
    SourceControlEvent event = allEvents.get(0);
    assertThat(event.getApplicationId()).isEqualTo(app.getId());
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.MANIFEST_EVALUATION_EVENT);
    assertThat(event.getStageTypeId()).isEqualTo("develop");
    assertThat(event.getBranchName()).isEqualTo("customBranch");

  }
}
