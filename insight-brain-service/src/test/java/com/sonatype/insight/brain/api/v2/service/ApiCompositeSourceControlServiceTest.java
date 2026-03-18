/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeValueDTO;
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

  @Inject
  protected SourceControlDAO sourceControlDAO;

  @Inject
  protected OrganizationDAO organizationDAO;

  @Inject
  protected PlexusCipher plexusCipher;

  private Application app;

  private Application childApp;

  private Organization level1ChildOrg;

  private Organization level2ChildOrg;

  protected Organization rootOrganization;

  protected SourceControl rootOrgSourcecontrol;

  protected static final String ENC = "CMMDwoV";

  @Override
  public void configure(final Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Before
  public void setup() throws Exception {
    rootOrgSourcecontrol = tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt(ROOT_TOKEN, ENC),
            SourceControlProvider.GITHUB);
    rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
    level1ChildOrg = tempEntity.newOrganization(rootOrganization);
    app = tempEntity.newApplicationWithParent(level1ChildOrg);
    level2ChildOrg = tempEntity.newOrganization(level1ChildOrg);
    childApp = tempEntity.newApplicationWithParent(level2ChildOrg);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_RootOrg() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(true);
    rootOrgSourcecontrol.setSourceControlScanTarget("target/*");
    rootOrgSourcecontrol.setSshEnabled(true);
    rootOrgSourcecontrol.setCommitStatusEnabled(false);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setClosePrOnFailedChecksEnabled(true);
    rootOrgSourcecontrol.setClosePrAfterDaysOpenEnabled(true);
    rootOrgSourcecontrol.setClosePrAfterDays(7);
    sourceControlDAO.update(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = rootOrgSourcecontrol.getId();
    actualDTO.ownerId = rootOrgSourcecontrol.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITHUB.toString();
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.remediationPullRequestsEnabled.value = true;
    actualDTO.baseBranch.value = "master";
    actualDTO.pullRequestCommentingEnabled.value = false;
    actualDTO.sourceControlEvaluationsEnabled.value = true;
    actualDTO.sourceControlScanTarget.value = "target/*";
    actualDTO.sshEnabled.value = true;
    actualDTO.commitStatusEnabled.value = false;
    actualDTO.manualPullRequestsEnabled.value = false;
    actualDTO.closePrOnFailedChecksEnabled.value = true;
    actualDTO.closePrAfterDaysOpenEnabled.value = true;
    actualDTO.closePrAfterDays.value = 7;

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_RootOrgNotConfigured() {
    sourceControlDAO.delete(rootOrgSourcecontrol);
    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID);

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = rootOrgSourcecontrol.getOwnerId();

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Organization() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(false);
    rootOrgSourcecontrol.setSshEnabled(false);
    rootOrgSourcecontrol.setCommitStatusEnabled(true);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(rootOrgSourcecontrol);
    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(level1ChildOrg.getId(), null, null, null, TOKEN, null, false,
            null, null, null, true, true, "/target/*", true, false, true, true);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level1ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = orgSourceControl.getId();
    actualDTO.ownerId = orgSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.value = false;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.value = true;
    actualDTO.pullRequestCommentingEnabled.parentName = rootOrganization.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = false;
    actualDTO.sourceControlEvaluationsEnabled.value = true;
    actualDTO.sourceControlEvaluationsEnabled.parentName = rootOrganization.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = false;
    actualDTO.sourceControlScanTarget.value = "/target/*";
    actualDTO.sshEnabled.value = true;
    actualDTO.sshEnabled.parentName = rootOrganization.getName();
    actualDTO.sshEnabled.parentValue = false;
    actualDTO.commitStatusEnabled.value = false;
    actualDTO.commitStatusEnabled.parentName = rootOrganization.getName();
    actualDTO.commitStatusEnabled.parentValue = true;
    actualDTO.manualPullRequestsEnabled.value = true;
    actualDTO.manualPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.value = true;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = rootOrganization.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationNoRootOrg() {
    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(
            level1ChildOrg.getId(),
            null,
            TOKEN,
            SourceControlProvider.GITHUB,
            false,
            null,
            null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level1ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = orgSourceControl.getId();
    actualDTO.ownerId = orgSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITHUB.toString();
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.remediationPullRequestsEnabled.value = false;

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationNoRootOrgOrOrganization() {
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level1ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = level1ChildOrg.getId();

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationNotConfigured() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(true);
    rootOrgSourcecontrol.setSourceControlScanTarget("target/*");
    rootOrgSourcecontrol.setSshEnabled(true);
    rootOrgSourcecontrol.setCommitStatusEnabled(false);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    rootOrgSourcecontrol.setClosePrOnFailedChecksEnabled(true);
    rootOrgSourcecontrol.setClosePrAfterDaysOpenEnabled(true);
    rootOrgSourcecontrol.setClosePrAfterDays(7);
    sourceControlDAO.update(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level1ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = level1ChildOrg.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.parentName = rootOrganization.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = false;
    actualDTO.sourceControlEvaluationsEnabled.parentName = rootOrganization.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = rootOrganization.getName();
    actualDTO.sourceControlScanTarget.parentValue = "target/*";
    actualDTO.sshEnabled.parentName = rootOrganization.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = rootOrganization.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = rootOrganization.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    actualDTO.closePrOnFailedChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.closePrOnFailedChecksEnabled.parentValue = true;
    actualDTO.closePrAfterDaysOpenEnabled.parentName = rootOrganization.getName();
    actualDTO.closePrAfterDaysOpenEnabled.parentValue = true;
    actualDTO.closePrAfterDays.parentName = rootOrganization.getName();
    actualDTO.closePrAfterDays.parentValue = 7;
    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_OrganizationOverridesProviderNoToken() {
    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(
            level1ChildOrg.getId(),
            null,
            null,
            SourceControlProvider.GITLAB,
            false,
            null,
            null);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level1ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = orgSourceControl.getId();
    actualDTO.ownerId = orgSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.value = false;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "master";

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Application() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    tempEntity.newSourceControl(level1ChildOrg.getId(), null, TOKEN, null, false, null, null);
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null, null, true, null);

    ApiCompositeSourceControlDTO resultDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);

    appSourceControl.setBaseBranch("BASE_BRANCH_APP");
    appSourceControl.setRemediationPullRequestsEnabled(true);
    appSourceControl.setPullRequestCommentingEnabled(false);
    appSourceControl.setSourceControlEvaluationsEnabled(true);
    appSourceControl.setSourceControlScanTarget("target/*");
    appSourceControl.setSshEnabled(true);
    appSourceControl.setCommitStatusEnabled(false);
    appSourceControl.setManualPullRequestsEnabled(false);
    appSourceControl.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(appSourceControl);

    resultDTO = apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.value = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.baseBranch.value = "BASE_BRANCH_APP";
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.value = false;
    actualDTO.sourceControlEvaluationsEnabled.value = true;
    actualDTO.sourceControlScanTarget.value = "target/*";
    actualDTO.sshEnabled.value = true;
    actualDTO.commitStatusEnabled.value = false;
    actualDTO.manualPullRequestsEnabled.value = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.value = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNoOrgSourceControl() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(true);
    rootOrgSourcecontrol.setSourceControlScanTarget("target/*");
    rootOrgSourcecontrol.setSshEnabled(true);
    rootOrgSourcecontrol.setCommitStatusEnabled(false);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(rootOrgSourcecontrol);

    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null, null, true, null);

    final ApiCompositeSourceControlDTO resultDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.parentName = rootOrganization.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = false;
    actualDTO.sourceControlEvaluationsEnabled.parentName = rootOrganization.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = rootOrganization.getName();
    actualDTO.sourceControlScanTarget.parentValue = "target/*";
    actualDTO.sshEnabled.parentName = rootOrganization.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = rootOrganization.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = rootOrganization.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNoRootOrgSourceControl() {
    tempEntity.newSourceControl(level1ChildOrg.getId(), null, null, null, TOKEN, SourceControlProvider.GITLAB, false,
        null, null, null, true, true, "/target/*", true, false, false, false);
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, null, null, true, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentName = level1ChildOrg.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.pullRequestCommentingEnabled.parentName = level1ChildOrg.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = true;
    actualDTO.sourceControlEvaluationsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = level1ChildOrg.getName();
    actualDTO.sourceControlScanTarget.parentValue = "/target/*";
    actualDTO.sshEnabled.parentName = level1ChildOrg.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = level1ChildOrg.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = level1ChildOrg.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNotConfigured() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(true);
    rootOrgSourcecontrol.setSourceControlScanTarget("target/*");
    rootOrgSourcecontrol.setSshEnabled(true);
    rootOrgSourcecontrol.setCommitStatusEnabled(false);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(rootOrgSourcecontrol);

    tempEntity.newSourceControl(level1ChildOrg.getId(), null, TOKEN, null, false, null, null);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = app.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.parentName = rootOrganization.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = false;
    actualDTO.sourceControlEvaluationsEnabled.parentName = rootOrganization.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = rootOrganization.getName();
    actualDTO.sourceControlScanTarget.parentValue = "target/*";
    actualDTO.sshEnabled.parentName = rootOrganization.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = rootOrganization.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = rootOrganization.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationAppSourceControlOnly() {
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, TOKEN, SourceControlProvider.GITLAB, null, true, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITLAB.toString();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationRootOrgSourceControlOnly() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = app.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationOrgSourceControlOnly() {
    tempEntity.newSourceControl(level1ChildOrg.getId(), null, null, null, TOKEN, SourceControlProvider.GITLAB, false,
        null, null, null, true, true, "/target/*", true, false, false, false);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = app.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentName = level1ChildOrg.getName();
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.remediationPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.pullRequestCommentingEnabled.parentName = level1ChildOrg.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = true;
    actualDTO.sourceControlEvaluationsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = level1ChildOrg.getName();
    actualDTO.sourceControlScanTarget.parentValue = "/target/*";
    actualDTO.sshEnabled.parentName = level1ChildOrg.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = level1ChildOrg.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = level1ChildOrg.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelOrganization() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(false);
    rootOrgSourcecontrol.setSshEnabled(false);
    rootOrgSourcecontrol.setCommitStatusEnabled(true);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(rootOrgSourcecontrol);

    tempEntity.newSourceControl(level1ChildOrg.getId(), null, null, null, TOKEN, null, false,
        null, null, null, true, true, "/target/*", true, false, true, true);

    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(level2ChildOrg.getId(), null, null, null, TOKEN,
            SourceControlProvider.GITLAB, false,
            null, "NewBranch", null, true, true, "/target/childOrg/*", true, false, false, false);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level2ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = orgSourceControl.getId();
    actualDTO.ownerId = orgSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.value = false;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.value = "NewBranch";
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.value = true;
    actualDTO.pullRequestCommentingEnabled.parentName = level1ChildOrg.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = true;
    actualDTO.sourceControlEvaluationsEnabled.value = true;
    actualDTO.sourceControlEvaluationsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.value = "/target/childOrg/*";
    actualDTO.sourceControlScanTarget.parentName = level1ChildOrg.getName();
    actualDTO.sourceControlScanTarget.parentValue = "/target/*";
    actualDTO.sshEnabled.value = true;
    actualDTO.sshEnabled.parentName = level1ChildOrg.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.value = false;
    actualDTO.commitStatusEnabled.parentName = level1ChildOrg.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.value = false;
    actualDTO.manualPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = true;
    actualDTO.innerSourceAutomatedUpdatesEnabled.value = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = level1ChildOrg.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = true;

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelOrganization_NoRootOrg() {
    sourceControlDAO.delete(rootOrgSourcecontrol);
    tempEntity.newSourceControl(
        level1ChildOrg.getId(),
        null,
        TOKEN,
        SourceControlProvider.GITHUB,
        false,
        null,
        null);

    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(level2ChildOrg.getId(), null, null, null, TOKEN,
            SourceControlProvider.GITLAB, true,
            null, "NewBranch", null, true, true, "/target/childOrg/*", true, false, false, false);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level2ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = orgSourceControl.getId();
    actualDTO.ownerId = orgSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = level1ChildOrg.getName();
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.remediationPullRequestsEnabled.value = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.value = "NewBranch";
    actualDTO.pullRequestCommentingEnabled.value = true;
    actualDTO.sourceControlEvaluationsEnabled.value = true;
    actualDTO.sourceControlScanTarget.value = "/target/childOrg/*";
    actualDTO.sshEnabled.value = true;
    actualDTO.commitStatusEnabled.value = false;
    actualDTO.manualPullRequestsEnabled.value = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.value = false;

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelOrganization_NoParentOrOrganizations() {
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(level2ChildOrg.getId(), null, null, null, TOKEN,
            SourceControlProvider.GITLAB, true,
            null, "NewBranch", null, true, true, "/target/childOrg/*", true, false, false, false);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level2ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = orgSourceControl.getId();
    actualDTO.ownerId = orgSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITLAB.toString();
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.remediationPullRequestsEnabled.value = true;
    actualDTO.baseBranch.value = "NewBranch";
    actualDTO.pullRequestCommentingEnabled.value = true;
    actualDTO.sourceControlEvaluationsEnabled.value = true;
    actualDTO.sourceControlScanTarget.value = "/target/childOrg/*";
    actualDTO.sshEnabled.value = true;
    actualDTO.commitStatusEnabled.value = false;
    actualDTO.manualPullRequestsEnabled.value = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.value = false;

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevel_OrganizationNotConfigured() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(true);
    rootOrgSourcecontrol.setSourceControlScanTarget("target/*");
    rootOrgSourcecontrol.setSshEnabled(true);
    rootOrgSourcecontrol.setCommitStatusEnabled(false);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(rootOrgSourcecontrol);

    tempEntity.newSourceControl(
        level1ChildOrg.getId(),
        null,
        TOKEN,
        SourceControlProvider.GITHUB,
        false,
        null,
        null);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level2ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = level2ChildOrg.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = level1ChildOrg.getName();
    actualDTO.token.parentName = level1ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level1ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.parentName = rootOrganization.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = false;
    actualDTO.sourceControlEvaluationsEnabled.parentName = rootOrganization.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = rootOrganization.getName();
    actualDTO.sourceControlScanTarget.parentValue = "target/*";
    actualDTO.sshEnabled.parentName = rootOrganization.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = rootOrganization.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = rootOrganization.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelOrganization_OverridesProviderNoToken() {
    final SourceControl orgSourceControl =
        tempEntity.newSourceControl(
            level2ChildOrg.getId(),
            null,
            null,
            SourceControlProvider.GITLAB,
            false,
            null,
            null);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, level2ChildOrg.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = orgSourceControl.getId();
    actualDTO.ownerId = orgSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.value = false;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "master";

    validateCompositeSourceControlDTO(OwnerType.ORGANIZATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_ApplicationNoLevelConfigured() {
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, app.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = app.getId();
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelApplication() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    tempEntity.newSourceControl(level2ChildOrg.getId(), null, TOKEN, null, false, null, null);
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(childApp.getId(), VALID_URL, TOKEN, null, null, true, null);

    ApiCompositeSourceControlDTO resultDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level2ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);

    appSourceControl.setBaseBranch("BASE_BRANCH_APP");
    appSourceControl.setRemediationPullRequestsEnabled(true);
    appSourceControl.setPullRequestCommentingEnabled(false);
    appSourceControl.setSourceControlEvaluationsEnabled(true);
    appSourceControl.setSourceControlScanTarget("target/*");
    appSourceControl.setSshEnabled(true);
    appSourceControl.setCommitStatusEnabled(false);
    appSourceControl.setManualPullRequestsEnabled(false);
    appSourceControl.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(appSourceControl);

    resultDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level2ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.value = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.baseBranch.value = "BASE_BRANCH_APP";
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.value = false;
    actualDTO.sourceControlEvaluationsEnabled.value = true;
    actualDTO.sourceControlScanTarget.value = "target/*";
    actualDTO.sshEnabled.value = true;
    actualDTO.commitStatusEnabled.value = false;
    actualDTO.manualPullRequestsEnabled.value = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.value = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelApplication_NoOrgSourceControl() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(true);
    rootOrgSourcecontrol.setSourceControlScanTarget("target/*");
    rootOrgSourcecontrol.setSshEnabled(true);
    rootOrgSourcecontrol.setCommitStatusEnabled(false);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(rootOrgSourcecontrol);

    final SourceControl appSourceControl =
        tempEntity.newSourceControl(childApp.getId(), VALID_URL, TOKEN, null, null, true, null);

    final ApiCompositeSourceControlDTO resultDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.parentName = rootOrganization.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = false;
    actualDTO.sourceControlEvaluationsEnabled.parentName = rootOrganization.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = rootOrganization.getName();
    actualDTO.sourceControlScanTarget.parentValue = "target/*";
    actualDTO.sshEnabled.parentName = rootOrganization.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = rootOrganization.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = rootOrganization.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelApplication_NoRootOrgSourceControl() {
    tempEntity.newSourceControl(level2ChildOrg.getId(), null, null, null, TOKEN, SourceControlProvider.GITLAB, false,
        null, null, null, true, true, "/target/*", true, false, false, false);
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(childApp.getId(), VALID_URL, TOKEN, null, null, true, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.parentValue = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentName = level2ChildOrg.getName();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.token.parentName = level2ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.pullRequestCommentingEnabled.parentName = level2ChildOrg.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = true;
    actualDTO.sourceControlEvaluationsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = level2ChildOrg.getName();
    actualDTO.sourceControlScanTarget.parentValue = "/target/*";
    actualDTO.sshEnabled.parentName = level2ChildOrg.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = level2ChildOrg.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = level2ChildOrg.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelApplicationNotConfigured() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    rootOrgSourcecontrol.setPullRequestCommentingEnabled(false);
    rootOrgSourcecontrol.setSourceControlEvaluationsEnabled(true);
    rootOrgSourcecontrol.setSourceControlScanTarget("target/*");
    rootOrgSourcecontrol.setSshEnabled(true);
    rootOrgSourcecontrol.setCommitStatusEnabled(false);
    rootOrgSourcecontrol.setManualPullRequestsEnabled(false);
    rootOrgSourcecontrol.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControlDAO.update(rootOrgSourcecontrol);

    tempEntity.newSourceControl(level2ChildOrg.getId(), null, TOKEN, null, false, null, null);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = childApp.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.parentName = level2ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    actualDTO.pullRequestCommentingEnabled.parentName = rootOrganization.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = false;
    actualDTO.sourceControlEvaluationsEnabled.parentName = rootOrganization.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = rootOrganization.getName();
    actualDTO.sourceControlScanTarget.parentValue = "target/*";
    actualDTO.sshEnabled.parentName = rootOrganization.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = rootOrganization.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = rootOrganization.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelApplicationA_ppSourceControlOnly() {
    final SourceControl appSourceControl =
        tempEntity.newSourceControl(childApp.getId(), VALID_URL, TOKEN, SourceControlProvider.GITLAB, null, true, null);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.id = appSourceControl.getId();
    actualDTO.ownerId = appSourceControl.getOwnerId();
    actualDTO.provider.value = SourceControlProvider.GITLAB.toString();
    actualDTO.repositoryUrl = VALID_URL;
    actualDTO.token.value = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.value = true;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelApplication_RootOrgSourceControlOnly() {
    rootOrgSourcecontrol.setToken(TOKEN);
    rootOrgSourcecontrol.setBaseBranch("BASE_BRANCH");
    sourceControlDAO.update(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = childApp.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITHUB.toString();
    actualDTO.provider.parentName = rootOrganization.getName();
    actualDTO.token.parentName = rootOrganization.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.statusChecksEnabled.parentName = rootOrganization.getName();
    actualDTO.statusChecksEnabled.parentValue = true;
    actualDTO.remediationPullRequestsEnabled.parentName = rootOrganization.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = true;
    actualDTO.baseBranch.parentName = rootOrganization.getName();
    actualDTO.baseBranch.parentValue = "BASE_BRANCH";
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void testGetCompositeSourceControlByOwner_NLevelApplication_OrgSourceControlOnly() {
    tempEntity.newSourceControl(level2ChildOrg.getId(), null, null, null, TOKEN, SourceControlProvider.GITLAB, false,
        null, null, null, true, true, "/target/*", true, false, false, false);
    sourceControlDAO.delete(rootOrgSourcecontrol);

    final ApiCompositeSourceControlDTO resultDTO = apiCompositeSourceControlService
        .getCompositeSourceControlByOwner(OwnerType.APPLICATION, childApp.getId());

    ApiCompositeSourceControlDTO actualDTO = new ApiCompositeSourceControlDTO();
    actualDTO.ownerId = childApp.getId();
    actualDTO.provider.parentValue = SourceControlProvider.GITLAB.toString();
    actualDTO.provider.parentName = level2ChildOrg.getName();
    actualDTO.token.parentName = level2ChildOrg.getName();
    actualDTO.token.parentValue = FAKE_SECRET_KEY;
    actualDTO.remediationPullRequestsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.remediationPullRequestsEnabled.parentValue = false;
    actualDTO.pullRequestCommentingEnabled.parentName = level2ChildOrg.getName();
    actualDTO.pullRequestCommentingEnabled.parentValue = true;
    actualDTO.sourceControlEvaluationsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.sourceControlEvaluationsEnabled.parentValue = true;
    actualDTO.sourceControlScanTarget.parentName = level2ChildOrg.getName();
    actualDTO.sourceControlScanTarget.parentValue = "/target/*";
    actualDTO.sshEnabled.parentName = level2ChildOrg.getName();
    actualDTO.sshEnabled.parentValue = true;
    actualDTO.commitStatusEnabled.parentName = level2ChildOrg.getName();
    actualDTO.commitStatusEnabled.parentValue = false;
    actualDTO.manualPullRequestsEnabled.parentName = level2ChildOrg.getName();
    actualDTO.manualPullRequestsEnabled.parentValue = false;
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentName = level2ChildOrg.getName();
    actualDTO.innerSourceAutomatedUpdatesEnabled.parentValue = false;
    validateCompositeSourceControlDTO(OwnerType.APPLICATION, resultDTO, actualDTO);
  }

  @Test
  public void getCompositeSourceControlByOwnerDecrypted() throws Exception {
    // given a token at the root org and overridden at the org level
    tempEntity.newSourceControl(level1ChildOrg.getId(), null, plexusCipher.encrypt(TOKEN, ENC), null);

    // when we get source control decrypted
    ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlService.getCompositeSourceControlByOwnerDecrypted(
            OwnerType.ORGANIZATION,
            level1ChildOrg.getId());

    // then the passwords at both levels match
    assertThat(dto.token.value).isEqualTo(TOKEN);
    assertThat(dto.token.parentValue).isEqualTo(ROOT_TOKEN);
  }

  @Test
  public void getCompositeSourceControlByOwner_tokens() throws Exception {
    // given a token at the root org and overridden at the org level
    tempEntity.newSourceControl(level1ChildOrg.getId(), null, plexusCipher.encrypt(TOKEN, ENC), null);

    // when we get source control not decrypted
    ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlService.getCompositeSourceControlByOwner(
            OwnerType.ORGANIZATION,
            level1ChildOrg.getId());

    // then the passwords are redacted
    assertThat(dto.token.value).isEqualTo(FAKE_SECRET_KEY);
    assertThat(dto.token.parentValue).isEqualTo(FAKE_SECRET_KEY);
  }

  private void validateCompositeSourceControlDTO(
      OwnerType currentOwnerType,
      ApiCompositeSourceControlDTO result,
      ApiCompositeSourceControlDTO expected)
  {
    System.out.println("Result >> " + result.toString());
    System.out.println("Expected >> " + expected.toString());

    assertThat(result.ownerId).isEqualTo(expected.ownerId);
    if (expected.id != null) {
      assertThat(result.id).isEqualTo(expected.id);
    }
    else {
      assertThat(result.id).isNull();
    }
    if (currentOwnerType.equals(OwnerType.APPLICATION)) {
      if (expected.repositoryUrl != null) {
        assertThat(result.repositoryUrl).isEqualTo(expected.repositoryUrl);
      }
      else {
        assertThat(result.repositoryUrl).isNull();
      }
    }
    else {
      assertThat(result.repositoryUrl).isNull();
    }

    assertField(result.provider, expected.provider);
    assertField(result.username, expected.username);
    assertField(result.token, expected.token);
    assertField(result.baseBranch, expected.baseBranch);
    assertField(result.remediationPullRequestsEnabled, result.remediationPullRequestsEnabled);
    assertField(result.statusChecksEnabled, expected.statusChecksEnabled);
    assertField(result.pullRequestCommentingEnabled, expected.pullRequestCommentingEnabled);
    assertField(result.sourceControlEvaluationsEnabled, expected.sourceControlEvaluationsEnabled);
    assertField(result.sourceControlScanTarget, expected.sourceControlScanTarget);
    assertField(result.sshEnabled, expected.sshEnabled);
    assertField(result.commitStatusEnabled, expected.commitStatusEnabled);
    assertField(result.manualPullRequestsEnabled, expected.manualPullRequestsEnabled);
    assertField(result.innerSourceAutomatedUpdatesEnabled, expected.innerSourceAutomatedUpdatesEnabled);
    assertField(result.closePrOnFailedChecksEnabled, expected.closePrOnFailedChecksEnabled);
    assertField(result.closePrAfterDaysOpenEnabled, expected.closePrAfterDaysOpenEnabled);
    assertField(result.closePrAfterDays, expected.closePrAfterDays);
    assertField(result.authenticationType, expected.authenticationType);
  }

  private <T> void assertField(ApiCompositeValueDTO<T> actualField, ApiCompositeValueDTO<T> expectedField) {
    if (expectedField == null) {
      assertThat(actualField).isNull();
      return;
    }

    if (expectedField.value == null) {
      assertThat(actualField.value).isNull();
    }
    else {
      assertThat(actualField.value).isEqualTo(expectedField.value);
    }

    if (expectedField.parentValue == null) {
      assertThat(actualField.parentValue).isNull();
    }
    else {
      assertThat(actualField.parentValue).isEqualTo(expectedField.parentValue);
    }

    if (expectedField.parentName == null) {
      assertThat(actualField.parentName).isNull();
    }
    else {
      assertThat(actualField.parentName).isEqualTo(expectedField.parentName);
    }
  }
}
