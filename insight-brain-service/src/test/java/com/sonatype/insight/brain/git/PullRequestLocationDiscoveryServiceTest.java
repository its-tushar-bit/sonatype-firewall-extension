/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.RankedSourceLocation;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestLocationDiscoveryServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private GitApiFactory gitApiFactory;

  @Mock
  private GitApi gitApi;

  @Mock
  private LocationDiscoveryExecutor locationDiscoveryExecutor;

  private InsightConfig insightConfig = new InsightConfig();

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private FileCleaner fileCleaner;

  private GitRepositoryInfo gitRepositoryInfo =
      new GitRepositoryInfo("https://github.com/org/proj", "user", "token", SourceControlProvider.GITHUB,
          "master", true, true);

  private String branch = "branch";

  private String applicationId = "appId";

  private PolicyEvaluation evaluation = new PolicyEvaluation(applicationId, "stage-type-id", "scan-id", "system");

  // Subject
  private PullRequestLocationDiscoveryService locationDiscoveryService;

  public PullRequestLocationDiscoveryServiceTest() {
    super(PullRequestLocationDiscoveryService.class);
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    super.setup();

    when(gitApiFactory.createGitApi(gitRepositoryInfo)).thenReturn(gitApi);

    Application application = new Application("app-one", "app one", Organization.ROOT_ORGANIZATION_ID);
    when(applicationDAO.getById(applicationId)).thenReturn(application);

    locationDiscoveryService = new PullRequestLocationDiscoveryService(
        gitApiFactory, applicationDAO, locationDiscoveryExecutor, insightConfig, fileCleaner);
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
        error("Failed to execute pull request location discovery")
    );
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
        debug("Pull request location discovery completed for application 'appId': 0 components found")
    );
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
    list.add(new RankedSourceLocation("path", 1, 1));
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
        debug("Pull request location discovery completed for application 'appId': 1 components found")
    );
  }

  private class PolicyViolationBuilder
  {
    private ComponentIdentifier componentIdentifier;

    PolicyViolationBuilder withComponentIdentifier(ComponentIdentifier componentIdentifier) {
      this.componentIdentifier = componentIdentifier;
      return this;
    }

    PolicyViolation build() {
      return new PolicyViolation(evaluation, "policyId", "policyName", 5, PolicyThreatCategory.LICENSE, "hash",
          componentIdentifier, "{}", null);
    }
  } 
}
