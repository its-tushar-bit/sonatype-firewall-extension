/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.utils.FirewallForContainerImagesHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyAlertUtilTest
    extends AbstractDataTest
{
  private PolicyDAO policyDAO;

  private PolicyViolationDAO policyViolationDAO;

  private PolicyAlertUtil policyAlertUtil;

  private ReportComponentService reportComponentService;

  private OrganizationDAO organizationDAO;

  @BeforeEach
  public void setUp() {
    reportComponentService = Mockito.mock(ReportComponentService.class);
    OwnerDAO ownerDAO = daoFactory.createOwnerDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
    RepositoryDAO repositoryDAO = daoFactory.createRepositoryDAO();
    policyDAO = daoFactory.createPolicyDAO();
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
    FirewallForContainerImagesHelper firewallForContainerImagesHelper =
        new FirewallForContainerImagesHelper(organizationDAO, repositoryDAO, ownerDAO);
    policyAlertUtil = new PolicyAlertUtil(ownerDAO, policyDAO, policyViolationDAO, reportComponentService,
        firewallForContainerImagesHelper);
  }

  @Test
  public void testCreatePolicyAlerts_DeletedPolicy() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policyDoesNotExist = tempEntity.newPolicy();
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policyDoesNotExist);
    policyDAO.delete(policyDoesNotExist);
    List<PolicyAlert> alerts = policyAlertUtil.createPolicyAlerts(Collections.singletonList(policyViolation),
        policyEval.getStageTypeId(), app.getId(), policyEval.isForMonitoring(), true);
    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getTrigger().getPolicyId()).isEqualTo(policyDoesNotExist.getId());
    assertThat(alert.getTrigger().getPolicyName()).isEqualTo(policyDoesNotExist.getName());
    assertThat(alert.getActions()).isEmpty();
  }

  @Test
  public void testCreatePolicyAlerts_NoUnnecessaryData() throws IOException {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);
    ConditionFact conditionFact0 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "some summary", "some reason");
    conditionFact0.setTriggerJson("trigger 0");
    ConditionFact conditionFact1 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        1 /* conditionIndex */, "some summary", "some reason");
    conditionFact1.setTriggerJson("trigger 1");
    ConstraintFact constraintFact = new ConstraintFact("someConstraintId", "some constraint name", "and");
    constraintFact.addConditionFact(conditionFact0);
    constraintFact.addConditionFact(conditionFact1);
    policyViolation.setConstraintFacts(Collections.singletonList(constraintFact));

    Component component = new Component();
    component.setHash("hash");
    component.addPathname("a.jar");
    component.addPathname("path/b.jar");
    Mockito.when(reportComponentService.getReportComponents(Mockito.eq("scanId"), Mockito.any()))
        .thenReturn(Arrays.asList(component));

    List<PolicyAlert> alerts =
        policyAlertUtil.createPolicyAlerts(new ArrayList<>(), Collections.singletonList(policyViolation),
            policyEval.getStageTypeId(), app.getId(), policyEval.isForMonitoring(), true, "scanId");

    assertThat(alerts).hasSize(1);

    PolicyAlert alert = alerts.get(0);
    List<ComponentFact> componentFacts = alert.getTrigger().getComponentFacts();
    assertThat(componentFacts).hasSize(1);
    assertThat(componentFacts.get(0).getPathnames()).containsExactlyInAnyOrder("a.jar", "path/b.jar");

    List<ConstraintFact> constraintFacts = componentFacts.get(0).getConstraintFacts();
    assertThat(constraintFacts).hasSize(1);
    constraintFact = constraintFacts.get(0);

    List<ConditionFact> conditionFacts = constraintFact.getConditionFacts();
    assertThat(conditionFacts).hasSize(2);
    // The condition index and triggers should not be populated in policy alerts.
    for (ConditionFact conditionFact : conditionFacts) {
      assertThat(conditionFact.getConditionIndex()).isEqualTo(0);
      assertThat(conditionFact.getTriggerJson()).isNull();
    }
  }

  @Test
  public void testCreatePolicyAlerts_OnePolicyAlertForEachPolicyViolation() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    String reason1 = "test reason1";
    String reason2 = "test reason2";
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEval, policy, componentIdentifier, hash,
        reason1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEval, policy, componentIdentifier, hash,
        reason2);

    List<PolicyAlert> alerts = policyAlertUtil.createPolicyAlerts(Arrays.asList(policyViolation1, policyViolation2),
        policyEval.getStageTypeId(), app.getId(), policyEval.isForMonitoring(), true);

    assertThat(alerts).hasSize(2);

    PolicyAlert alert1 = alerts.get(0);
    assertThat(alert1.getTrigger().getPolicyId()).isEqualTo(policy.getId());
    assertThat(alert1.getTrigger().getPolicyName()).isEqualTo(policy.getName());
    assertThat(alert1.getTrigger().getPolicyViolationId()).isEqualTo(policyViolation1.getId());
    assertThat(alert1.getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason()).isEqualTo(reason1);
    assertThat(alert1.getActions()).isEmpty();

    PolicyAlert alert2 = alerts.get(1);
    assertThat(alert2.getTrigger().getPolicyId()).isEqualTo(policy.getId());
    assertThat(alert2.getTrigger().getPolicyName()).isEqualTo(policy.getName());
    assertThat(alert2.getTrigger().getPolicyViolationId()).isEqualTo(policyViolation2.getId());
    assertThat(alert2.getTrigger()
        .getComponentFacts()
        .get(0)
        .getConstraintFacts()
        .get(0)
        .getConditionFacts()
        .get(0)
        .getReason()).isEqualTo(reason2);
    assertThat(alert2.getActions()).isEmpty();
  }

  @Test
  public void testCreatePolicyAlerts_ActionsEnabled() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policyDAO.update(policy);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);
    ConditionFact conditionFact0 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "some summary", "some reason");
    conditionFact0.setTriggerJson("trigger 0");
    ConstraintFact constraintFact = new ConstraintFact("someConstraintId", "some constraint name", "and");
    constraintFact.addConditionFact(conditionFact0);
    policyViolation.setConstraintFacts(Collections.singletonList(constraintFact));

    List<PolicyAlert> alerts = policyAlertUtil.createPolicyAlerts(Collections.singletonList(policyViolation),
        policyEval.getStageTypeId(), app.getId(), policyEval.isForMonitoring(), true);

    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getActions()).hasSize(1);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }

  @Test
  public void testCreatePolicyAlerts_ActionsDisabled() {
    Application app = tempEntity.newApplicationWithParent("app-id");
    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policyDAO.update(policy);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "some-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);
    ConditionFact conditionFact0 = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
        0 /* conditionIndex */, "some summary", "some reason");
    conditionFact0.setTriggerJson("trigger 0");
    ConstraintFact constraintFact = new ConstraintFact("someConstraintId", "some constraint name", "and");
    constraintFact.addConditionFact(conditionFact0);
    policyViolation.setConstraintFacts(Collections.singletonList(constraintFact));

    List<PolicyAlert> alerts = policyAlertUtil.createPolicyAlerts(Collections.singletonList(policyViolation),
        policyEval.getStageTypeId(), app.getId(), policyEval.isForMonitoring(), false);

    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getActions()).isEmpty();
  }

  @Test
  public void testCreatePolicyAlerts_Pathnames_ByHash() {
    Application application = tempEntity.newApplicationWithParent();
    String stageTypeId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    Component component = new Component();
    component.setHash("hash");
    component.addPathname("a.jar");
    component.addPathname("path/b.jar");

    List<PolicyAlert> policyAlerts = policyAlertUtil.createPolicyAlerts(
        Collections.singletonList(component),
        Collections.singletonList(policyViolation),
        stageTypeId,
        application.getId(),
        policyEvaluation.isForMonitoring(),
        false);

    assertThat(policyAlerts).hasSize(1);
    PolicyAlert policyAlert = policyAlerts.get(0);
    assertThat(policyAlert).isNotNull();
    PolicyFact policyFact = policyAlert.getTrigger();
    assertThat(policyFact).isNotNull();
    List<ComponentFact> componentFacts = policyFact.getComponentFacts();
    assertThat(componentFacts).hasSize(1);
    ComponentFact componentFact = componentFacts.get(0);
    assertThat(componentFact).isNotNull();
    assertThat(componentFact.getPathnames()).isEqualTo(component.getPathnames());
  }

  @Test
  public void testCreatePolicyAlerts_Pathnames_ByHash_NoResult() {
    Application application = tempEntity.newApplicationWithParent();
    String stageTypeId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    Component component = new Component();
    component.setHash("otherHash");
    component.addPathname("a.jar");
    component.addPathname("path/b.jar");

    List<PolicyAlert> policyAlerts = policyAlertUtil.createPolicyAlerts(
        Collections.singletonList(component),
        Collections.singletonList(policyViolation),
        stageTypeId,
        application.getId(),
        policyEvaluation.isForMonitoring(),
        false);

    assertThat(policyAlerts).hasSize(1);
    PolicyAlert policyAlert = policyAlerts.get(0);
    assertThat(policyAlert).isNotNull();
    PolicyFact policyFact = policyAlert.getTrigger();
    assertThat(policyFact).isNotNull();
    List<ComponentFact> componentFacts = policyFact.getComponentFacts();
    assertThat(componentFacts).hasSize(1);
    ComponentFact componentFact = componentFacts.get(0);
    assertThat(componentFact).isNotNull();
    assertThat(componentFact.getPathnames()).isEmpty();
  }

  @Test
  public void testCreatePolicyAlerts_Pathnames_ByComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    String stageTypeId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setHash(null);
    policyViolationDAO.update(policyViolation);
    Component component1 = new Component();
    component1.setComponentIdentifier(policyViolation.getComponentIdentifier());
    component1.addPathname("a.jar");
    component1.addPathname("path/b.jar");
    Component component2 = new Component();
    component2.setComponentIdentifier(policyViolation.getComponentIdentifier());
    component2.addPathname("path/b.jar");
    component2.addPathname("other/path/c.jar");

    List<PolicyAlert> policyAlerts = policyAlertUtil.createPolicyAlerts(
        Arrays.asList(component1, component2),
        Collections.singletonList(policyViolation),
        stageTypeId,
        application.getId(),
        policyEvaluation.isForMonitoring(),
        false);

    assertThat(policyAlerts).hasSize(1);
    PolicyAlert policyAlert = policyAlerts.get(0);
    assertThat(policyAlert).isNotNull();
    PolicyFact policyFact = policyAlert.getTrigger();
    assertThat(policyFact).isNotNull();
    List<ComponentFact> componentFacts = policyFact.getComponentFacts();
    assertThat(componentFacts).hasSize(1);
    ComponentFact componentFact = componentFacts.get(0);
    assertThat(componentFact).isNotNull();
    assertThat(componentFact.getPathnames()).isEqualTo(
        Stream.concat(component1.getPathnames().stream(), component2.getPathnames().stream())
            .distinct()
            .sorted()
            .collect(Collectors.toList()));
  }

  @Test
  public void testCreatePolicyAlerts_Pathnames_ByComponentIdentifier_NoResult() {
    Application application = tempEntity.newApplicationWithParent();
    String stageTypeId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setHash(null);
    policyViolationDAO.update(policyViolation);
    Component component = new Component();
    component.setComponentIdentifier(policyViolation.getComponentIdentifier().createAlternativeVersion("v2"));
    component.addPathname("a.jar");
    component.addPathname("path/b.jar");

    List<PolicyAlert> policyAlerts = policyAlertUtil.createPolicyAlerts(
        Collections.singletonList(component),
        Collections.singletonList(policyViolation),
        stageTypeId,
        application.getId(),
        policyEvaluation.isForMonitoring(),
        false);

    assertThat(policyAlerts).hasSize(1);
    PolicyAlert policyAlert = policyAlerts.get(0);
    assertThat(policyAlert).isNotNull();
    PolicyFact policyFact = policyAlert.getTrigger();
    assertThat(policyFact).isNotNull();
    List<ComponentFact> componentFacts = policyFact.getComponentFacts();
    assertThat(componentFacts).hasSize(1);
    ComponentFact componentFact = componentFacts.get(0);
    assertThat(componentFact).isNotNull();
    assertThat(componentFact.getPathnames()).isEmpty();
  }

  @Test
  public void testCreatePolicyAlerts_ActionsFromApplicationHierarchyWithoutRelatedRepository() {
    Organization organization = tempEntity.newOrganization("org-without-repo");
    Application app = tempEntity.newApplicationWithParent(organization);

    Policy policy = tempEntity.newPolicy(organization);
    policy.setAction(Stage.ID_PROXY, Action.ID_WARN);
    policyDAO.update(policy);

    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_PROXY, "proxy-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, policy);

    List<PolicyAlert> alerts = policyAlertUtil.createPolicyAlerts(Collections.singletonList(policyViolation),
        policyEval.getStageTypeId(), app.getId(), policyEval.isForMonitoring(), true);

    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getActions()).hasSize(1);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_WARN);
  }

  @Test
  public void testCreatePolicyAlerts_ActionsFromRelatedRepositoryHierarchy() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository dockerProxyRepo =
        tempEntity.newRepository(repositoryManager, "docker-proxy-repo", RepositoryType.proxy, "docker");

    Organization orgWithRepo = tempEntity.newOrganization("org-with-docker-repo");
    orgWithRepo.setRelatedRepositoryId(dockerProxyRepo.getId());
    organizationDAO.update(orgWithRepo);

    Application app = tempEntity.newApplicationWithParent(orgWithRepo);

    Policy repositoryPolicy = tempEntity.newPolicy(repositoryManager);
    repositoryPolicy.setAction(Stage.ID_PROXY, Action.ID_FAIL);
    policyDAO.update(repositoryPolicy);

    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_PROXY, "proxy-scan");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEval, repositoryPolicy);

    List<PolicyAlert> alerts = policyAlertUtil.createPolicyAlerts(Collections.singletonList(policyViolation),
        policyEval.getStageTypeId(), app.getId(), policyEval.isForMonitoring(), true);

    assertThat(alerts).hasSize(1);
    PolicyAlert alert = alerts.get(0);
    assertThat(alert.getActions()).hasSize(1);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }
}
