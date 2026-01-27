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
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @since 1.17
 */
public class RepositoryServiceTest
    extends AbstractRepositoryServiceTest
{
  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

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
    RepositoryComponent componentRepo1ToKeep =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_1", now);
    RepositoryComponent componentRepo1ToDelete =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_2", now);
    RepositoryComponent componentRepo1ToKeepBecauseItIsNewer =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_3", new Date(now.getTime() + 1));

    Repository repository2 = tempEntity.newRepository(repoManager, "testRepoPublicId2", true);
    RepositoryComponent componentRepo2 = tempEntity.newRepositoryComponent(repository2.getId(), "pathname2_1", now);

    RepositoryComponentPathnames repositoryComponentPathnames = new RepositoryComponentPathnames();
    repositoryComponentPathnames.time = now;
    repositoryComponentPathnames.pathnames.add(componentRepo1ToKeep.getPathname());

    repositoryService.removeExtraComponents("testRepoManagerInstanceId", "testRepoPublicId1",
        repositoryComponentPathnames);

    RepositoryComponentDAO repositoryComponentDAO = this.repositoryComponentDAO;
    assertThat(repositoryComponentDAO.getById(componentRepo1ToKeep.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getById(componentRepo1ToDelete.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(componentRepo1ToKeepBecauseItIsNewer.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getById(componentRepo2.getId())).isNotNull();
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
    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repo.getId());
    assertThat(repositoryComponents).isEmpty();

    // test that the policy violation is not persisted
    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repo.getId());
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
            componentEvaluationDataRequestList, null)).withMessage("The format cannot be null or empty.");
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
            componentEvaluationDataRequestList, null)).withMessage("The hash cannot be null or empty.");
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
            componentEvaluationDataRequestList, null)).withMessage("The format cannot be null or empty.");
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
            componentEvaluationDataRequestList, null)).withMessage("The hash cannot be null or empty.");
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
  public void testEvaluateComponentsAdhoc_MissingLicenseFeature() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("npm", "foobar", "hash");
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        newRepositoryComponentEvaluationDataRequestList(Collections.singletonList(componentEvaluationDataRequest));

    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> repositoryService.evaluateComponentsAdhoc(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null)).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest(List<RepositoryDTO> repositoryDTOs) {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0-01", repositoryDTOs);
  }

  @Override
  protected String getUserAgent() {
    return "Nexus/3.60.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)";
  }
}
