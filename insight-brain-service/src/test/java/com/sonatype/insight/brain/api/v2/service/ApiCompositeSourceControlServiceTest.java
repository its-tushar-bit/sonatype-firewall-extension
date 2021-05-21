/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiCompositeSourceControlServiceTest
    extends AbstractComponentTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private static final String TOKEN = "token";

  private static final String ROOT_TOKEN = "root-token";

  @Inject
  private ApiCompositeSourceControlService apiCompositeSourceControlService;

  @Mock
  private TelemetrySender telemetrySenderMock;

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private Application app;

  private Organization org;

  private Organization rootOrganization;

  private SourceControl rootOrgSourcecontrol;

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  @Inject
  private PlexusCipher plexusCipher;

  private static final String ENC = "CMMDwoV";

  @Override
  public void configure(final Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Before
  public void setup() throws Exception {
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
    rootOrgSourcecontrol = tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt(ROOT_TOKEN, ENC),
            SourceControlProvider.GITHUB);
    rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_RootOrg() {
    rootOrgSourcecontrol.setToken(TOKEN);
    sourceControlDAO.update(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    assertThat(dto.id).isEqualTo(rootOrgSourcecontrol.getId());
    assertThat(dto.ownerId).isEqualTo(rootOrgSourcecontrol.getOwnerId());
    assertThat(dto.provider.value).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isNull();
    assertThat(dto.token.parentValue).isNull();
    assertThat(dto.enableStatusChecks.value).isTrue();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isTrue();
    assertThat(dto.enablePullRequests.parentName).isNull();
    assertThat(dto.enablePullRequests.parentValue).isNull();
    assertThat(dto.baseBranch.value).isEqualTo("master");
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_RootOrgNotConfigured() {
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    assertThat(dto.id).isNull();
    assertThat(dto.ownerId).isEqualTo(rootOrgSourcecontrol.getOwnerId());
    assertThat(dto.provider.parentValue).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isNull();
    assertThat(dto.token.parentValue).isNull();
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isNull();
    assertThat(dto.enablePullRequests.parentValue).isNull();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Organization() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(org.getId(), null, TOKEN, null, false, null, null);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());

    assertThat(dto.id).isEqualTo(orgSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(orgSourceControl.getOwnerId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isFalse();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enablePullRequests.parentValue).isTrue();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationNoRootOrg() {
    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(org.getId(), null, TOKEN, SourceControlProvider.GITHUB, false, null, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());

    assertThat(dto.id).isEqualTo(orgSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(orgSourceControl.getOwnerId());
    assertThat(dto.provider.parentValue).isNull();
    assertThat(dto.provider.parentName).isNull();
    assertThat(dto.provider.value).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isNull();
    assertThat(dto.token.parentValue).isNull();
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isFalse();
    assertThat(dto.enablePullRequests.parentName).isNull();
    assertThat(dto.enablePullRequests.parentValue).isNull();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationNoRootOrgOrOrganization() {
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());

    assertThat(dto.id).isEqualTo(null);
    assertThat(dto.ownerId).isEqualTo(org.getId());
    assertThat(dto.provider.parentValue).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isNull();
    assertThat(dto.token.parentValue).isNull();
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isNull();
    assertThat(dto.enablePullRequests.parentValue).isNull();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationNotConfigured() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());

    assertThat(dto.id).isNull();
    assertThat(dto.ownerId).isEqualTo(org.getId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enablePullRequests.parentValue).isTrue();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationOverridesProviderNoToken() {
    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(org.getId(), null, null, SourceControlProvider.GITLAB, false, null, null);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());

    assertThat(dto.id).isEqualTo(orgSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(orgSourceControl.getOwnerId());
    assertThat(dto.provider.value).isEqualTo(SourceControlProvider.GITLAB.toString());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isFalse();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enablePullRequests.parentValue).isTrue();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("master");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Application() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final Organization parentOrg = organizationDAO.getById(app.getOrganizationId());
    tempEntity.newSourceControl(parentOrg.getId(), null, TOKEN, null, false, null, null);
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null, null, true, null);

    ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isEqualTo(appSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(appSourceControl.getOwnerId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isTrue();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.enablePullRequests.parentValue).isFalse();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("BASE_BRANCH");

    appSourceControl.setBaseBranch("BASE_BRANCH_APP");
    appSourceControl.setEnablePullRequests(true);
    sourceControlDAO.update(appSourceControl);

    dto = apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isEqualTo(appSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(appSourceControl.getOwnerId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isTrue();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isTrue();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.enablePullRequests.parentValue).isFalse();
    assertThat(dto.baseBranch.value).isEqualTo("BASE_BRANCH_APP");
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNoOrgSourceControl() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null, null, true, null);

    final ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isEqualTo(appSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(appSourceControl.getOwnerId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isTrue();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enablePullRequests.parentValue).isTrue();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNoRootOrgSourceControl() {
    final Organization parentOrg = organizationDAO.getById(app.getOrganizationId());
    tempEntity.newSourceControl(parentOrg.getId(), null, TOKEN, SourceControlProvider.GITLAB, false, null, null);
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null, null, true, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isEqualTo(appSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(appSourceControl.getOwnerId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITLAB.toString());
    assertThat(dto.provider.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isTrue();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.enablePullRequests.parentValue).isFalse();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNotConfigured() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final Organization parentOrg = organizationDAO.getById(app.getOrganizationId());
    tempEntity.newSourceControl(parentOrg.getId(), null, TOKEN, null, false, null, null);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isNull();
    assertThat(dto.ownerId).isEqualTo(app.getId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.enablePullRequests.parentValue).isFalse();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationAppSourceControlOnly() {
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITLAB, null, true, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isEqualTo(appSourceControl.getId());
    assertThat(dto.ownerId).isEqualTo(appSourceControl.getOwnerId());
    assertThat(dto.provider.parentValue).isNull();
    assertThat(dto.provider.parentName).isNull();
    assertThat(dto.provider.value).isEqualTo(SourceControlProvider.GITLAB.toString());
    assertThat(dto.repositoryUrl).isEqualTo(VALID_URL);
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentName).isNull();
    assertThat(dto.token.parentValue).isNull();
    assertThat(dto.enableStatusChecks.value).isTrue();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isNull();
    assertThat(dto.enablePullRequests.parentValue).isNull();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationRootOrgSourceControlOnly() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isNull();
    assertThat(dto.ownerId).isEqualTo(app.getId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITHUB.toString());
    assertThat(dto.provider.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enableStatusChecks.parentValue).isTrue();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.enablePullRequests.parentValue).isTrue();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isEqualTo(rootOrganization.getName());
    assertThat(dto.baseBranch.parentValue).isEqualTo("BASE_BRANCH");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationOrgSourceControlOnly() {
    final Organization parentOrg = organizationDAO.getById(app.getOrganizationId());
    tempEntity.newSourceControl(parentOrg.getId(), null, TOKEN, SourceControlProvider.GITLAB, false, null, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isNull();
    assertThat(dto.ownerId).isEqualTo(app.getId());
    assertThat(dto.provider.parentValue).isEqualTo(SourceControlProvider.GITLAB.toString());
    assertThat(dto.provider.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isEqualTo(parentOrg.getName());
    assertThat(dto.enablePullRequests.parentValue).isFalse();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNoLevelConfigured() {
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(dto.id).isNull();
    assertThat(dto.ownerId).isEqualTo(app.getId());
    assertThat(dto.provider.parentValue).isNull();
    assertThat(dto.provider.parentName).isNull();
    assertThat(dto.provider.value).isNull();
    assertThat(dto.repositoryUrl).isNull();
    assertThat(dto.username.value).isNull();
    assertThat(dto.username.parentName).isNull();
    assertThat(dto.username.parentValue).isNull();
    assertThat(dto.token.value).isNull();
    assertThat(dto.token.parentName).isNull();
    assertThat(dto.token.parentValue).isNull();
    assertThat(dto.enableStatusChecks.value).isNull();
    assertThat(dto.enableStatusChecks.parentName).isNull();
    assertThat(dto.enableStatusChecks.parentValue).isNull();
    assertThat(dto.enablePullRequests.value).isNull();
    assertThat(dto.enablePullRequests.parentName).isNull();
    assertThat(dto.enablePullRequests.parentValue).isNull();
    assertThat(dto.baseBranch.value).isNull();
    assertThat(dto.baseBranch.parentName).isNull();
    assertThat(dto.baseBranch.parentValue).isNull();
  }

  @Test
  public void getCompositeSourceControlByOwnerDecrypted() throws Exception {
    // given a token at the root org and overridden at the org level
    tempEntity.newSourceControl(org.getId(), null, plexusCipher.encrypt(TOKEN, ENC), null);

    // when we get source control decrypted
    ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlService.getCompositeSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, org.getId());

    // then the passwords at both levels match
    assertThat(dto.token.value).isEqualTo(TOKEN);
    assertThat(dto.token.parentValue).isEqualTo(ROOT_TOKEN);
  }

  @Test
  public void getCompositeSourceControlByOwner_tokens() throws Exception {
    // given a token at the root org and overridden at the org level
    tempEntity.newSourceControl(org.getId(), null, plexusCipher.encrypt(TOKEN, ENC), null);

    // when we get source control not decrypted
    ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, org.getId());

    // then the passwords are redacted
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
  }
}
