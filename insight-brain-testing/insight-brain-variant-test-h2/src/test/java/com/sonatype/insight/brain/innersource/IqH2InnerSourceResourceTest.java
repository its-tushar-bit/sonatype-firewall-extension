/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original {@code com.sonatype.insight.brain.innersource} package because it references
 * the package-private {@link InnerSourceResource#RESOURCE_PATH} and
 * {@link InnerSourceResource#COMPONENT_LATEST_VERSION_PATH}.
 */
@IqH2Test
class IqH2InnerSourceResourceTest
{
  private IqTestContext ctx;

  @Test
  void testGetComponentLatestVersion() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = ctx.tempEntity().newApplicationWithParent();
    String version = "1.0.0";
    InnerSourceApplication innerSourceApplication =
        ctx.tempEntity()
            .newInnerSourceApplication(
                InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(), app);
    ctx.tempEntity().newInnerSourceVersion(innerSourceApplication, version, StageTypes.RELEASE.getId());

    HttpResponse response = ctx.restRequest()
        .path(InnerSourceResource.RESOURCE_PATH)
        .path(InnerSourceResource.COMPONENT_LATEST_VERSION_PATH)
        .query("componentIdentifier", componentIdentifier)
        .get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo(version);
  }
}
