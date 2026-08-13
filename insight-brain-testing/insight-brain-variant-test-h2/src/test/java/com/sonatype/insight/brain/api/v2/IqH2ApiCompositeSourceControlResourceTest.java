/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@IqH2Test
class IqH2ApiCompositeSourceControlResourceTest
{
  static final String VALID_URL = "https://example.com/organization/project";

  private IqTestContext ctx;

  private Application app;

  private Organization org;

  private Organization rootOrganization;

  private SourceControlDAO sourceControlDAO;

  private OrganizationDAO organizationDAO;

  private SourceControl rootOrgSourceControl;

  @BeforeEach
  void setup() {
    sourceControlDAO = ctx.lookup(SourceControlDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);

    app = ctx.tempEntity().newApplicationWithParent();
    org = ctx.tempEntity().newOrganization();
    rootOrgSourceControl =
        ctx.tempEntity()
            .newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, "TOKEN", SourceControlProvider.GITHUB,
                null, null, "BASE_BRANCH", null, true, true, "/target/*", null, null, false, false);
    rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.COMPOSITE_SOURCE_CONTROL_PATH_V2).auth();
  }

  @Test
  void testGetCompositeSourceControlByOwner_RootOrg() throws Exception {
    final HttpResponse response = restRequest()
        .path(ApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)
        .get();
    ctx.assertResponseStatus(200, response);
    final ApiCompositeSourceControlDTO result = response.getBody(ApiCompositeSourceControlDTO.class);

    assertThat(result.id).isEqualTo(rootOrgSourceControl.getId());
    assertThat(result.ownerId).isEqualTo(rootOrgSourceControl.getOwnerId());
    assertThat(result.provider.value).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(result.repositoryUrl).isNull();
    assertThat(result.username.value).isNull();
    assertThat(result.username.parentName).isNull();
    assertThat(result.username.parentValue).isNull();
    assertThat(result.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(result.token.parentName).isNull();
    assertThat(result.token.parentValue).isNull();
    assertThat(result.statusChecksEnabled.value).isTrue();
    assertThat(result.statusChecksEnabled.parentName).isNull();
    assertThat(result.statusChecksEnabled.parentValue).isNull();
    assertThat(result.remediationPullRequestsEnabled.value).isTrue();
    assertThat(result.remediationPullRequestsEnabled.parentName).isNull();
    assertThat(result.remediationPullRequestsEnabled.parentValue).isNull();
    assertThat(result.baseBranch.value).isEqualTo("BASE_BRANCH");
    assertThat(result.baseBranch.parentName).isNull();
    assertThat(result.baseBranch.parentValue).isNull();
    assertThat(result.pullRequestCommentingEnabled.value).isTrue();
    assertThat(result.pullRequestCommentingEnabled.parentName).isNull();
    assertThat(result.pullRequestCommentingEnabled.parentValue).isNull();
    assertThat(result.sourceControlEvaluationsEnabled.value).isTrue();
    assertThat(result.sourceControlEvaluationsEnabled.parentName).isNull();
    assertThat(result.sourceControlEvaluationsEnabled.parentValue).isNull();
    assertThat(result.sourceControlScanTarget.value).isEqualTo("/target/*");
    assertThat(result.sourceControlScanTarget.parentName).isNull();
    assertThat(result.sourceControlScanTarget.parentValue).isNull();
    assertThat(result.manualPullRequestsEnabled.value).isFalse();
    assertThat(result.manualPullRequestsEnabled.parentName).isNull();
    assertThat(result.manualPullRequestsEnabled.parentValue).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.value).isFalse();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentName).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentValue).isNull();
  }

  @Test
  void testGetCompositeSourceControlByOwner_RootOrgNotConfigured() throws Exception {
    sourceControlDAO.delete(rootOrgSourceControl);

    final HttpResponse response = restRequest()
        .path(ApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)
        .get();
    ctx.assertResponseStatus(200, response);
    final ApiCompositeSourceControlDTO result = response.getBody(ApiCompositeSourceControlDTO.class);

    assertThat(result.id).isNull();
    assertThat(result.ownerId).isEqualTo(rootOrgSourceControl.getOwnerId());
    assertThat(result.provider.value).isNull();
    assertThat(result.provider.parentValue).isNull();
    assertThat(result.repositoryUrl).isNull();
    assertThat(result.username.value).isNull();
    assertThat(result.username.parentName).isNull();
    assertThat(result.username.parentValue).isNull();
    assertThat(result.token.value).isNull();
    assertThat(result.token.parentName).isNull();
    assertThat(result.token.parentValue).isNull();
    assertThat(result.statusChecksEnabled.value).isNull();
    assertThat(result.statusChecksEnabled.parentName).isNull();
    assertThat(result.statusChecksEnabled.parentValue).isNull();
    assertThat(result.remediationPullRequestsEnabled.value).isNull();
    assertThat(result.remediationPullRequestsEnabled.parentName).isNull();
    assertThat(result.remediationPullRequestsEnabled.parentValue).isNull();
    assertThat(result.baseBranch.value).isNull();
    assertThat(result.baseBranch.parentName).isNull();
    assertThat(result.baseBranch.parentValue).isNull();
    assertThat(result.pullRequestCommentingEnabled.value).isNull();
    assertThat(result.pullRequestCommentingEnabled.parentName).isNull();
    assertThat(result.pullRequestCommentingEnabled.parentValue).isNull();
    assertThat(result.sourceControlEvaluationsEnabled.value).isNull();
    assertThat(result.sourceControlEvaluationsEnabled.parentName).isNull();
    assertThat(result.sourceControlEvaluationsEnabled.parentValue).isNull();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isNull();
    assertThat(result.sourceControlScanTarget.parentValue).isNull();
    assertThat(result.manualPullRequestsEnabled.value).isNull();
    assertThat(result.manualPullRequestsEnabled.parentName).isNull();
    assertThat(result.manualPullRequestsEnabled.parentValue).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.value).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentName).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentValue).isNull();
  }

  @Test
  void testGetCompositeSourceControlByOwner_Organization() throws Exception {
    final SourceControl orgSourceControl =
        ctx.tempEntity()
            .newSourceControl(org.getId(), null, null, "TOKEN", null, false,
                null, null, null, false, false, null);
    final HttpResponse response = restRequest()
        .path(ApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .get();
    ctx.assertResponseStatus(200, response);
    final ApiCompositeSourceControlDTO result = response.getBody(ApiCompositeSourceControlDTO.class);

    assertThat(result.id).isEqualTo(orgSourceControl.getId());
    assertThat(result.ownerId).isEqualTo(orgSourceControl.getOwnerId());
    assertThat(result.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(result.repositoryUrl).isNull();
    assertThat(result.username.value).isNull();
    assertThat(result.username.parentName).isNull();
    assertThat(result.username.parentValue).isNull();
    assertThat(result.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(result.token.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(result.statusChecksEnabled.value).isNull();
    assertThat(result.statusChecksEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.statusChecksEnabled.parentValue).isTrue();
    assertThat(result.remediationPullRequestsEnabled.value).isFalse();
    assertThat(result.remediationPullRequestsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.remediationPullRequestsEnabled.parentValue).isTrue();
    assertThat(result.baseBranch.value).isNull();
    assertThat(result.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
    assertThat(result.pullRequestCommentingEnabled.value).isFalse();
    assertThat(result.pullRequestCommentingEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.pullRequestCommentingEnabled.parentValue).isTrue();
    assertThat(result.sourceControlEvaluationsEnabled.value).isFalse();
    assertThat(result.sourceControlEvaluationsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlEvaluationsEnabled.parentValue).isTrue();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScanTarget.parentValue).isEqualTo("/target/*");
    assertThat(result.manualPullRequestsEnabled.value).isNull();
    assertThat(result.manualPullRequestsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.manualPullRequestsEnabled.parentValue).isFalse();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.value).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentValue).isFalse();
  }

  @Test
  void testGetCompositeSourceControlByOwner_Application() throws Exception {
    final Organization parentOrg = organizationDAO.getById(app.getOrganizationId());
    ctx.tempEntity().newSourceControl(parentOrg.getId(), null, "TOKEN", null, false, null, null);
    final SourceControl appSourceControl =
        ctx.tempEntity()
            .newSourceControl(app.getId(), VALID_URL, null, "TOKEN", null, null,
                true, null, null, false, false, null);

    final HttpResponse response = restRequest()
        .path(ApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();
    ctx.assertResponseStatus(200, response);
    final ApiCompositeSourceControlDTO result = response.getBody(ApiCompositeSourceControlDTO.class);

    assertThat(result.id).isEqualTo(appSourceControl.getId());
    assertThat(result.ownerId).isEqualTo(appSourceControl.getOwnerId());
    assertThat(result.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(result.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(result.username.value).isNull();
    assertThat(result.username.parentName).isNull();
    assertThat(result.username.parentValue).isNull();
    assertThat(result.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(result.token.parentName).isEqualTo(parentOrg.getName());
    assertThat(result.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(result.statusChecksEnabled.value).isTrue();
    assertThat(result.statusChecksEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.statusChecksEnabled.parentValue).isTrue();
    assertThat(result.remediationPullRequestsEnabled.value).isNull();
    assertThat(result.remediationPullRequestsEnabled.parentName).isEqualTo(parentOrg.getName());
    assertThat(result.remediationPullRequestsEnabled.parentValue).isFalse();
    assertThat(result.baseBranch.value).isNull();
    assertThat(result.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
    assertThat(result.pullRequestCommentingEnabled.value).isFalse();
    assertThat(result.pullRequestCommentingEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.pullRequestCommentingEnabled.parentValue).isTrue();
    assertThat(result.sourceControlEvaluationsEnabled.value).isFalse();
    assertThat(result.sourceControlEvaluationsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlEvaluationsEnabled.parentValue).isTrue();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScanTarget.parentValue).isEqualTo("/target/*");
    assertThat(result.manualPullRequestsEnabled.value).isNull();
    assertThat(result.manualPullRequestsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.manualPullRequestsEnabled.parentValue).isFalse();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.value).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentValue).isFalse();
  }

  @Test
  void testGetCompositeSourceControlByOwner_ApplicationNotConfigured() throws Exception {
    final HttpResponse response = restRequest()
        .path(ApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();
    ctx.assertResponseStatus(200, response);
    final ApiCompositeSourceControlDTO result = response.getBody(ApiCompositeSourceControlDTO.class);

    assertThat(result.id).isNull();
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(result.repositoryUrl).isNull();
    assertThat(result.username.value).isNull();
    assertThat(result.username.parentName).isNull();
    assertThat(result.username.parentValue).isNull();
    assertThat(result.token.value).isNull();
    assertThat(result.token.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(result.statusChecksEnabled.value).isNull();
    assertThat(result.statusChecksEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.statusChecksEnabled.parentValue).isTrue();
    assertThat(result.remediationPullRequestsEnabled.value).isNull();
    assertThat(result.remediationPullRequestsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.remediationPullRequestsEnabled.parentValue).isTrue();
    assertThat(result.baseBranch.value).isNull();
    assertThat(result.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
    assertThat(result.pullRequestCommentingEnabled.value).isNull();
    assertThat(result.pullRequestCommentingEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.pullRequestCommentingEnabled.parentValue).isTrue();
    assertThat(result.sourceControlEvaluationsEnabled.value).isNull();
    assertThat(result.sourceControlEvaluationsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlEvaluationsEnabled.parentValue).isTrue();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScanTarget.parentValue).isEqualTo("/target/*");
    assertThat(result.manualPullRequestsEnabled.value).isNull();
    assertThat(result.manualPullRequestsEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.manualPullRequestsEnabled.parentValue).isFalse();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.value).isNull();
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.innerSourceAutomatedUpdatesEnabled.parentValue).isFalse();
  }
}
