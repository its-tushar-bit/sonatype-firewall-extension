/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 port of the legacy JUnit4 {@code RepositoryResourceAuditTest} (which targets {@link RepositoryResource}). Combines
 * every {@code @Test} inherited (via {@code AbstractRepositoryResourceAuditTest}) with the adhoc component-evaluation,
 * container-image evaluation, and proprietary-component-name removal {@code @Test} methods defined directly on the
 * legacy class.
 */
@IqH2Test
class IqH2RepositoryResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private static final String REPOSITORY_MANAGER_INSTANCE_ID = "repoManInsId";

  private static final String REPOSITORY_PUBLIC_ID = "repoPubId";

  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private RepositoryDAO repositoryDAO;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private com.sonatype.insight.brain.model.security.User unauthorizedUser;

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    proxyRepositoryComponentDAO = ctx.lookup(ProxyRepositoryComponentDAO.class);
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private String getResourcePath() {
    return RepositoryResource.RESOURCE_PATH;
  }

  private ConfigureRepositoriesRequest createConfigureRepositoriesRequest(RepositoryDTO repositoryDTO) {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0", "http://localhost:8081",
        Collections.singletonList(repositoryDTO));
  }

  private HttpRequest restRequest() {
    // Integration REST endpoints don't use/require CSRF
    return ctx.restRequest().noCsrfToken();
  }

  @Test
  void testSetAuditEnabled_Connect() throws Exception {
    ctx.tempEntity().newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);

    enableAuditRequest(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, null);
    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicId(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", REPOSITORY_MANAGER_INSTANCE_ID);
  }

  @Test
  void testSetAuditEnabled_Disconnect() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    enableAuditRequest(repositoryManager.getInstanceId(), repository.getPublicId(), false).post();

    repository = repositoryDAO.getById(repository.getId());
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DISCONNECT_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  void testSetAuditEnabled_Unauthorized() throws Exception {
    enableAuditRequest(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, "unauthorized");
    assertCustomData(auditDTO, "repositoryPublicId", REPOSITORY_PUBLIC_ID);
  }

  @Test
  void testEvaluateComponents_ImplicitlyEnableAudit() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).post();

    repository = repositoryDAO.getById(repository.getId());
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  void testEvaluateComponentsWithQuarantine_ImplicitlyEnableAudit() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).post();

    repository.setAuditEnabled(true);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  void testEvaluateComponents_InitialAudit() throws Exception {
    testEvaluateComponents(false, 0, RepositoryComponentEvaluationDataRequestList.INITIAL_AUDIT);
  }

  @Test
  void testEvaluateComponents_NewComponent() throws Exception {
    testEvaluateComponents(false, 1, RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);
  }

  @Test
  void testEvaluateComponents_Reevaluation() throws Exception {
    testEvaluateComponents(false, 2, RepositoryComponentEvaluationDataRequestList.REEVALUATION);
  }

  @Test
  void testEvaluateComponents_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testEvaluateComponents_NullComponentsAndCause() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList evalList = new RepositoryComponentEvaluationDataRequestList();
    evalList.components = null;
    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), evalList).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, 0, null);
  }

  @Test
  void testEvaluateComponentWithQuarantine() throws Exception {
    testEvaluateComponents(true, 2, RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);
  }

  @Test
  void testEvaluateComponentsWithQuarantine_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testEvaluateComponentsWithQuarantine_RetainSubEvent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    ctx.tempEntity().newPolicy(failProxyOnExactMatch());
    RepositoryComponentEvaluationDataRequestList repoComponentEvalList = repoComponentEvalList(1);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList).post();

    repository = repositoryDAO.getById(repository.getId());
    AuditDTO auditDTO = assertAuditLog(AuditEvent.RETAIN_QUARANTINE, null, SYSTEM_USER);
    assertRepositoryData(auditDTO, repository);
    assertComponentData(auditDTO, repoComponentEvalList.components.get(0).hash,
        repoComponentEvalList.components.get(0).pathname);
  }

  private Policy failProxyOnExactMatch() {
    Policy policy = new Policy();
    policy.setName("policy");
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(MatchStateConditionType.ID, "is", MatchState.EXACT.getId()));
    policy.addConstraint(constraint);
    policy.getActions().put(Stage.ID_PROXY, "fail");
    return policy;
  }

  @Test
  void testEvaluateComponents_QuarantinedComponent_ResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), null);
    proxyRepositoryComponent.setHash("differentHash");
    proxyRepositoryComponentDAO.update(proxyRepositoryComponent);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList(1))
        .post();

    repository = repositoryDAO.getById(repository.getId());
    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentData(auditDTO, proxyRepositoryComponent.getHash(), proxyRepositoryComponent.getPathname());
  }

  @Test
  void testEvaluateComponents_NeverQuarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), "pathname", null, null);
    proxyRepositoryComponent.setHash("differentHash");
    proxyRepositoryComponentDAO.update(proxyRepositoryComponent);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList(1))
        .post();

    assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  @Test
  void testEvaluateComponents_UnquarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), new Date());
    proxyRepositoryComponent.setHash("differentHash");
    proxyRepositoryComponentDAO.update(proxyRepositoryComponent);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList(1))
        .post();

    assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  private void testEvaluateComponents(boolean withQuarantine, int count, String cause) throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequestList repoComponentEvalList = repoComponentEvalList(count);
    repoComponentEvalList.cause = cause;

    evaluateRequest(withQuarantine, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, count, cause.replace('_', '-'));
  }

  @Test
  void testSetQuarantine_Enabled() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    quarantineRequest(repositoryManager.getInstanceId(), repository.getPublicId(), true).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "quarantine", "enabled");
  }

  @Test
  void testSetQuarantine_Disabled() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    quarantineRequest(repositoryManager.getInstanceId(), repository.getPublicId(), false).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "quarantine", "disabled");
  }

  @Test
  void testSetQuarantine_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    quarantineRequest(repositoryManager.getInstanceId(), repository.getPublicId(), true).with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testRemoveComponent_QuarantinedComponent_ResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), null);

    componentRequest(repositoryManager.getInstanceId(), repository.getPublicId(),
        proxyRepositoryComponent.getPathname())
            .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentData(auditDTO, proxyRepositoryComponent.getHash(), proxyRepositoryComponent.getPathname());
  }

  @Test
  void testRemoveComponent_NeverQuarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), "pathname", null, null);

    ctx.assertResponseStatus(204,
        componentRequest(repositoryManager.getInstanceId(), repository.getPublicId(),
            proxyRepositoryComponent.getPathname())
                .delete());

    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  @Test
  void testRemoveComponent_UnquarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), new Date());

    ctx.assertResponseStatus(204,
        componentRequest(repositoryManager.getInstanceId(), repository.getPublicId(),
            proxyRepositoryComponent.getPathname())
                .delete());

    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  @Test
  void testEvaluateComponentsWithQuarantine_ImplicitlyEnableQuarantine() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, false);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).post();

    repository = repositoryDAO.getById(repository.getId());
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "quarantine", "enabled");
  }

  @Test
  void testAddProprietaryComponentNames() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();

    restRequest().path(getResourcePath(), AbstractRepositoryResource.PROPRIETARY_NAMES_PATH)
        .parameter(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID)
        .body(new ProprietaryComponentNames("npm").addNames("name1", "name").addNamespaces("namespace1"))
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryPublicId", REPOSITORY_PUBLIC_ID);
    assertCustomData(auditDTO, "addedPatternCount", 3);
  }

  @Test
  void testAddProprietaryComponentNames_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();

    restRequest().path(getResourcePath(), AbstractRepositoryResource.PROPRIETARY_NAMES_PATH)
        .parameter(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID)
        .body(new ProprietaryComponentNames("npm").addNames("name1", "name").addNamespaces("namespace1"))
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES, "unauthorized");
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryPublicId", REPOSITORY_PUBLIC_ID);
  }

  @Test
  void testConfigureRepositories_NewRepository() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();

    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = "testRepoName";
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.proxy;
    repositoryDTO.auditEnabled = true;
    repositoryDTO.quarantineEnabled = true;
    repositoryDTO.policyCompliantComponentSelectionEnabled = true;
    repositoryDTO.namespaceConfusionProtectionEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest = createConfigureRepositoriesRequest(repositoryDTO);

    restRequest().path(getResourcePath(), AbstractRepositoryResource.CONFIGURE_REPOSITORIES_PATH)
        .parameter(repositoryManager.getInstanceId())
        .body(configureRepositoriesRequest)
        .post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CONFIGURE_REPOSITORY, 2, null /* error */);
    for (AuditDTO auditDTO : auditDTOs) {
      assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
      if (auditDTO.data.size() > 1) {
        Repository repository = repositoryDAO
            .getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(), "testRepoName");
        assertRepositoryData(auditDTO, repository);
      }
    }
    assertThat(auditDTOs).hasSize(2);
  }

  @Test
  void testConfigureRepositories_ExistingRepository() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "testRepoName");

    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = "testRepoName";
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.proxy;
    repositoryDTO.auditEnabled = true;
    repositoryDTO.quarantineEnabled = true;
    repositoryDTO.policyCompliantComponentSelectionEnabled = true;
    repositoryDTO.namespaceConfusionProtectionEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest = createConfigureRepositoriesRequest(repositoryDTO);

    restRequest().path(getResourcePath(), AbstractRepositoryResource.CONFIGURE_REPOSITORIES_PATH)
        .parameter(repositoryManager.getInstanceId())
        .body(configureRepositoriesRequest)
        .post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CONFIGURE_REPOSITORY, 2, null /* error */);
    for (AuditDTO auditDTO : auditDTOs) {
      assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
      if (auditDTO.data.size() > 1) {
        repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
            "testRepoName");
        assertRepositoryData(auditDTO, repository);
      }
    }
    assertThat(auditDTOs).hasSize(2);
  }

  @Test
  void testConfigureRepositories_ExistingRepositoryWrongType() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository =
        ctx.tempEntity().newRepository(repositoryManager, "testRepoName", ComponentIdentifier.FORMAT_NPM);

    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = repository.getName();
    repositoryDTO.format = repository.getFormat();
    repositoryDTO.type = RepositoryType.hosted;
    repositoryDTO.auditEnabled = repository.isAuditEnabled();
    repositoryDTO.quarantineEnabled = repository.isQuarantineEnabled();
    repositoryDTO.policyCompliantComponentSelectionEnabled = repository.isPolicyCompliantComponentSelectionEnabled();
    repositoryDTO.namespaceConfusionProtectionEnabled = repository.isNamespaceConfusionProtectionEnabled();
    ConfigureRepositoriesRequest configureRepositoriesRequest = createConfigureRepositoriesRequest(repositoryDTO);

    restRequest().path(getResourcePath(), AbstractRepositoryResource.CONFIGURE_REPOSITORIES_PATH)
        .parameter(repositoryManager.getInstanceId())
        .body(configureRepositoriesRequest)
        .post();

    List<AuditDTO> auditDTOs = getLogEntries(AuditEvent.CONFIGURE_REPOSITORY);
    for (AuditDTO auditDTO : auditDTOs) {
      assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
      if (auditDTO.data.size() == 1) {
        assertStandardData(auditDTO, AuditEvent.CONFIGURE_REPOSITORY, null /* error */);
      }
      else {
        assertStandardData(auditDTO, AuditEvent.CONFIGURE_REPOSITORY, "Error updating repository "
            + repository.getPublicId() + " (" + repository.getId() + "): Cannot change the repository type.");
        repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
            "testRepoName");
        repository.setRepositoryType(repositoryDTO.type);
        assertRepositoryData(auditDTO, repository);
      }
    }
    assertThat(auditDTOs).hasSize(2);
  }

  @Test
  void testConfigureRepositories_ExistingRepositoryWrongFormat() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "testRepoName");
    repository.setFormat("maven2");
    repositoryDAO.update(repository);

    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = "testRepoName";
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.proxy;
    repositoryDTO.auditEnabled = true;
    repositoryDTO.quarantineEnabled = false;
    repositoryDTO.policyCompliantComponentSelectionEnabled = false;
    repositoryDTO.namespaceConfusionProtectionEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest = createConfigureRepositoriesRequest(repositoryDTO);

    restRequest().path(getResourcePath(), AbstractRepositoryResource.CONFIGURE_REPOSITORIES_PATH)
        .parameter(repositoryManager.getInstanceId())
        .body(configureRepositoriesRequest)
        .post();

    List<AuditDTO> auditDTOs = getLogEntries(AuditEvent.CONFIGURE_REPOSITORY);
    for (AuditDTO auditDTO : auditDTOs) {
      assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
      if (auditDTO.data.size() == 1) {
        assertStandardData(auditDTO, AuditEvent.CONFIGURE_REPOSITORY, null /* error */);
      }
      else {
        assertStandardData(auditDTO, AuditEvent.CONFIGURE_REPOSITORY, "Error updating repository "
            + repository.getPublicId() + " (" + repository.getId() + "): Cannot change the repository format.");
        repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
            "testRepoName");
        repository.setFormat(repositoryDTO.format);
        assertRepositoryData(auditDTO, repository);
      }
    }
    assertThat(auditDTOs).hasSize(2);
  }

  @Test
  void testRemoveRepository() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "testRepoMaven", "maven2");

    restRequest().path(getResourcePath(), AbstractRepositoryResource.REPOSITORY_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_REPOSITORY, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryPublicId", repository.getPublicId());
  }

  @Test
  void testConfigureRepositories_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();

    restRequest().path(getResourcePath(), AbstractRepositoryResource.CONFIGURE_REPOSITORIES_PATH)
        .parameter(repositoryManager.getInstanceId())
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY, "unauthorized");
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  void testEvaluateComponentsAdhoc_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    evaluateAdhocRequest(new RepositoryComponentEvaluationDataRequestList()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testEvaluateComponentsAdhoc_OneComponent() throws Exception {
    testEvaluateComponentsAdhoc(1);
  }

  @Test
  void testEvaluateComponentsAdhoc_TwoComponents() throws Exception {
    testEvaluateComponentsAdhoc(2);
  }

  @Test
  void testEvaluateComponentsAdhoc_NoComponents() throws Exception {
    testEvaluateComponentsAdhoc(0);
  }

  private HttpRequest evaluateAdhocRequest(
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    return restRequest()
        .path(getResourcePath())
        .path(RepositoryResource.EVALUATE_COMPONENTS_ADHOC_PATH)
        .parameter(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID)
        .body(componentEvaluationDataRequestList);
  }

  private void testEvaluateComponentsAdhoc(int count) throws Exception {
    Repository repository = ctx.tempEntity().newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequestList repoComponentEvalList = repoComponentEvalList(count);
    repoComponentEvalList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;

    evaluateAdhocRequest(repoComponentEvalList).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, count, RepositoryComponentEvaluationDataRequestList.ADHOC);
  }

  @Test
  void testRemoveProprietaryComponentNames() throws Exception {
    restRequest().path(getResourcePath(), AbstractRepositoryResource.PROPRIETARY_NAMES_PATH)
        .parameter(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", REPOSITORY_MANAGER_INSTANCE_ID);
    assertCustomData(auditDTO, "repositoryPublicId", REPOSITORY_PUBLIC_ID);
  }

  @Test
  void testRemoveProprietaryComponentNames_Unauthorized() throws Exception {
    RepositoryManager repoManager = ctx.tempEntity().newRepositoryManager();
    Repository repo = ctx.tempEntity().newHostedRepository(repoManager, "testPublicId", "npm", true);
    restRequest().path(getResourcePath(), AbstractRepositoryResource.PROPRIETARY_NAMES_PATH)
        .parameter(repoManager.getInstanceId(), repo.getPublicId())
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES, "unauthorized");
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repoManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryPublicId", repo.getPublicId());
  }

  @Test
  void testEvaluateContainerImage() throws Exception {
    Repository repository = ctx.tempEntity().newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    String bomJson = buildTestBom();

    ctx.mockReport("SCAN-ID", "/RepositoryResourceAuditTest/report");

    evaluateContainerImageRequest(bomJson).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testEvaluateContainerImage_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    String bomJson = buildTestBom();

    evaluateContainerImageRequest(bomJson).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest evaluateContainerImageRequest(String bomJson) {
    return restRequest()
        .path(getResourcePath())
        .path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID)
        .body(bomJson);
  }

  private String buildTestBom() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();
    bom.setMetadata(metadata);

    Component containerImageComponent = new Component();
    containerImageComponent.setType(Type.CONTAINER);
    containerImageComponent.setPurl(PackageUrlIdentifier.toPackageUrl(
        ComponentIdentifier.createContainerCoordinates("test-namespace", "test-image", "1.0.0")));
    metadata.setComponent(containerImageComponent);

    Property baseUrlProperty = new Property();
    baseUrlProperty.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    baseUrlProperty.setValue("https://test.repository-manager.com");
    metadata.addProperty(baseUrlProperty);

    try {
      return BomGeneratorFactory.createJson(Version.VERSION_16, bom).toJsonString();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private HttpRequest enableAuditRequest(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      boolean auditEnabled)
  {
    return restRequest().path(getResourcePath(), AbstractRepositoryResource.AUDIT_ENABLE_PATH)
        .parameter(repositoryManagerInstanceId, repositoryPublicId, auditEnabled);
  }

  private HttpRequest evaluateRequest(
      boolean withQuarantine,
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList repoComponentEvalList)
  {
    return restRequest().path(getResourcePath(),
        withQuarantine
            ? AbstractRepositoryResource.EVALUATE_COMPONENTS_WITH_QUARANTINE_PATH
            : AbstractRepositoryResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repositoryManagerInstanceId, repositoryPublicId)
        .body(repoComponentEvalList);
  }

  private HttpRequest quarantineRequest(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      boolean enabled)
  {
    return restRequest().path(getResourcePath(), AbstractRepositoryResource.QUARANTINE_PATH)
        .parameter(repositoryManagerInstanceId, repositoryPublicId, enabled);
  }

  private HttpRequest componentRequest(String repositoryManagerInstanceId, String repositoryPublicId, String pathname) {
    return restRequest().path(getResourcePath(), AbstractRepositoryResource.COMPONENTS_PATH)
        .parameter(repositoryManagerInstanceId, repositoryPublicId, pathname);
  }

  private void assertRepositoryEvaluationData(AuditDTO auditDTO, int componentCount, String evaluationCause) {
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "evaluationCause", evaluationCause);
  }

  private RepositoryComponentEvaluationDataRequestList repoComponentEvalList(int componentCount) {
    RepositoryComponentEvaluationDataRequestList evalList = new RepositoryComponentEvaluationDataRequestList();
    for (int i = 0; i < componentCount; i++) {
      RepositoryComponentEvaluationDataRequest evalRequest = new RepositoryComponentEvaluationDataRequest();
      evalRequest.format = "format";
      evalRequest.pathname = "pathname";
      evalRequest.hash = "hash";
      evalList.components.add(evalRequest);
    }
    mockHdsResponse(componentCount);
    return evalList;
  }

  private void mockHdsResponse(int componentCount) {
    ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
    componentEvaluationDataList.components = new ArrayList<>();
    for (int i = 0; i < componentCount; i++) {
      ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
      componentEvaluationData.requestIndex = i;
      componentEvaluationData.hash = "hash";
      componentEvaluationData.matchState = MatchState.EXACT.getId();
      componentEvaluationData.declaredLicenses = Collections.emptySet();
      componentEvaluationData.observedLicenses = Collections.emptySet();
      componentEvaluationDataList.components.add(componentEvaluationData);
    }
    ctx.hdsRespondWith(componentEvaluationDataList).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }

  private void assertComponentData(AuditDTO auditDTO, String componentHash, String componentPathname) {
    assertCustomData(auditDTO, "componentHash", componentHash);
    assertCustomData(auditDTO, "componentPathname", componentPathname);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
