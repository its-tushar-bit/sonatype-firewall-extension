/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiComponentReleaseQuarantineServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPOSITORY_PUBLIC_ID = "publicId";

  @Inject
  private ApiComponentReleaseQuarantineService apiComponentReleaseQuarantineService;

  @Test
  public void testReleaseQuarantineWithoutReEval_Authorized() {
    String packageUrl = "pkg:maven/tomcat/catalina@5.5.23?type=jar";
    Repository repository = createRepository();
    ProxyRepositoryComponent proxyRepositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier(), true);

    grantWritePermission(repository.getId());
    apiComponentReleaseQuarantineService.releaseQuarantineWithoutReEval(proxyRepositoryComponent.getId(), "comment");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReleaseQuarantineWithoutReEval_Unauthenticated() {
    String packageUrl = "pkg:maven/tomcat/catalina@5.5.23?type=jar";
    Repository repository = createRepository();
    ProxyRepositoryComponent proxyRepositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier(), true);

    apiComponentReleaseQuarantineService.releaseQuarantineWithoutReEval(proxyRepositoryComponent.getId(), "comment");
  }

  @Test(expected = UnauthorizedException.class)
  public void testReleaseQuarantineWithoutReEval_Unauthorized() {
    String packageUrl = "pkg:maven/tomcat/catalina@5.5.23?type=jar";
    Repository repository = createRepository();
    ProxyRepositoryComponent proxyRepositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier(), true);

    login();
    apiComponentReleaseQuarantineService.releaseQuarantineWithoutReEval(proxyRepositoryComponent.getId(), "comment");
  }

  private Repository createRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(MANUAL_REPO_MAN_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
  }
}
