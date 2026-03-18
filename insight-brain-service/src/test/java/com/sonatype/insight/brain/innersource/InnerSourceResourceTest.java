/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(InnerSourceResource.RESOURCE_PATH);
  }

  @Test
  public void testGetComponentLatestVersion() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = tempEntity.newApplicationWithParent();
    String version = "1.0.0";
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(
            InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerSourceApplication, version, StageTypes.RELEASE.getId());

    HttpResponse response = restRequest().path(InnerSourceResource.COMPONENT_LATEST_VERSION_PATH)
        .query("componentIdentifier", componentIdentifier)
        .get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo(version);
  }
}
