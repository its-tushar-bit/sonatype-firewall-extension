/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiManifestEvaluationServiceTest
    extends AbstractComponentTest
{
  @Inject
  ApiManifestEvaluationService apiManifestEvaluationService;

  @Inject
  private PlexusCipher plexusCipher;

  private static final String ENC = "CMMDwoV";

  @Test
  public void testApiManifestScanService() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity
        .newSourceControl(app.getId(), "http://github.com/my/repo.git", null, plexusCipher.encrypt("TOKEN", ENC), null,
            null, true, null, null);

    apiManifestEvaluationService.performManifestScan(app.getId(), "stage", "a-branch", "useragent");

    List<SourceControlEvent> sourceControlEvents = new SourceControlEventDAO().getAll();
    assertThat(sourceControlEvents.size()).isOne();
    SourceControlEvent sourceControlEvent = sourceControlEvents.get(0);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(app.getId());
    assertThat(sourceControlEvent.getStageTypeId()).isEqualTo("stage");
    assertThat(sourceControlEvent.getBranchName()).isEqualTo("a-branch");
    assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.MANIFEST_EVALUATION_EVENT);
  }

  @Test(expected = IOException.class)
  public void testApiManifestScanService_noGitRepoInfo() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    apiManifestEvaluationService.performManifestScan(app.getId(), "stage", "a-branch", "useragent");
  }
}
