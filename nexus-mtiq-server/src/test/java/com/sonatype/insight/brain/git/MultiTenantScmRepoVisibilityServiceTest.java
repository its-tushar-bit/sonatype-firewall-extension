/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MultiTenantScmRepoVisibilityServiceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private static final String TEST_REPO_URL = "https://github.com/foo/bar";

  private ScmRepoVisibilityService scmRepoVisibilityService;

  private SourceControlUtils sourceControlUtils;

  private PasswordHandler passwordHandler;

  private GitApiClient mockGitApiClient;

  @Before
  public void before() {
    scmRepoVisibilityService = lookup(ScmRepoVisibilityService.class);
    sourceControlUtils = lookup(SourceControlUtils.class);
    passwordHandler = lookup(PasswordHandler.class);
  }

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);

    GitClientFactory mockGitClientFactory = mock(GitClientFactory.class);
    mockGitApiClient = mock(GitApiClient.class);

    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);

    when(mockGitClientFactory.createApiClient(Mockito.any())).thenReturn(mockGitApiClient);
  }

  @Test
  public void testIsRepositoryPrivate_PerTenant() throws IOException {
    AtomicReference<String> applicationIdTenant1 = new AtomicReference<>();
    AtomicReference<String> applicationIdTenant2 = new AtomicReference<>();

    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      provisionTenant(tenant.tenantSlug);
      Application application = createScm();
      applicationIdTenant1.set(application.getId());
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      provisionTenant(tenant.tenantSlug);
      Application application = createScm();
      applicationIdTenant2.set(application.getId());
    });

    // Make the `isRepositoryPrivate` call (mocked remote call fronted by cache & TenantReference) for each tenant.
    // Note that the repo URL is the SAME for both tenants to verify that the cache is per tenant (as the cache itself
    // is keyed by the repo URL).
    runTestForTenant(tenant1, applicationIdTenant1.get(), true);
    runTestForTenant(tenant2, applicationIdTenant2.get(), false);

    // also verify the remote call was made twice (i.e. cache was not used)
    verify(mockGitApiClient, times(2)).isRepositoryPrivate();
  }

  private void runTestForTenant(Tenant tenant, String applicationId, boolean isPrivate) {
    TenantTestHelper.testAsTenant(tenant, t1 -> {
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

      when(mockGitApiClient.isRepositoryPrivate()).thenReturn(isPrivate);

      boolean result = scmRepoVisibilityService.isPrivateRepository(gitRepositoryInfo);
      assertThat(result).isEqualTo(isPrivate);
    });
  }

  private Application createScm() {
    String token = new String(passwordHandler.encryptPassword("TOKEN".toCharArray()));
    Application application = tenantTemporaryEntity.newApplicationWithParent();
    tenantTemporaryEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, token, SourceControlProvider.GITHUB);
    tenantTemporaryEntity.newSourceControl(application.getId(), TEST_REPO_URL);
    return application;
  }
}
