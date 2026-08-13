/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link SourceControlDAOTest} (CLM-45228).
 */
@RunWith(MockitoJUnitRunner.class)
@PostgresTest
public class SourceControlDAOPgTest
    extends AbstractDbDAOTest
{
  private static final String NULL_REPO_URL = null;

  private static final String VALID_URL = "https://example.com/organization/Project.git";

  private static final String VALID_NORMALIZED_URL = "https://example.com/organization/Project";

  private static final String VALID_SSH_URL = "git@example.com:organization/Project.git";

  private static final int INTERVAL_IN_HOURS = 24;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private SourceControlDAO sourceControlDAO;

  private Application app;

  private Organization org;

  private DAOSecretRotator daoSecretRotator;

  @Mock
  private SourceControlSshValidator sourceControlSshValidator;

  @Override
  @Before
  public void setup() {
    policyEvaluationDAO = daoFactory.createPolicyEvaluationDAO();

    sourceControlDAO = daoFactory.createSourceControlDAO();
    sourceControlPullRequestDAO = daoFactory.createSourceControlPullRequestDAO();

    daoSecretRotator = new DAOSecretRotator();

    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @After
  public void cleanup() {
    if (sourceControlDAO != null) {
      sourceControlDAO.getAll().forEach(sourceControlDAO::delete);
    }
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_noMatchingApp_postgres() {
    // given: a root organization source control
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, NULL_REPO_URL, "fake token", GITHUB);

    // when: build the composite source control for an application that does not exist
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId("contrived app id");

    // then: null result
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_inheritFromRoot_postgres() {
    // given: a hierarchy with all attributes inheriting from the root org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgID", "appOne")
        .withProvider(GITHUB, null, null)
        .withToken("fakeToken", null, null)
        .withDefaultBranch("main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, null)
        .withRemediationPullRequests(true, null, null)
        .withSourceControlEvaluations(false, null, null)
        .withSsh(false, null, null)
        .withCommitStatusEnabled(false, null, null)
        .withManualPullRequestsEnabled(false, null, null)
        .withStatusChecks(false, null, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: composite source control is built successfully
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_CommitStatusEnabled_AllNull_postgres() {
    // given: a hierarchy with commit status enabled set to null
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITHUB, null, null)
        .withDefaultBranch("main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withCommitStatusEnabled(null, null, null)
        .withManualPullRequestsEnabled(null, null, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: composite source control is built, with commitStatusEnabled having a null value
    assertThat(sourceControl.getCommitStatusEnabled()).isNull();
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_inheritFromIntermediateOrg_postgres() {
    // given: a hierarchy with multiple nested organizations
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "org1", "org2", "org3", "appOne")
        .withProvider(GITLAB, null, GITHUB, null, null)
        .withToken("rootToken", null, "org2.token", null, null)
        .withDefaultBranch("trunk", null, "main", null, null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, null, true, null, null)
        .withRemediationPullRequests(true, null, false, null, null)
        .withSourceControlEvaluations(false, null, true, null, null)
        .withSsh(false, null, true, null, null)
        .withCommitStatusEnabled(false, null, true, null, null)
        .withStatusChecks(false, null, true, null, null)
        .withManualPullRequestsEnabled(false, null, true, null, null)
        .branchFrom("org2", "org4", "app2")
        .withRepositoryUrl("https://test.sonatype.com/app/2", "ssh://test.sonatype.com/app/2.git")
        .withToken("org4.token", null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationId_inheritFromOrg_postgres() {
    // given: a hierarchy with everything inheriting from parent org
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITLAB, GITHUB, null)
        .withToken("rootToken", "gh-token", null)
        .withDefaultBranch("trunk", "main", null)
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, null)
        .withRemediationPullRequests(true, false, null)
        .withSourceControlEvaluations(false, true, null)
        .withSsh(false, true, null)
        .withCommitStatusEnabled(false, true, null)
        .withStatusChecks(false, true, null)
        .withManualPullRequestsEnabled(false, true, null)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testBuildCompositeSourceControlForApplicationid_overrideAll_postgres() {
    // given: a hierarchy with everything overridden in the app source control
    TestableHierarchy testableHierarchy = new TestableHierarchy()
        .with_N_OrgsAndAnApp(ROOT_ORGANIZATION_ID, "orgId", "appOne")
        .withProvider(GITLAB, GITHUB, GITLAB)
        .withToken("rootToken", "gh-token", "gl-token")
        .withDefaultBranch("trunk", "main", "develop")
        .withRepositoryUrl("https://test.sonatype.com/app/1", "ssh://test.sonatype.com/app/1.git")
        .withPullRequestCommenting(false, true, false)
        .withRemediationPullRequests(true, null, false)
        .withSourceControlEvaluations(false, null, true)
        .withSsh(false, true, false)
        .withCommitStatusEnabled(false, true, false)
        .withStatusChecks(false, null, true)
        .withManualPullRequestsEnabled(false, true, false)
        .build();

    // when: build the composite source control for appOne
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlForApplicationId(
        testableHierarchy.getApplication("appOne").getId());

    // then: source control is built correctly
    assertSourceControl(
        sourceControl,
        testableHierarchy.getExpectedCompositeSourceControl(sourceControl.getOwnerId()));
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    for (int i = 0; i < 4; i++) {
      Application app = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
      tempEntity.newSourceControl(app.getId(), "https://github.com/some/repo", "token_" + i + "_old",
          SourceControlProvider.GITHUB);
    }

    Function<String, String> secretRotator = secret -> secret.replace("old", "new");

    daoSecretRotator.rotateEncryptedSecrets(sourceControlDAO, secretRotator);

    List<SourceControl> results = sourceControlDAO.getAll();

    assertThat(results.stream().filter(sc -> sc.getToken() == null).count()).isEqualTo(1);
    assertThat(results.stream().filter(sc -> sc.getToken() != null).count()).isEqualTo(4);
    results.stream()
        .filter(sc -> sc.getToken() != null)
        .forEach(sc -> {
          assertThat(sc.getToken()).doesNotContain("old");
          assertThat(sc.getToken()).contains("new");
        });
  }

  private void assertSourceControl(SourceControl actualSC, SourceControl expectedSC) {
    assertThat(actualSC.getId()).isEqualTo(expectedSC.getId());
    assertThat(actualSC.getOwnerId()).isEqualTo(expectedSC.getOwnerId());
    assertThat(actualSC.getRepositoryUrl()).isEqualTo(expectedSC.getRepositoryUrl());
    assertThat(actualSC.getUsername()).isEqualTo(expectedSC.getUsername());
    assertThat(actualSC.getToken()).isEqualTo(expectedSC.getToken());
    assertThat(actualSC.getProvider()).isEqualTo(expectedSC.getProvider());
    assertThat(actualSC.getBaseBranch()).isEqualTo(expectedSC.getBaseBranch());
    assertThat(actualSC.getRemediationPullRequestsEnabled()).isEqualTo(expectedSC.getRemediationPullRequestsEnabled());
    assertThat(actualSC.getStatusChecksEnabled()).isEqualTo(expectedSC.getStatusChecksEnabled());
    assertThat(actualSC.getPullRequestCommentingEnabled()).isEqualTo(expectedSC.getPullRequestCommentingEnabled());
    assertThat(actualSC.getPullRequestPollTime()).isEqualTo(expectedSC.getPullRequestPollTime());
    assertThat(actualSC.getPullRequestErrorCount()).isEqualTo(expectedSC.getPullRequestErrorCount());
    assertThat(actualSC.getSourceControlEvaluationsEnabled())
        .isEqualTo(expectedSC.getSourceControlEvaluationsEnabled());
    assertThat(actualSC.getSourceControlScanTarget()).isEqualTo(expectedSC.getSourceControlScanTarget());
    assertThat(actualSC.getManualPullRequestsEnabled()).isEqualTo(expectedSC.getManualPullRequestsEnabled());
    assertThat(actualSC.getNonGoldenPullRequestsEnabled()).isEqualTo(expectedSC.getNonGoldenPullRequestsEnabled());
  }

  private class TestableHierarchy
  {
    private final List<SourceControl> sourceControlList = new ArrayList<>();

    private final Map<String, SourceControl> sourceControlMap = new HashMap<>();

    private final Map<String, String> childParentMap = new HashMap<>();

    private final Map<String, Application> applicationMap = new HashMap<>();

    private SourceControl currentAppSourceControl;

    private int hierarchyDepth;

    private int offset = 0;

    private Application getApplication(String applicationId) {
      return applicationMap.get(applicationId);
    }

    private SourceControl getSourceControl(int relativeIndex) {
      return sourceControlList.get(offset + relativeIndex);
    }

    private void setupHierarchy(String parent, String... ownerIds) {
      String currentParent = parent;
      for (String ownerId : ownerIds) {
        childParentMap.put(ownerId, currentParent);
        currentParent = ownerId;
      }
    }

    private TestableHierarchy branchFrom(String orgId, String... ownerIds) {
      setupHierarchy(orgId, ownerIds);
      offset = sourceControlList.size();
      hierarchyDepth = ownerIds.length;
      addOrgsAndApp(orgId, ownerIds);
      return this;
    }

    private TestableHierarchy with_N_OrgsAndAnApp(String... ownerIds) {
      assertThat(ROOT_ORGANIZATION_ID).isEqualTo(ownerIds[0]);
      hierarchyDepth = ownerIds.length;
      setupHierarchy(null, ownerIds);

      SourceControl sc = new SourceControl();
      sc.setOwnerId(ROOT_ORGANIZATION_ID);
      sourceControlList.add(sc);
      sourceControlMap.put(ROOT_ORGANIZATION_ID, sc);

      return addOrgsAndApp(ROOT_ORGANIZATION_ID, Arrays.copyOfRange(ownerIds, 1, ownerIds.length));
    }

    private TestableHierarchy addOrgsAndApp(String parentOwnerId, String... ownerIds) {
      for (int i = 0; i < ownerIds.length - 1; i++) {
        Organization org = tempEntity.newOrganizationWithSpecificIdAndParent(ownerIds[i], ownerIds[i], parentOwnerId);

        SourceControl sc = new SourceControl();
        sc.setOwnerId(org.getId());
        sourceControlList.add(sc);
        sourceControlMap.put(org.getId(), sc);

        parentOwnerId = ownerIds[i];
      }
      String applicationId = ownerIds[ownerIds.length - 1];
      Application app =
          tempEntity.newApplicationWithSpecificId(applicationId, applicationId, applicationId, parentOwnerId);
      applicationMap.put(applicationId, app);
      currentAppSourceControl = new SourceControl();
      currentAppSourceControl.setOwnerId(app.getId());
      sourceControlList.add(currentAppSourceControl);
      sourceControlMap.put(app.getId(), currentAppSourceControl);
      return this;
    }

    private TestableHierarchy withRepositoryUrl(String repositoryUrl, String sshUrl) {
      currentAppSourceControl.setRepositoryUrl(repositoryUrl);
      currentAppSourceControl.setRepositorySshUrl(sshUrl);
      return this;
    }

    private TestableHierarchy withProvider(SourceControlProvider... providers) {
      assertHierarchyDepth(providers.length);
      for (int i = 0; i < providers.length; i++) {
        getSourceControl(i).setProvider(providers[i]);
      }
      return this;
    }

    private TestableHierarchy withToken(String... tokens) {
      assertHierarchyDepth(tokens.length);
      for (int i = 0; i < tokens.length; i++) {
        getSourceControl(i).setToken(tokens[i]);
      }
      return this;
    }

    private TestableHierarchy withDefaultBranch(String... branches) {
      assertHierarchyDepth(branches.length);
      for (int i = 0; i < branches.length; i++) {
        getSourceControl(i).setBaseBranch(branches[i]);
      }
      return this;
    }

    private TestableHierarchy withRemediationPullRequests(Boolean... remediationFlags) {
      assertHierarchyDepth(remediationFlags.length);
      for (int i = 0; i < remediationFlags.length; i++) {
        getSourceControl(i).setRemediationPullRequestsEnabled(remediationFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withPullRequestCommenting(Boolean... commentingFlags) {
      assertHierarchyDepth(commentingFlags.length);
      for (int i = 0; i < commentingFlags.length; i++) {
        getSourceControl(i).setPullRequestCommentingEnabled(commentingFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withSourceControlEvaluations(Boolean... sourceControlEvaluationFlags) {
      assertHierarchyDepth(sourceControlEvaluationFlags.length);
      for (int i = 0; i < sourceControlEvaluationFlags.length; i++) {
        getSourceControl(i).setSourceControlEvaluationsEnabled(sourceControlEvaluationFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withSsh(Boolean... sshFlags) {
      assertHierarchyDepth(sshFlags.length);
      for (int i = 0; i < sshFlags.length; i++) {
        getSourceControl(i).setSshEnabled(sshFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withCommitStatusEnabled(Boolean... commitStatusEnabledFlags) {
      assertHierarchyDepth(commitStatusEnabledFlags.length);
      for (int i = 0; i < commitStatusEnabledFlags.length; i++) {
        getSourceControl(i).setCommitStatusEnabled(commitStatusEnabledFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withStatusChecks(Boolean... statusCheckFlags) {
      assertHierarchyDepth(statusCheckFlags.length);
      for (int i = 0; i < statusCheckFlags.length; i++) {
        getSourceControl(i).setStatusChecksEnabled(statusCheckFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withManualPullRequestsEnabled(Boolean... manualPullRequestsEnabledFlags) {
      assertHierarchyDepth(manualPullRequestsEnabledFlags.length);
      for (int i = 0; i < manualPullRequestsEnabledFlags.length; i++) {
        getSourceControl(i).setManualPullRequestsEnabled(manualPullRequestsEnabledFlags[i]);
      }
      return this;
    }

    private TestableHierarchy withNonGoldenPullRequestsEnabled(Boolean... nonGoldenPullRequestsEnabledFlags) {
      assertHierarchyDepth(nonGoldenPullRequestsEnabledFlags.length);
      for (int i = 0; i < nonGoldenPullRequestsEnabledFlags.length; i++) {
        getSourceControl(i).setNonGoldenPullRequestsEnabled(nonGoldenPullRequestsEnabledFlags[i]);
      }
      return this;
    }

    private void assertHierarchyDepth(int depth) {
      assertThat(depth).isEqualTo(hierarchyDepth);
    }

    private SourceControl getExpectedCompositeSourceControl(String appId) {
      SourceControl composite = new SourceControl();
      SourceControl sc = sourceControlMap.get(appId);
      SourceControl.coalesce(composite, sc);
      while (null != (sc = sourceControlMap.get(childParentMap.get(sc.getOwnerId())))) {
        SourceControl.coalesce(composite, sc);
      }
      return composite;
    }

    TestableHierarchy build() {
      List<SourceControl> list = new ArrayList<>();
      for (SourceControl sourceControl : sourceControlList) {
        list.add(tempEntity.newSourceControl(sourceControl));
      }
      sourceControlList.clear();
      sourceControlList.addAll(list);

      return this;
    }
  }
}
