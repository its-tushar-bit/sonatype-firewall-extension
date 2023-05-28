/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeValueDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.model.SCMRepository;
import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.google.inject.Binder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScmOnboardingServiceBatchImportTest
    extends AbstractComponentTest
{
  @Inject
  private ScmOnboardingService scmOnboardingService;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Mock
  private SourceControlOrganizationImportEventDAO mockScmImportEventDAO;

  @Mock
  private ApiCompositeSourceControlService mockCompositeSourceControlService;

  @Mock
  private GitClientFactory mockGitClientFactory;

  @Spy
  private SourceControlDAO sourceControlDAO;

  @Mock
  private GeneralSCMApiClient mockScmClient;

  private SourceControl rootOrgSourceControl;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private TestProductLicense testProductLicense;

  private static final String ENC = "CMMDwoV";

  @Override
  public void configure(final Binder binder) {
    binder.bind(SourceControlEventPublisher.class).toInstance(mockSourceControlEventPublisher);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(SourceControlOrganizationImportEventDAO.class).toInstance(mockScmImportEventDAO);
    binder.bind(GitClientFactory.class).toInstance(mockGitClientFactory);
    binder.bind(ApiCompositeSourceControlService.class).toInstance(mockCompositeSourceControlService);
    super.configure(binder);
  }

  @Before
  public void before() throws Exception {
    testProductLicense.setMaxApplications(500);
    rootOrgSourceControl = tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt("TOKEN", ENC), SourceControlProvider.GITHUB);
    rootOrgSourceControl.setSourceControlEvaluationsEnabled(true);
    sourceControlDAO.update(rootOrgSourceControl);
    when(mockGitClientFactory.createGeneralApiClient(any(SourceControlProvider.class), any(String.class), any(), any()))
        .thenReturn(mockScmClient);
    when(mockCompositeSourceControlService.getCompositeSourceControlByOwnerDecrypted(eq(OwnerType.ORGANIZATION), any()))
        .thenReturn(mockSourceControlDTO());
  }

  @After
  public void after() {
    testProductLicense.reset();
  }

  private ApiCompositeSourceControlDTO mockSourceControlDTO() {
    ApiCompositeSourceControlDTO dto = new ApiCompositeSourceControlDTO();
    dto.provider = new ApiCompositeValueDTO<>();
    dto.provider.value = SourceControlProvider.GITHUB.toString();
    dto.token = new ApiCompositeValueDTO<>();
    dto.token.value = "token";
    dto.username = new ApiCompositeValueDTO<>();
    dto.username.value = "user";
    return dto;
  }

  @Test
  public void testDoScmOrganizationImport_UpdatesStatusAtEvery100thImport() throws Exception {
    when(mockScmClient.listAllRepositories()).thenReturn(testDataSet(210));

    SourceControlOrganizationImportEvent mockEvent = tempEntity.newSourceControlOrganizationImportEvent();
    scmOnboardingService.doScmOrganizationImport(mockEvent);

    //updates at success counts 100, 200 and finally at 210
    //Given the same object is being updated the mockito argument verify methods unable to capture
    // the different attribute values in the multiple invocations
    verify(mockScmImportEventDAO, times(3)).update(any(SourceControlOrganizationImportEvent.class));
  }

  private List<SCMRepository> testDataSet(int numOfScmReposToMock) {
    List<SCMRepository> results = new ArrayList<>();
    for (int i = 0; i < numOfScmReposToMock; i++) {
      String owner = RandomStringUtils.randomAlphanumeric(5);
      String repo = RandomStringUtils.randomAlphanumeric(10);
      results.add(
          new SCMRepository(SourceControlProvider.GITHUB, String.format("https://github.com/%s/%s", owner, repo),
              String.format("git://github.com/%s/%s.git", owner, repo), false, owner, repo,
              "random description", "main"));
    }
    return results;
  }
}
