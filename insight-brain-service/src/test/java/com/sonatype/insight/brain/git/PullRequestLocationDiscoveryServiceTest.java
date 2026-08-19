/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.RankedSourceLocation;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.sonatype.insight.brain.testsupport.TempFolder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PullRequestLocationDiscoveryServiceTest
    extends VerifiableLoggingTestBase
{
  @RegisterExtension
  public TempFolder temporaryFolder = new TempFolder();

  @Mock
  private GitApiFactory gitApiFactory;

  @Mock
  private GitApi gitApi;

  @Mock
  private LocationDiscoveryExecutor locationDiscoveryExecutor;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private SourceControlSshService sourceControlSshService;

  private final GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo("https://github.com/org/proj", null, "user",
      "token", SourceControlProvider.GITHUB, "master", true, true, true, true, true, true, false, null);

  private final String branch = "branch";

  private final String applicationId = "appId";

  private final PolicyEvaluation evaluation =
      new PolicyEvaluation(applicationId, "stage-type-id", "scan-id", CurrentUser.SYSTEM, ScanTriggerType.CLI);

  // Subject
  private PullRequestLocationDiscoveryService locationDiscoveryService;

  public PullRequestLocationDiscoveryServiceTest() {
    super(PullRequestLocationDiscoveryService.class);
  }

  @BeforeEach
  public void setUp() {
    super.setup();

    when(gitApiFactory.createGitApi(gitRepositoryInfo)).thenReturn(gitApi);

    Application application = new Application("app-one", "app one", Organization.ROOT_ORGANIZATION_ID);
    when(applicationDAO.getById(applicationId)).thenReturn(application);
    when(mockSourceControlUtils.getCheckoutDirectory(application))
        .thenReturn(new File(temporaryFolder.getRoot(), applicationId));

    locationDiscoveryService = new PullRequestLocationDiscoveryService(
        gitApiFactory, applicationDAO, locationDiscoveryExecutor, mockSourceControlUtils, sourceControlSshService);
  }

  @Test
  public void testDoLocationDiscovery_EmptyViolationList() throws GitException {
    // given:
    List<PolicyViolation> violationList = new ArrayList<>();

    // when:
    LocationDiscoveryResult discoveryResult =
        locationDiscoveryService.doLocationDiscovery(violationList, gitRepositoryInfo, branch, applicationId);

    // then: no locations discovered
    assertThat(discoveryResult).isNull();
    verify(locationDiscoveryExecutor, never()).execute(any());
    verifySshServiceInvoked();
  }

  @Test
  public void testDoLocationDiscovery_NoComponentIdentifiersInViolationList() throws GitException {
    // given:
    List<PolicyViolation> violationList = new ArrayList<>();
    PolicyViolation violation = new PolicyViolationBuilder().build();
    violationList.add(violation);

    // when:
    LocationDiscoveryResult discoveryResult =
        locationDiscoveryService.doLocationDiscovery(violationList, gitRepositoryInfo, branch, applicationId);

    // then: no locations discovered
    assertThat(discoveryResult).isNull();
    verify(locationDiscoveryExecutor, never()).execute(any());
    verifySshServiceInvoked();
  }

  @Test
  public void testDoLocationDiscovery_ExceptionInLocationDiscovery() throws GitException {
    // given:
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("comp-1", "1.1.0");
    List<PolicyViolation> violationList = new ArrayList<>();
    PolicyViolation violation = new PolicyViolationBuilder()
        .withComponentIdentifier(identifier)
        .build();
    violationList.add(violation);

    when(locationDiscoveryExecutor.execute(any())).thenThrow(new RuntimeException("simulated"));

    // when:
    LocationDiscoveryResult discoveryResult =
        locationDiscoveryService.doLocationDiscovery(violationList, gitRepositoryInfo, branch, applicationId);

    // then: no locations discovered
    assertThat(discoveryResult).isNull();
    assertThatLogMessagesEqual(
        debug("Pull request location discovery initiated for application 'appId'"),
        error("Failed to execute pull request location discovery"));
    verifySshServiceInvoked();
  }

  @Test
  public void testDoLocationDiscovery_NoLocationFound() throws GitException {
    // given:
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("comp-1", "1.1.0");
    List<PolicyViolation> violationList = new ArrayList<>();
    PolicyViolation violation = new PolicyViolationBuilder()
        .withComponentIdentifier(identifier)
        .build();
    violationList.add(violation);

    when(locationDiscoveryExecutor.execute(any())).thenReturn(new LocationDiscoveryResult());

    // when:
    LocationDiscoveryResult discoveryResult =
        locationDiscoveryService.doLocationDiscovery(violationList, gitRepositoryInfo, branch, applicationId);

    // then: at least one location discovered
    assertThat(discoveryResult).isNotNull();
    assertThat(discoveryResult.getLocationMap()).isEmpty();
    assertThatLogMessagesEqual(
        debug("Pull request location discovery initiated for application 'appId'"),
        debug("Pull request location discovery completed for application 'appId': 0 components found"));
    verifySshServiceInvoked();
  }

  @Test
  public void testDoLocationDiscovery_LocationFound() throws GitException {
    // given:
    ComponentIdentifier identifier = ComponentIdentifier.createNpmCoordinates("comp-1", "1.1.0");
    List<PolicyViolation> violationList = new ArrayList<>();
    PolicyViolation violation = new PolicyViolationBuilder()
        .withComponentIdentifier(identifier)
        .build();
    violationList.add(violation);

    LocationDiscoveryResult result = new LocationDiscoveryResult();
    List<RankedSourceLocation> list = new LinkedList<>();
    list.add(new RankedSourceLocation("path", 1, "456", 1));
    result.getLocationMap().put(identifier, list);
    when(locationDiscoveryExecutor.execute(any())).thenReturn(result);

    // when:
    LocationDiscoveryResult discoveryResult =
        locationDiscoveryService.doLocationDiscovery(violationList, gitRepositoryInfo, branch, applicationId);

    // then: at least one location discovered
    assertThat(discoveryResult).isNotNull();
    assertThat(discoveryResult.getLocationMap()).isNotEmpty();
    assertThatLogMessagesEqual(
        debug("Pull request location discovery initiated for application 'appId'"),
        debug("Pull request location discovery completed for application 'appId': 1 components found"));
    verifySshServiceInvoked();
  }

  private class PolicyViolationBuilder
  {
    private ComponentIdentifier componentIdentifier;

    PolicyViolationBuilder withComponentIdentifier(ComponentIdentifier componentIdentifier) {
      this.componentIdentifier = componentIdentifier;
      return this;
    }

    PolicyViolation build() {
      ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "operatorName");
      return new PolicyViolation(evaluation, "policyId", "policyName", 5, PolicyThreatCategory.LICENSE, "hash",
          componentIdentifier, Collections.singletonList(constraintFact), null);
    }
  }

  private void verifySshServiceInvoked() {
    verify(sourceControlSshService, times(1)).verifySshUrlAndUpdateIfNeeded(applicationId);
  }
}
