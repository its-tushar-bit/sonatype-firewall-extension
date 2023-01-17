/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDetailResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ComponentDetailResource.RESOURCE_PATH);
  }

  @Test
  public void testGetApplicationDetailsByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testGetApplicationDetailsByHash");
    String hash = "ababababab";
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    HttpResponse response = restRequest().path("applications").query("hash", hash).get();
    assertResponseStatus(200, response);
    ApplicationComponentDetailsDTO[] applicationComponentDetailsDTOs = response
        .getBody(ApplicationComponentDetailsDTO[].class);
    assertThat(applicationComponentDetailsDTOs).hasSize(1);
  }

  @Test
  public void testGetComponentNameByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testGetComponentNameByHash");
    String hash = "ababababab";
    HttpRequest request = restRequest().path("name").query("hash", hash);

    HttpResponse response = request.get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Unknown component with hash ababababab.");

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    response = request.get();
    assertResponseStatus(200, response);
    ComponentDisplayName name = response.getBody(ComponentDisplayName.class);
    DisplayFieldValueAssertionUtil.assertDisplayFieldValuesForGAV(name.parts, "groupId", "artifactId", "version");
  }

  @Test
  public void testGetComponentNameByComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");
    HttpRequest request = restRequest().path("nameByIdentifier").query("componentIdentifier", componentIdentifier);

    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    ComponentDisplayName name = response.getBody(ComponentDisplayName.class);
    DisplayFieldValueAssertionUtil.assertDisplayFieldValuesForGAV(name.parts, "groupId", "artifactId", "version");
  }
}
