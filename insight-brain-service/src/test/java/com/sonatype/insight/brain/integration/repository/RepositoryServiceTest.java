/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.RepositoryComponentPathnames;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

/**
 * @since 1.17
 */
@Category(SlowTest.class)
public class RepositoryServiceTest
    extends AbstractRepositoryServiceTest
{
  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  private RepositoryService repositoryService;

  @Override
  protected AbstractRepositoryService getRepositoryService() {
    return repositoryService;
  }

  @Test
  public void testRemoveExtraComponents() {
    Date now = new Date();
    RepositoryManager repoManager = tempEntity.newRepositoryManager("testRepoManagerInstanceId");
    Repository repository1 = tempEntity.newRepository(repoManager, "testRepoPublicId1", true);
    ProxyRepositoryComponent componentRepo1ToKeep =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_1", now);
    ProxyRepositoryComponent componentRepo1ToDelete =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_2", now);
    ProxyRepositoryComponent componentRepo1ToKeepBecauseItIsNewer =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_3", new Date(now.getTime() + 1));

    Repository repository2 = tempEntity.newRepository(repoManager, "testRepoPublicId2", true);
    ProxyRepositoryComponent componentRepo2 =
        tempEntity.newRepositoryComponent(repository2.getId(), "pathname2_1", now);

    RepositoryComponentPathnames repositoryComponentPathnames = new RepositoryComponentPathnames();
    repositoryComponentPathnames.time = now;
    repositoryComponentPathnames.pathnames.add(componentRepo1ToKeep.getPathname());

    repositoryService.removeExtraComponents("testRepoManagerInstanceId", "testRepoPublicId1",
        repositoryComponentPathnames);

    ProxyRepositoryComponentDAO proxyRepositoryComponentDAO = this.proxyRepositoryComponentDAO;
    assertThat(proxyRepositoryComponentDAO.getById(componentRepo1ToKeep.getId())).isNotNull();
    assertThat(proxyRepositoryComponentDAO.getById(componentRepo1ToDelete.getId())).isNull();
    assertThat(proxyRepositoryComponentDAO.getById(componentRepo1ToKeepBecauseItIsNewer.getId())).isNotNull();
    assertThat(proxyRepositoryComponentDAO.getById(componentRepo2.getId())).isNotNull();
  }

  @Test
  public void testRemoveExtraComponents_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> repositoryService.removeExtraComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testEvaluateComponentsAdhoc_ViolatePolicy() {
    Repository repo = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "somepath/foobar", "somehash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    mockHdsRequestForComponent(componentEvaluationDataRequestList,
        ImmutableMap.of(componentEvaluationDataRequest, componentIdentifier), securityVulnerabilities);

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList = repositoryService
        .evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, null);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults).hasSize(1);

    List<PolicyAlert> policyAlerts = repositoryComponentEvaluationDataList.componentEvalResults.get(0).policyAlerts;
    assertThat(policyAlerts).hasSize(1);

    PolicyFact policyFact = policyAlerts.get(0).getTrigger();
    assertThat(policyFact.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyFact.getPolicyName()).isEqualTo(policy.getName());
    assertThat(policyFact.getThreatLevel()).isEqualTo(policy.getThreatLevel());
    assertThat(policyFact.getComponentFacts()).hasSize(1);

    ComponentFact componentFact = policyFact.getComponentFacts().get(0);
    assertThat(componentFact.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(componentFact.getHash()).isEqualTo("somehash");
    assertThat(componentFact.getPathnames()).containsExactly("somepath/foobar");

    // test that the component is not persisted
    List<ProxyRepositoryComponent> repositoryComponents = proxyRepositoryComponentDAO.getByRepositoryId(repo.getId());
    assertThat(repositoryComponents).isEmpty();

    // test that the policy violation is not persisted
    List<ProxyRepositoryPolicyViolation> policyViolations =
        proxyRepositoryPolicyViolationDAO.getByRepositoryId(repo.getId());
    assertThat(policyViolations).isEmpty();
  }

  @Test
  public void testEvaluateComponentsAdhoc_MultipleComponents() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest1 =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar1", "hash1");
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest2 =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar2", "hash2");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(
            Arrays.asList(componentEvaluationDataRequest1, componentEvaluationDataRequest2));

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("foobar1", "1.0.0");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("foobar2", "1.0.0");
    mockHdsRequestForComponent(componentEvaluationDataRequestList, ImmutableMap.of(componentEvaluationDataRequest1,
        componentIdentifier1, componentEvaluationDataRequest2, componentIdentifier2), Collections.emptyList());

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList = repositoryService
        .evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, null);

    RepositoryComponentEvaluationData evaluationData1 =
        repositoryComponentEvaluationDataList.componentEvalResults.get(0);
    assertThat(evaluationData1.quarantine).isFalse();
    assertThat(evaluationData1.requestIndex).isEqualTo(0);
    assertThat(evaluationData1.policyAlerts).isEmpty();

    RepositoryComponentEvaluationData evaluationData2 =
        repositoryComponentEvaluationDataList.componentEvalResults.get(1);
    assertThat(evaluationData2.quarantine).isFalse();
    assertThat(evaluationData2.requestIndex).isEqualTo(1);
    assertThat(evaluationData2.policyAlerts).isEmpty();
  }

  @Test
  public void testEvaluateComponentsAdhoc_RepositoryExistsAndEnabled() {
    Repository repo = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    repo.setAuditEnabled(true);
    repositoryDAO.update(repo);

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");
    mockHdsRequestForComponent(componentEvaluationDataRequestList,
        ImmutableMap.of(componentEvaluationDataRequest, componentIdentifier),
        Collections.emptyList());

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList =
        repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults).hasSize(1);

    repo = repositoryDAO.getById(repo.getId());
    assertThat(repo.isAuditEnabled()).isTrue();
  }

  @Test
  public void testEvaluateComponentsAdhoc_RepositoryExistsAndNotEnabled() {
    Repository repo = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    repo.setAuditEnabled(false);
    repositoryDAO.update(repo);

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");
    mockHdsRequestForComponent(componentEvaluationDataRequestList,
        ImmutableMap.of(componentEvaluationDataRequest, componentIdentifier),
        Collections.emptyList());

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList =
        repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults).hasSize(1);

    repo = repositoryDAO.getById(repo.getId());
    assertThat(repo.isAuditEnabled()).isFalse();
  }

  @Test
  public void testEvaluateComponentsAdhoc_RepositoryDoesNotExist() {
    Repository repo =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(repo).isNull();

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");
    mockHdsRequestForComponent(componentEvaluationDataRequestList,
        ImmutableMap.of(componentEvaluationDataRequest, componentIdentifier),
        Collections.emptyList());

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList =
        repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults).hasSize(1);

    // check that a repository was created and it is not enabled
    repo = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(repo).isNotNull();
    assertThat(repo.isAuditEnabled()).isFalse();
  }

  @Test
  public void testEvaluateComponentsAdhoc_NoViolations() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");
    mockHdsRequestForComponent(componentEvaluationDataRequestList,
        ImmutableMap.of(componentEvaluationDataRequest, componentIdentifier),
        Collections.emptyList());

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList =
        repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults.get(0).policyAlerts).hasSize(0);
  }

  @Test
  public void testEvaluateComponentsAdhoc_PathnameSlashPrefix() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "/somepath/foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    mockHdsRequestForComponent(componentEvaluationDataRequestList,
        ImmutableMap.of(componentEvaluationDataRequest, componentIdentifier),
        securityVulnerabilities);

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList =
        repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults).hasSize(1);

    // check that the forward slash prefix was deleted from the component pathname
    PolicyFact policyFact =
        repositoryComponentEvaluationDataList.componentEvalResults.get(0).policyAlerts.get(0).getTrigger();
    ComponentFact componentFact = policyFact.getComponentFacts().get(0);
    assertThat(componentFact.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(componentFact.getPathnames()).containsExactly("somepath/foobar");
  }

  @Test
  public void testEvaluateComponentsAdhoc_LongHash() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    String hash = "01234567890123456789";
    String longHash = hash + "1";
    // Sanity check
    assertThat(longHash.length()).isGreaterThan(HashHelper.MAX_LENGTH);

    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", longHash);
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");

    // Prepare request and mock the HDS request
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(
        componentIdentifier, hash, MatchState.EXACT, 0, Collections.emptySet(), Collections.emptySet(),
        securityVulnerabilities, 0);
    hdsResult.components.add(componentEvaluationData);
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList =
        repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null);
    assertThat(repositoryComponentEvaluationDataList.componentEvalResults).hasSize(1);

    // check that hash was treated correctly
    PolicyFact policyFact =
        repositoryComponentEvaluationDataList.componentEvalResults.get(0).policyAlerts.get(0).getTrigger();
    assertThat(policyFact.getComponentFacts().get(0).getHash()).isEqualTo(hash);
  }

  @Test
  public void testEvaluateComponentsAdhoc_NullFormat() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(null, "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponentsAdhoc_NullHash() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", null);
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponentsAdhoc_NullPathname() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", null, "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    // NEW BEHAVIOR: No exception thrown, invalid component is filtered out
    RepositoryComponentEvaluationDataList result = repositoryService.evaluateComponentsAdhoc(
        REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, null);

    // Verify empty result since the only component was invalid
    assertThat(result).isNotNull();
    assertThat(result.componentEvalResults).isEmpty();
    assertThat(componentEvaluationDataRequestList.components)
        .as("Invalid component should be filtered out")
        .isEmpty();
  }

  @Test
  public void testEvaluateComponentsAdhoc_EmptyFormat() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponentsAdhoc_EmptyHash() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponentsAdhoc_EmptyPathname() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    // NEW BEHAVIOR: No exception thrown, invalid component is filtered out
    RepositoryComponentEvaluationDataList result = repositoryService.evaluateComponentsAdhoc(
        REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, null);

    // Verify empty result since the only component was invalid
    assertThat(result).isNotNull();
    assertThat(result.componentEvalResults).isEmpty();
    assertThat(componentEvaluationDataRequestList.components)
        .as("Invalid component should be filtered out")
        .isEmpty();
  }

  @Test
  public void testBatchEvaluationFiltersComponentsWithNullPathname() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequest validRequest1 =
        new RepositoryComponentEvaluationDataRequest("npm", "valid/path1.jar", "hash1");
    RepositoryComponentEvaluationDataRequest invalidRequest =
        new RepositoryComponentEvaluationDataRequest("npm", null, "badHash");
    RepositoryComponentEvaluationDataRequest validRequest2 =
        new RepositoryComponentEvaluationDataRequest("npm", "valid/path2.jar", "hash2");

    RepositoryComponentEvaluationDataRequestList requestList = new RepositoryComponentEvaluationDataRequestList();
    requestList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;
    requestList.components.add(validRequest1);
    requestList.components.add(invalidRequest);
    requestList.components.add(validRequest2);

    // Mock HDS responses for the valid components only
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("component1", "1.0.0");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("component2", "1.0.0");

    // Create a request list with only valid components for mocking
    RepositoryComponentEvaluationDataRequestList validRequestListForMocking =
        new RepositoryComponentEvaluationDataRequestList();
    validRequestListForMocking.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;
    validRequestListForMocking.components.add(validRequest1);
    validRequestListForMocking.components.add(validRequest2);

    mockHdsRequestForComponent(validRequestListForMocking,
        ImmutableMap.of(validRequest1, componentIdentifier1, validRequest2, componentIdentifier2),
        Collections.emptyList());

    RepositoryComponentEvaluationDataList result = repositoryService.evaluateComponentsAdhoc(
        REPO_MAN_INSTANCE_ID,
        REPO_PUBLIC_ID,
        requestList,
        null);

    assertThat(result).isNotNull();
    assertThat(result.componentEvalResults)
        .as("Should process only the 2 valid components")
        .hasSize(2);

    assertThat(requestList.components)
        .as("Invalid component should be filtered from request list")
        .hasSize(2)
        .extracting(req -> req.pathname)
        .containsExactlyInAnyOrder("valid/path1.jar", "valid/path2.jar");
  }

  private void mockHdsRequestForComponent(
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      Map<RepositoryComponentEvaluationDataRequest, ComponentIdentifier> evalDataToComponentIdent,
      List<SecurityVulnerability> vulnerabilities)
  {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    List<RepositoryComponentEvaluationDataRequest> components = componentEvaluationDataRequestList.components;
    for (int i = 0; i < components.size(); i++) {
      RepositoryComponentEvaluationDataRequest component = components.get(i);
      ComponentIdentifier componentIdentifier = evalDataToComponentIdent.get(component);
      ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(
          componentIdentifier, component.hash, MatchState.EXACT, i, Collections.emptySet(), Collections.emptySet(),
          vulnerabilities, 0);
      hdsResult.components.add(componentEvaluationData);
    }

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
  }

  private RepositoryComponentEvaluationDataRequestList newRepositoryComponentEvaluationDataRequestList(
      List<RepositoryComponentEvaluationDataRequest> components)
  {
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;
    componentEvaluationDataRequestList.components = components;

    return componentEvaluationDataRequestList;
  }

  @Test
  public void testEvaluateComponentsAdhoc_QuarantinedComponentReturnsQuarantineTrue() {
    Repository repo = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    // Pre-insert a quarantined component in the DB at the path we will request
    String pathname = "maven2/com/example/foo/1.0/foo-1.0.jar";
    String hash = "somehash";
    ProxyRepositoryComponent quarantinedComponent =
        tempEntity.newRepositoryComponent(repo.getId(), pathname, new Date() /* quarantineTime */, null);
    // Update hash to match what we'll send in the request
    quarantinedComponent.setHash(hash);
    proxyRepositoryComponentDAO.update(quarantinedComponent);

    RepositoryComponentEvaluationDataRequest componentRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    RepositoryComponentEvaluationDataRequestList requestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentRequest));
    requestList.quarantineEnabled = true;

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("com.example", "foo", "1.0");
    ComponentEvaluationDataList hdsResult1 = new ComponentEvaluationDataList();
    hdsResult1.components.add(createComponentEvaluationData(
        componentIdentifier, hash, MatchState.EXACT, 0, Collections.emptySet(), Collections.emptySet(),
        Collections.emptyList(), 0));
    mockHdsRequest(requestList, hdsResult1, true /* quarantineEnabled → quarantineHdsClient */);

    RepositoryComponentEvaluationDataList result =
        repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, requestList, null);

    assertThat(result.componentEvalResults).hasSize(1);
    assertThat(result.componentEvalResults.get(0).quarantine).isTrue();
  }

  @Test
  public void testEvaluateComponentsAdhoc_NonQuarantinedComponentReturnsQuarantineFalse() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequest componentRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList requestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentRequest));
    requestList.quarantineEnabled = true;

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("foobar", "1.0.0");
    ComponentEvaluationDataList hdsResult2 = new ComponentEvaluationDataList();
    hdsResult2.components.add(createComponentEvaluationData(
        componentIdentifier, "hash", MatchState.EXACT, 0, Collections.emptySet(), Collections.emptySet(),
        Collections.emptyList(), 0));
    mockHdsRequest(requestList, hdsResult2, true /* quarantineEnabled → quarantineHdsClient */);

    RepositoryComponentEvaluationDataList result =
        repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, requestList, null);

    assertThat(result.componentEvalResults).hasSize(1);
    assertThat(result.componentEvalResults.get(0).quarantine).isFalse();
  }

  @Test
  public void testEvaluateComponentsAdhoc_MissingLicenseFeature() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testGetConfiguredRepositories_ReturnsRepositoriesForManager() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).publicId).isEqualTo(REPO_PUBLIC_ID);
    assertThat(result.manager.instanceId).isEqualTo(REPO_MAN_INSTANCE_ID);
  }

  @Test
  public void testGetConfiguredRepositories_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> repositoryService.getConfiguredRepositories(
            REPO_MAN_INSTANCE_ID, null, null, null, null, null, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testGetConfiguredRepositories_FilterBySearchText() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "maven-central", null, false);
    tempEntity.newHostedRepository(repoManager, "npm-proxy", null, false);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, "maven", null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).publicId).isEqualTo("maven-central");
  }

  @Test
  public void testGetConfiguredRepositories_FilterByFormat() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "maven-central", "maven2", false);
    tempEntity.newHostedRepository(repoManager, "npm-proxy", "npm", false);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, "npm", null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).publicId).isEqualTo("npm-proxy");
  }

  @Test
  public void testGetConfiguredRepositories_SortByPublicId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "zz-repo", null, false);
    tempEntity.newHostedRepository(repoManager, "aa-repo", null, false);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, "publicId", "asc", null, null);

    assertThat(result.repositories).hasSize(2);
    assertThat(result.repositories.get(0).publicId).isEqualTo("aa-repo");
    assertThat(result.repositories.get(1).publicId).isEqualTo("zz-repo");
  }

  @Test
  public void testGetConfiguredRepositories_SortDescending() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "aa-repo", null, false);
    tempEntity.newHostedRepository(repoManager, "zz-repo", null, false);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, "publicId", "desc", null, null);

    assertThat(result.repositories).hasSize(2);
    assertThat(result.repositories.get(0).publicId).isEqualTo("zz-repo");
  }

  @Test
  public void testGetConfiguredRepositories_SortByLastScannedTime() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repoA = tempEntity.newHostedRepository(repoManager, "repo-a", null, false);
    Repository repoB = tempEntity.newHostedRepository(repoManager, "repo-b", null, false);
    Date older = new Date(1000L);
    Date newer = new Date(2000L);
    // repo-b gets older scan, repo-a gets newer — so alphabetical order (a,b) differs from scan-time order (b,a)
    createHrcWithEvaluation(repoB, older);
    createHrcWithEvaluation(repoA, newer);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, "lastscannedtime", "asc", null,
            null);

    assertThat(result.repositories).hasSize(2);
    assertThat(result.repositories.get(0).publicId).isEqualTo("repo-b");
    assertThat(result.repositories.get(1).publicId).isEqualTo("repo-a");
  }

  @Test
  public void testGetConfiguredRepositories_Pagination() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "repo-1", null, false);
    tempEntity.newHostedRepository(repoManager, "repo-2", null, false);
    tempEntity.newHostedRepository(repoManager, "repo-3", null, false);

    HostedRepositoryListDTO page1 =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, "publicId", "asc", 1, 2);
    HostedRepositoryListDTO page2 =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, "publicId", "asc", 2, 2);

    assertThat(page1.repositories).hasSize(2);
    assertThat(page2.repositories).hasSize(1);
  }

  @Test
  public void testGetConfiguredRepositories_PageBeyondResults_ReturnsEmpty() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, 99, 10);

    assertThat(result.repositories).isEmpty();
  }

  @Test
  public void testGetConfiguredRepositories_LastScannedTimePopulated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repo = tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);
    Date scanDate = new Date(5000L);
    createHrcWithEvaluation(repo, scanDate);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).lastScannedTime).isEqualTo(scanDate.getTime());
  }

  @Test
  public void testGetConfiguredRepositories_LastScannedTime_AdvancesAfterReEval() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repo = tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repo);
    Date firstScan = new Date(1_700_000_000_000L);
    Date reEval = new Date(1_800_000_000_000L);
    tempEntity.newPolicyEvaluation(hrc.getId(), "build", UUID.randomUUID().toString(), firstScan);
    tempEntity.newPolicyEvaluation(hrc.getId(), "build", UUID.randomUUID().toString(), reEval);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, null, null);

    assertThat(result.repositories.get(0).lastScannedTime).isEqualTo(reEval.getTime());
  }

  @Test
  public void testGetConfiguredRepositories_NoComponents_LastScannedTimeNull() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).lastScannedTime).isNull();
  }

  @Test
  public void testGetConfiguredRepositories_SortByLastScannedTime_DescNullAppearsLast() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repoA = tempEntity.newHostedRepository(repoManager, "repo-a", null, false);
    tempEntity.newHostedRepository(repoManager, "repo-b", null, false);
    createHrcWithEvaluation(repoA, new Date(1000L));

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, "lastscannedtime", "desc", null,
            null);

    assertThat(result.repositories).hasSize(2);
    assertThat(result.repositories.get(0).publicId).isEqualTo("repo-a");
    assertThat(result.repositories.get(1).lastScannedTime).isNull();
  }

  @Test
  public void testGetConfiguredRepositories_SortByLastScannedTime_AscNullAppearsLast() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repoA = tempEntity.newHostedRepository(repoManager, "repo-a", null, false);
    tempEntity.newHostedRepository(repoManager, "repo-b", null, false);
    createHrcWithEvaluation(repoA, new Date(1000L));

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, "lastscannedtime", "asc", null,
            null);

    assertThat(result.repositories).hasSize(2);
    assertThat(result.repositories.get(0).publicId).isEqualTo("repo-a");
    assertThat(result.repositories.get(1).lastScannedTime).isNull();
  }

  @Test
  public void testGetConfiguredRepositories_HasQueuedScans_True_WhenPendingEntryExists() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repo = tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), "some-path");
    tempEntity.newHostedComponentScanQueue(component.getId(), repo.getId(),
        HostedComponentScanQueueDAO.Status.PENDING.name());

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).hasQueuedScans).isTrue();
  }

  @Test
  public void testGetConfiguredRepositories_HasQueuedScans_True_WhenInProgressEntryExists() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repo = tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), "some-path");
    tempEntity.newHostedComponentScanQueue(component.getId(), repo.getId(),
        HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).hasQueuedScans).isTrue();
  }

  @Test
  public void testGetConfiguredRepositories_HasQueuedScans_False_WhenOnlyCompletedEntryExists() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repo = tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), "some-path");
    tempEntity.newHostedComponentScanQueue(component.getId(), repo.getId(),
        HostedComponentScanQueueDAO.Status.COMPLETED.name());

    HostedRepositoryListDTO result =
        repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID, null, null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).hasQueuedScans).isFalse();
  }

  @Test
  public void testGetAvailableFormats_ReturnsDistinctSortedFormats() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "repo-npm-1", "npm", false);
    tempEntity.newHostedRepository(repoManager, "repo-npm-2", "npm", false);
    tempEntity.newHostedRepository(repoManager, "repo-maven", "maven2", false);

    List<String> formats = repositoryService.getAvailableFormats(REPO_MAN_INSTANCE_ID);

    assertThat(formats).containsExactly("maven2", "npm");
  }

  @Test
  public void testGetAvailableFormats_ExcludesNullFormats() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, REPO_PUBLIC_ID, null, false);

    List<String> formats = repositoryService.getAvailableFormats(REPO_MAN_INSTANCE_ID);

    assertThat(formats).doesNotContainNull();
  }

  @Test
  public void testGetConfiguredRepositories_ExcludesProxyRepositories() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "hosted-repo", "maven2", false);
    tempEntity.newRepository(repoManager, "proxy-repo", "maven2");

    HostedRepositoryListDTO result = repositoryService.getConfiguredRepositories(REPO_MAN_INSTANCE_ID,
        null, null, null, null, null, null);

    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).publicId).isEqualTo("hosted-repo");
  }

  @Test
  public void testGetAvailableFormats_ExcludesProxyRepositoryFormats() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newHostedRepository(repoManager, "hosted-maven", "maven2", false);
    tempEntity.newRepository(repoManager, "proxy-npm", "npm");

    List<String> formats = repositoryService.getAvailableFormats(REPO_MAN_INSTANCE_ID);

    assertThat(formats).containsExactly("maven2");
  }

  @Test
  public void testGetAvailableFormats_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> repositoryService.getAvailableFormats(REPO_MAN_INSTANCE_ID))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testIsMalwareWaived_returnsTrueWhenActiveWaivedViolationExistsForPathname() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repoManager, REPO_PUBLIC_ID, true);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "somepath", true, "policyId",
        "Security-Malicious", null /* componentIdentifier */);

    assertThat(repositoryService.isMalwareWaived(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath")).isTrue();
  }

  @Test
  public void testIsMalwareWaived_returnsFalseWhenViolationIsNotWaived() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repoManager, REPO_PUBLIC_ID, true);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "somepath", false, "policyId",
        "Security-Malicious", null /* componentIdentifier */);

    assertThat(repositoryService.isMalwareWaived(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath")).isFalse();
  }

  @Test
  public void testIsMalwareWaived_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> repositoryService.isMalwareWaived(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath"))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testIsMalwareWaived_returnsFalseWhenRepositoryDoesNotExist() {
    assertThat(repositoryService.isMalwareWaived("nonexistent-instance", REPO_PUBLIC_ID, "somepath")).isFalse();
  }

  @Test
  public void testIsMalwareWaived_returnsFalseWhenPathnameIsNull() {
    tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    assertThat(repositoryService.isMalwareWaived(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null)).isFalse();
  }

  @Test
  public void testIsMalwareWaived_returnsFalseWhenPathnameIsEmpty() {
    tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    assertThat(repositoryService.isMalwareWaived(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "")).isFalse();
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest(List<RepositoryDTO> repositoryDTOs) {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0-01", "http://localhost:8081", repositoryDTOs);
  }

  @Override
  protected String getUserAgent() {
    return "Nexus/3.60.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)";
  }

  /**
   * Creates an HRC with a policy evaluation LastPolicyEvaluation entry for testing lastScannedTime.
   */
  private void createHrcWithEvaluation(Repository repo, Date evaluationTime) {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repo);
    tempEntity.newPolicyEvaluation(hrc.getId(), "build", UUID.randomUUID().toString(), evaluationTime);
  }
}
