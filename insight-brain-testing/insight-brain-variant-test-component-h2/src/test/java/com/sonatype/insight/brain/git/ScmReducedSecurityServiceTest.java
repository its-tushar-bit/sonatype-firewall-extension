/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ScmReducedSecurityServiceTest
    extends AbstractComponentH2Test
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
