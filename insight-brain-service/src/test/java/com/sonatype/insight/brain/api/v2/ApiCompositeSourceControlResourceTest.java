/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApiCompositeSourceControlResourceTest
    extends AbstractResourceTest
{
  static final String VALID_URL = "https://example.com/organization/project";

  private Application app;

  private Organization org;

  private Organization rootOrganization;

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private SourceControl rootOrgSourceControl;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
    rootOrgSourceControl =
        tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, "TOKEN", SourceControlProvider.GITHUB, null,
            null, "BASE_BRANCH", null, true, true, "/target/*");
    rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.COMPOSITE_SOURCE_CONTROL_PATH_V2).auth();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_RootOrg() throws Exception {
    final HttpResponse response = restRequest()
        .path(DefaultApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)
        .get();
    assertResponseStatus(200, response);
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
    assertThat(result.sourceControlScansEnabled.value).isTrue();
    assertThat(result.sourceControlScansEnabled.parentName).isNull();
    assertThat(result.sourceControlScansEnabled.parentValue).isNull();
    assertThat(result.sourceControlScanTarget.value).isEqualTo("/target/*");
    assertThat(result.sourceControlScanTarget.parentName).isNull();
    assertThat(result.sourceControlScanTarget.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_RootOrgNotConfigured() throws Exception {
    sourceControlDAO.delete(rootOrgSourceControl);

    final HttpResponse response = restRequest()
        .path(DefaultApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID)
        .get();
    assertResponseStatus(200, response);
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
    assertThat(result.sourceControlScansEnabled.value).isNull();
    assertThat(result.sourceControlScansEnabled.parentName).isNull();
    assertThat(result.sourceControlScansEnabled.parentValue).isNull();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isNull();
    assertThat(result.sourceControlScanTarget.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Organization() throws Exception {
    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(org.getId(), null, null, "TOKEN", null, false,
            null, null, null, false, false, null);
    final HttpResponse response = restRequest()
        .path(DefaultApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .get();
    assertResponseStatus(200, response);
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
    assertThat(result.sourceControlScansEnabled.value).isFalse();
    assertThat(result.sourceControlScansEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScansEnabled.parentValue).isTrue();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScanTarget.parentValue).isEqualTo("/target/*");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Application() throws Exception {
    final Organization parentOrg = organizationDAO.getById(app.getOrganizationId());
    tempEntity.newSourceControl(parentOrg.getId(), null, "TOKEN", null, false, null, null);
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, null, "TOKEN", null, null,
            true, null, null, false, false, null);

    final HttpResponse response = restRequest()
        .path(DefaultApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();
    assertResponseStatus(200, response);
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
    assertThat(result.sourceControlScansEnabled.value).isFalse();
    assertThat(result.sourceControlScansEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScansEnabled.parentValue).isTrue();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScanTarget.parentValue).isEqualTo("/target/*");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNotConfigured() throws Exception {
    final HttpResponse response = restRequest()
        .path(DefaultApiCompositeSourceControlResource.BY_OWNER)
        .parameter(OwnerType.APPLICATION, app.getId())
        .get();
    assertResponseStatus(200, response);
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
    assertThat(result.sourceControlScansEnabled.value).isNull();
    assertThat(result.sourceControlScansEnabled.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScansEnabled.parentValue).isTrue();
    assertThat(result.sourceControlScanTarget.value).isNull();
    assertThat(result.sourceControlScanTarget.parentName).isEqualTo(rootOrganization.getName());
    assertThat(result.sourceControlScanTarget.parentValue).isEqualTo("/target/*");
  }
}
