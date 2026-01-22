/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ScmReducedSecurityServiceTest
    extends AbstractComponentTest
{
  private static final String TEST_URL = "https://github.com/foo/bar";

  @Mock
  private ScmRepoVisibilityService mockScmRepoVisibilityService;

  @Inject
  private ScmReducedSecurityService scmReducedSecurityService;

  @Inject
  private SourceControlUtils sourceControlUtils;

  @Inject
  private PasswordHandler passwordHandler;

  @Override
  public void configure(Binder binder) {
    binder.bind(ScmRepoVisibilityService.class).toInstance(mockScmRepoVisibilityService);

    super.configure(binder);
  }

  @Test
  public void testIsReducedSecurityData_PrivateRepo() {
    String applicationId = createApplicationWithSourceControl();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(true);

    assertThat(scmReducedSecurityService.isReducedSecurityData(applicationId)).isFalse();
  }

  @Test
  public void testIsReducedSecurityData_PublicRepo() {
    String applicationId = createApplicationWithSourceControl();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    when(mockScmRepoVisibilityService.isPrivateRepository(eq(gitRepositoryInfo))).thenReturn(false);

    assertThat(scmReducedSecurityService.isReducedSecurityData(applicationId)).isTrue();
  }

  private String createApplicationWithSourceControl() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(app.getId(), TEST_URL, null,
        new String(passwordHandler.encryptPassword("TOKEN".toCharArray())), null, null, true, null, null);

    return app.getId();
  }
}
