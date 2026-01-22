/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.HashSet;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.ApplicationServiceAuthzTest;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.util.HashUtils;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnterpriseReportingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  final String clientUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
      "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

  private final String embedDomain = "http%3A%2F%2Flocalhost%3A8070";

  @Inject
  private EnterpriseReportingService enterpriseReportingService;

  @Test(expected = UnauthenticatedException.class)
  public void testAcquireEmbedSession_UnAuthenticated() {
    enterpriseReportingService.acquireEmbedSession("rolling-recap", embedDomain, clientUserAgent);
  }

  @Test(expected = BadRequestException.class)
  public void testAcquireEmbedSession_UnAuthorized() {
    login();
    enterpriseReportingService.acquireEmbedSession(null, embedDomain, clientUserAgent);
  }

  /**
   * This test is set here to cover the behavior that only the apps that the user
   * has read access are included in the final request, other authz tests are not
   * necessary this method doesn't have an authz filter and there are already
   * tests for {@link ApplicationServiceAuthzTest#testGetApplicationsWithReadPermission}
   */
  @Test
  public void testCreateSSOEmbedUrlRequest() {
    login();

    String dashboardId = "dashboardId";
    String embedDomain = "http://sonatype.sonatype.sonatype.com";

    final Organization organization = tempEntity.newOrganization("Test Org");
    tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());

    grantReadPermission(application2.getId());
    grantReadPermission(application3.getId());
    grantReadPermission(application4.getId());

    SSOEmbedUrlRequest ssoEmbedUrlRequest = enterpriseReportingService
        .createEmbedRequest(dashboardId, embedDomain);

    Set<String> obfuscatedApplicationIds = new HashSet<>();
    obfuscatedApplicationIds.add(HashUtils.hash(application2.getId(), HashUtils.SHA1));
    obfuscatedApplicationIds.add(HashUtils.hash(application3.getId(), HashUtils.SHA1));
    obfuscatedApplicationIds.add(HashUtils.hash(application4.getId(), HashUtils.SHA1));

    assertThat(ssoEmbedUrlRequest.applicationIds)
        .containsExactlyInAnyOrder(obfuscatedApplicationIds.toArray(new String[] {}));
  }
}
