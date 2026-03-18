/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDTO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

@Named
public class RepositoryService
{
  public static final int MAX_REPOSITORY_EVALUATION_REQUEST_SIZE = 100;

  private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private final PolicyDAO policyDAO;

  private final OwnerDAO ownerDAO;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  private final OrganizationService organizationService;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public RepositoryService(
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      RepositoryManagerDAO repositoryManagerDAO,
      RepositoryDAO repositoryDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO,
      PolicyDAO policyDAO,
      OwnerDAO ownerDAO,
      OrganizationService organizationService,
      final ClusterLockManager clusterLockManager)
  {
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.proprietaryComponentNameDetector = proprietaryComponentNameDetector;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.proprietaryComponentNamePatternDAO = proprietaryComponentNamePatternDAO;
    this.policyDAO = policyDAO;
    this.ownerDAO = ownerDAO;
    this.organizationService = organizationService;
    this.clusterLockManager = clusterLockManager;
  }

  /**
   * @since 1.19.0
   */
  @Authorize(permission = Permission.WRITE)
  void unquarantineComponent(
      @AuthzContext(Key.REPOSITORY_ID) final String repositoryId,
      final String pathname,
      final String clientUserAgent)
  {
    auditComponentPath(pathname);
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repositoryId,
        pathname);
    if (repositoryComponent == null) {
      throw new NotFoundException("Cannot find a component with path " + pathname + " in repository with ID "
          + repositoryId + ".");
    }
    AuditData.get().setComponentHash(repositoryComponent.getHash());

    if (!repositoryComponent.isQuarantined()) {
      throw new BadRequestException(
          "Component " + pathname + " in repository " + repositoryId + " is not quarantined.");
    }

    // Part of the policy evaluation, the component is unquarantined if it doesn't have any policy violations that
    // require quarantine.
    Repository repository = repositoryDAO.getById(repositoryComponent.getRepositoryId());
    RepositoryComponentEvaluationDataRequestList componentRequestList =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    RepositoryComponentEvaluationDataRequest componentRequest = new RepositoryComponentEvaluationDataRequest();
    componentRequest.format = repository.getFormat();
    componentRequest.pathname = repositoryComponent.getPathname();
    componentRequest.hash = repositoryComponent.getHash();
    componentRequestList.components.add(componentRequest);

    RepositoryComponentEvaluationDataList evaluationDataList = repositoryPolicyEvaluator.evaluate(repository,
        componentRequestList, false /* withQuarantine */, clientUserAgent);

    if (evaluationDataList.componentEvalResults.get(0).quarantine) {
      throw new BadRequestException("Component " + pathname + " in repository " + repositoryId
          + " has policy violations.");
    }
  }

  private void auditComponentPath(final String pathname) {
    AuditData.get().setData("componentPathname", pathname);
  }

  @Authorize(permission = Permission.READ)
  RepositorySummary getRepositorySummary(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    long start = System.currentTimeMillis();

    Repository repository = repositoryDAO.getById(repositoryId);
    log.debug("Get summary for repository {}:{} ({})", repository.getRepositoryManagerId(),
        repository.getPublicId(), repositoryId);

    RepositorySummary summary = new RepositorySummary();
    summary.knownComponentCount = repositoryComponentDAO.getKnownComponentCountByRepositoryId(repository.getId());
    summary.totalComponentCount = repositoryComponentDAO.getComponentCountByRepositoryId(repository.getId());

    Map<Integer, Integer> policyViolationCountsByThreatLevel =
        repositoryPolicyViolationDAO.getCountsByPolicyThreatLevel(repositoryId);
    for (int policyThreatLevel : policyViolationCountsByThreatLevel.keySet()) {
      int policyViolationCount = policyViolationCountsByThreatLevel.get(policyThreatLevel);
      if (policyThreatLevel >= 8) {
        summary.criticalViolationCount += policyViolationCount;
      }
      else if (policyThreatLevel >= 4) {
        summary.severeViolationCount += policyViolationCount;
      }
      else if (policyThreatLevel >= 2) {
        summary.moderateViolationCount += policyViolationCount;
      }
    }

    summary.affectedComponentCount =
        repositoryComponentDAO.getCountWithPolicyViolationInPolicyThreatLevelRange(repositoryId, 2, 10);
    summary.quarantinedComponentCount = repository.isQuarantineEnabled()
        ? repositoryComponentDAO.getQuarantinedComponentCountByRepositoryId(repositoryId)
        : 0;

    log.debug("Got summary for repository {}:{} ({}) in {} ms", repository.getRepositoryManagerId(),
        repository.getPublicId(), repositoryId, System.currentTimeMillis() - start);

    return summary;
  }

  /**
   * @since 1.18.0
   */
  @Authorize(permission = Permission.READ)
  public RepositoryDTO getRepositoryById(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    RepositoryDTO repositoryDTO = convertRepositories(List.of(repositoryDAO.getByIdNotNull(repositoryId))).get(0);
    Date evaluationTime = repositoryComponentDAO.getOldestComponentEvaluationTimeByRepositoryId(repositoryId);
    if (evaluationTime != null) {
      repositoryDTO.oldestEvalTimestamp = evaluationTime.getTime();
    }
    return repositoryDTO;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void reevaluateRepository(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    // Don't add this executor to ShutdownHandler,
    // a repository re-evaluation can take a long time e.g., hours,
    // and so we don't want to wait for each repository re-evaluation to finish before shutting down the server
    ThreadPoolExecutor reevalExecutor = createReevaluationExecutor();
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    AuditData.get()
        .continueAsync(reevalExecutor,
            new RepositoryReevaluationTask(repository, repositoryPolicyEvaluator, reevalExecutor,
                MAX_REPOSITORY_EVALUATION_REQUEST_SIZE, repositoryComponentDAO, clusterLockManager));
  }

  private static ThreadPoolExecutor createReevaluationExecutor() {
    ThreadPoolExecutor executor =
        new TenantThreadPoolExecutor(20, 20, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ReevaluateRepository-%s").build(),
            new AbortPolicy(), "reevaluate_repository", "RepositoryService");
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  public void delete(Repository repository) {
    if (repository.getRelatedOrganizationId() != null) {
      try {
        organizationService.deleteOrganizationNoAuthz(repository.getRelatedOrganizationId());
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    repositoryDAO.delete(repository);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteRepository(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    if (repository.getRelatedOrganizationId() != null) {
      try {
        organizationService.deleteOrganizationNoAuthz(repository.getRelatedOrganizationId());
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    repositoryDAO.delete(repository);
    AuditData.get()
        .setData("repositoryManagerInstanceId",
            repositoryManagerDAO.getById(repository.getRepositoryManagerId()).getInstanceId());

    if (repository.isAuditEnabled()) {
      policyViolationLoggerFactory.newLogger(new Date(), repository).logClearEvent();
    }
  }

  public RepositoriesDTO getRepositories() {
    List<Repository> repositories = getRepositoriesWithReadPermission();
    if (repositories.isEmpty()) {
      return new RepositoriesDTO();
    }
    return new RepositoriesDTO(convertRepositories(repositories));
  }

  public List<Repository> getRepositoriesWithReadPermissionByIds(final Set<String> repositoryIds) {
    List<Repository> repositories = getRepositoriesWithReadPermission();

    if (CollectionUtils.isEmpty(repositoryIds)) {
      return repositories;
    }

    return repositories
        .stream()
        .filter(repository -> repositoryIds.contains(repository.getId()))
        .toList();
  }

  public List<Repository> getRepositoriesWithReadPermission() {
    return filterRepositoriesWithReadPermission(repositoryDAO.getAll());
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY)
  List<Repository> filterRepositoriesWithReadPermission(List<Repository> repositories) {
    return repositories;
  }

  private List<RepositoryDTO> convertRepositories(List<Repository> repositories) {
    Set<String> repositoryManagerIds = repositories.stream()
        .map(Repository::getRepositoryManagerId)
        .collect(Collectors.toSet());

    Map<String, RepositoryManager> repositoryManagerMap = repositoryManagerDAO.getByIds(repositoryManagerIds)
        .stream()
        .collect(Collectors.toMap(RepositoryManager::getId, Function.identity(), (existing, replacement) -> existing));

    List<RepositoryDTO> repositoryDTOs = new ArrayList<>(repositories.size());

    for (Repository repository : repositories) {
      RepositoryDTO repositoryDTO = new RepositoryDTO();
      repositoryDTO.repository = repository;

      RepositoryManager repositoryManager = repositoryManagerMap.get(repository.getRepositoryManagerId());
      repositoryDTO.managerInstanceId = repositoryManager.getInstanceId();
      repositoryDTO.managerName = repositoryManager.getName();

      repositoryDTOs.add(repositoryDTO);
    }

    return repositoryDTOs;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void reevaluateComponent(
      @AuthzContext(Key.REPOSITORY_ID) String repositoryId,
      String hash,
      final String clientUserAgent)
  {
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    List<RepositoryComponent> components = repositoryComponentDAO.getByRepositoryIdAndHash(repository.getId(), hash);
    AuditData.get()
        .setData("componentCount", components.size())
        .setData("evaluationCause", RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    if (components.isEmpty()) {
      throw new NotFoundException("Cannot find a repository component for hash " + hash + " in "
          + repository.getPublicId() + ".");
    }

    RepositoryComponentEvaluationDataRequestList request = new RepositoryComponentEvaluationDataRequestList(
        RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    for (RepositoryComponent component : components) {
      request.components.add(new RepositoryComponentEvaluationDataRequest(repository.getFormat(), component
          .getPathname(), component.getHash()));
    }

    repositoryPolicyEvaluator.evaluate(repository, request, false /* withQuarantine */, clientUserAgent);
  }

  /**
   * Used by the web UI to display various timestamps related to policy evaluations. The UI calls this method for
   * component versions for which it only has a component identifier (no hash or pathname).
   *
   * @since 1.139
   */
  @Authorize(permission = Permission.READ)
  PolicyEvaluationTimestampsDTO getPolicyEvaluationTimestamps(
      @AuthzContext(Key.REPOSITORY_ID) String repositoryId,
      ComponentIdentifier componentIdentifier)
  {
    PolicyEvaluationTimestampsDTO policyEvaluationTimestampsDTO = new PolicyEvaluationTimestampsDTO();

    List<RepositoryComponent> repositoryComponents =
        repositoryComponentDAO.getByRepositoryIdAndComponentIdentifier(repositoryId, componentIdentifier);

    if (repositoryComponents.isEmpty()) {
      return policyEvaluationTimestampsDTO;
    }

    for (RepositoryComponent repositoryComponent : repositoryComponents) {
      policyEvaluationTimestampsDTO.firstPolicyEvaluationTime =
          min(policyEvaluationTimestampsDTO.firstPolicyEvaluationTime, repositoryComponent.getTime());
      policyEvaluationTimestampsDTO.latestPolicyEvaluationTime =
          max(policyEvaluationTimestampsDTO.latestPolicyEvaluationTime, repositoryComponent.getLastEvaluationTime());
      policyEvaluationTimestampsDTO.quarantineTime =
          max(policyEvaluationTimestampsDTO.quarantineTime, repositoryComponent.getQuarantineTime());
      policyEvaluationTimestampsDTO.unquarantineTime =
          max(policyEvaluationTimestampsDTO.unquarantineTime, repositoryComponent.getUnquarantineTime());
    }
    if (repositoryComponents.size() == 1) {
      policyEvaluationTimestampsDTO.autoUnquarantined = repositoryComponents.get(0).getAutoUnquarantined();
    }

    return policyEvaluationTimestampsDTO;
  }

  private Date min(Date date1, Date date2) {
    if (date1 == null && date2 == null) {
      return null;
    }
    if (date1 == null) {
      return date2;
    }
    if (date2 == null) {
      return date1;
    }
    return date1.compareTo(date2) < 0 ? date1 : date2;
  }

  private Date max(Date date1, Date date2) {
    if (date1 == null && date2 == null) {
      return null;
    }
    if (date1 == null) {
      return date2;
    }
    if (date2 == null) {
      return date1;
    }
    return date1.compareTo(date2) > 0 ? date1 : date2;
  }

  @Authorize(permission = Permission.READ)
  List<RepositoryPolicyViolationDTO> getPolicyViolations(
      @AuthzContext(Key.REPOSITORY_ID) String repositoryId,
      String pathname)
  {
    auditComponentPath(pathname);
    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repositoryId, pathname);
    if (repositoryComponent == null) {
      throw new NotFoundException(
          "Cannot find a component with path " + pathname + " in repository with ID " + repositoryId + ".");
    }
    AuditData.get()
        .setComponentIdentifier(repositoryComponent.getComponentIdentifier())
        .setComponentHash(repositoryComponent.getHash());

    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathname(repositoryId, repositoryComponent.getPathname());
    repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);

    List<RepositoryPolicyViolationDTO> repositoryPolicyViolationDTOs =
        repositoryPolicyViolations.stream().map(this::toRepositoryPolicyViolationDTO).collect(toList());

    return repositoryPolicyViolationDTOs;
  }

  @Authorize(permission = Permission.READ)
  RepositoryPolicyViolationDTO getPolicyViolation(
      @AuthzContext(Key.REPOSITORY_ID) String repositoryId,
      String repositoryPolicyViolationId)
  {
    RepositoryPolicyViolation repositoryPolicyViolation =
        repositoryPolicyViolationDAO.getById(repositoryPolicyViolationId);
    if (repositoryPolicyViolation == null || !repositoryId.equals(repositoryPolicyViolation.getRepositoryId())) {
      throw new NotFoundException("Cannot find a repository policy violation with ID " + repositoryPolicyViolationId
          + " in repository with ID " + repositoryId + ".");
    }

    repositoryPolicyViolationDAO.loadConstraintFacts(Collections.singletonList(repositoryPolicyViolation));

    auditComponentPath(repositoryPolicyViolation.getPathname());
    AuditData.get()
        .setComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier())
        .setComponentHash(repositoryPolicyViolation.getHash());

    return toRepositoryPolicyViolationDTO(repositoryPolicyViolation);
  }

  public RepositoryPolicyViolationDTO toRepositoryPolicyViolationDTO(
      RepositoryPolicyViolation repositoryPolicyViolation)
  {
    List<PolicyThreats.PolicyConstraint> constraints =
        PolicyThreatsAdapter.toPolicyThreatsPolicyConstraints(repositoryPolicyViolation.getConstraintFacts());
    Policy policy = policyDAO.getById(repositoryPolicyViolation.getPolicyId());
    Owner policyOwner = policy == null ? null : ownerDAO.getById(policy.getOwnerId());
    return new RepositoryPolicyViolationDTO(repositoryPolicyViolation.getId(),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier()),
        ComponentDisplayNameUtil.fromIdentifier(repositoryPolicyViolation.getComponentIdentifier()),
        repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getPolicyId(),
        repositoryPolicyViolation.getPolicyName(), policyOwner, repositoryPolicyViolation.getThreatLevel(),
        repositoryPolicyViolation.getThreatCategory(), constraints, repositoryPolicyViolation.getConstraintFactsJson(),
        repositoryPolicyViolation.isWaived(), repositoryPolicyViolation.getActionTypeId(),
        repositoryPolicyViolation.getTime());
  }

  void updateProprietaryComponentNamePattern(ProprietaryComponentNamePatternDTO proprietaryComponentNamePatternDTO) {
    if (proprietaryComponentNamePatternDTO == null) {
      throw new BadRequestException("Missing request parameters");
    }

    log.debug("Updating proprietary component name pattern: {}", proprietaryComponentNamePatternDTO);

    ProprietaryComponentNamePattern proprietaryComponentNamePattern =
        proprietaryComponentNamePatternDAO.getById(proprietaryComponentNamePatternDTO.id);
    if (proprietaryComponentNamePattern == null) {
      throw new NotFoundException(
          "Cannot find a proprietary component name pattern with ID=" + proprietaryComponentNamePatternDTO.id);
    }

    Repository repository = repositoryDAO.getByIdNotNull(proprietaryComponentNamePattern.getRepositoryId());
    checkWritePermission(repository);

    // Only the enabled flag can be updated
    proprietaryComponentNamePattern.setEnabled(proprietaryComponentNamePatternDTO.enabled);
    proprietaryComponentNamePatternDAO.update(proprietaryComponentNamePattern);
    proprietaryComponentNameDetector.invalidateMatchers();
    proprietaryComponentNameDetector.invalidateMatchersOnOtherNodes();
  }

  RepositoriesDTO getRepositoriesByRepositoryManagerId(String repositoryManagerId) {
    log.debug("Getting repositories for repository manager ID {}...", repositoryManagerId);

    List<Repository> repositories =
        filterRepositoriesWithReadPermission(repositoryDAO.getByRepositoryManagerId(repositoryManagerId));
    log.debug("Found {} repositories for repository manager ID {}.", repositories.size(), repositoryManagerId);

    if (repositories.isEmpty()) {
      return new RepositoriesDTO();
    }

    return new RepositoriesDTO(convertRepositories(repositories));
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  @Authorize(permission = Permission.WRITE)
  void checkWritePermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  public boolean checkReadPermissionRepositoryContainer() {
    try {
      checkReadPermission(RepositoryContainer.SINGLETON);
      return true;
    }
    catch (UnauthenticatedException | UnauthorizedException error) {
      return false;
    }
  }

  ProprietaryComponentNamePatternFilter validateAndInitializeFilter(
      ProprietaryComponentNamePatternRequest request)
  {
    if (request.page <= 0 || request.pageSize <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }

    ProprietaryComponentNamePatternFilter filter = new ProprietaryComponentNamePatternFilter();
    filter.page = request.page;
    filter.pageSize = request.pageSize;
    filter.searchFilters = request.searchFilters;
    filter.sortFields = request.sortFields;

    return filter;
  }

  /**
   * @since 1.160
   */
  List<RepositoryManager> getUnconfiguredRepositoryManagers() {
    log.debug("Getting unconfigured repository managers...");

    // This method is called by the UI to determine if the Firewall Onboarding UI should be shown for the current user.
    // Although it is a getter, if the user doesn't have WRITE permission, s/he cannot finish the Firewall Onboarding.
    checkWritePermission(RepositoryContainer.SINGLETON);

    if (!SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.isEnabled()) {
      return Collections.emptyList();
    }

    List<RepositoryManager> unconfiguredRepositoryManagers = repositoryManagerDAO.getUnconfigured();
    String nxrmMinimalVersion = "3.60.0-SNAPSHOT";
    String artifactoryPluginMinimalVersion = "2.4.8-SNAPSHOT";
    unconfiguredRepositoryManagers = unconfiguredRepositoryManagers.stream()
        .filter(repositoryManager -> ("Nexus".equals(repositoryManager.getProductName())
            && compareVersions(repositoryManager.getProductVersion(), nxrmMinimalVersion) >= 0) ||
            ("Firewall_For_Jfrog_Artifactory".equals(repositoryManager.getProductName())
                && compareVersions(repositoryManager.getProductVersion(), artifactoryPluginMinimalVersion) >= 0))
        .collect(Collectors.toList());
    log.debug("Found {} unconfigured repository managers.", unconfiguredRepositoryManagers.size());
    return unconfiguredRepositoryManagers;
  }

  private static int compareVersions(String version1, String version2) {
    try {
      GenericVersionScheme scheme = new GenericVersionScheme();
      Version ver1 = scheme.parseVersion(version1);
      Version ver2 = scheme.parseVersion(version2);
      return ver1.compareTo(ver2);
    }
    catch (InvalidVersionSpecificationException e) {
      // the generic version scheme should accept anything
      throw new IllegalStateException(e);
    }
  }

  private void auditConfigureRepository(Repository repository, String errorMessage) {
    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CONFIGURE_REPOSITORY, false)) {
      AuditData.get().setRepository(repository);
      if (errorMessage != null) {
        AuditData.get().setError(errorMessage);
      }
    }
  }

  /**
   * @since 1.161
   */
  @Authorize(permission = Permission.WRITE)
  void configureRepositories(
      @AuthzContext(Key.REPOSITORY_MANAGER_ID) String repositoryManagerId,
      List<Repository> repositories)
  {
    AuditData.get().setRepositoryManagerId(repositoryManagerId);

    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repositoryManagerId);

    if (repositoryManager != null) {
      AuditData.get().setRepositoryManagerInstanceId(repositoryManager.getInstanceId());
    }

    if (repositoryManager == null) {
      throw new NotFoundException("Cannot find a repository manager with ID " + repositoryManagerId + ".");
    }

    try {
      repositoryManager.setConfigured(true);
      repositoryManager.setConfigureTime(new Date());
      repositoryManagerDAO.update(repositoryManager);
    }
    catch (RuntimeException e) {
      String errorMessage = String.format("Error updating repository manager with instance ID %s (%s): %s",
          repositoryManager.getInstanceId(), repositoryManager.getId(), e.getMessage());
      log.error(errorMessage, e);
      AuditData.get().setError(errorMessage);
    }

    if (repositories == null) {
      repositories = Collections.emptyList();
    }

    log.debug("Updating configuration of {} repositories for repository manager with ID:{}",
        repositories.size(), repositoryManager.getId());

    try {
      for (Repository repository : repositories) {
        Repository existingRepository =
            repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
                repository.getPublicId());

        if (existingRepository == null) {
          log.error(
              "Cannot update repository config with repository manager ID:{} and name:{} because " +
                  "it does not exist.",
              repositoryManager.getId(), repository.getPublicId());
          continue;
        }

        boolean updated = false;

        switch (existingRepository.getRepositoryType()) {
          case proxy:
            if (existingRepository.isQuarantineEnabled() != repository.isQuarantineEnabled()) {
              if (repository.isQuarantineEnabled()) {
                existingRepository.setAuditEnabled(true);
                existingRepository.setQuarantineEnabled(true);
                if (ComponentIdentifier.FORMAT_NPM.equals(existingRepository.getFormat())) {
                  existingRepository.setPolicyCompliantComponentSelectionEnabled(true);
                }
              }
              else {
                // Don't change the auditEnabld flag
                existingRepository.setQuarantineEnabled(false);
                existingRepository.setPolicyCompliantComponentSelectionEnabled(false);
              }
              updated = true;
            }
            break;
          case hosted:
            if (existingRepository.isNamespaceConfusionProtectionEnabled() != repository
                .isNamespaceConfusionProtectionEnabled())
            {
              existingRepository
                  .setNamespaceConfusionProtectionEnabled(repository.isNamespaceConfusionProtectionEnabled());
              updated = true;
            }
            break;
          default:
            log.error("Unknown repository type '{}' for repository {}:{} ({})", existingRepository.getRepositoryType(),
                existingRepository.getRepositoryManagerId(), existingRepository.getPublicId(),
                existingRepository.getId());
        }

        if (updated) {
          existingRepository.setLastManualConfigureTime(new Date());
          try {
            repositoryDAO.update(existingRepository);
            auditConfigureRepository(existingRepository, null /* errorMessage */);
          }
          catch (RuntimeException e) {
            String errorMessage = String.format("Error updating repository %s (%s): %s", existingRepository.getName(),
                existingRepository.getId(), e.getMessage());
            log.error(errorMessage, e);
            auditConfigureRepository(existingRepository, errorMessage);
          }
        }
      }
    }
    finally {
      AuditData.get().commitSubEvents();
    }
  }

  void configureFirewallOnboarding(FirewallOnboardingOptionsDTO firewallOnboardingOptionsDTO) {
    checkWritePermission(RepositoryContainer.SINGLETON);

    log.debug("Configuring firewall onboarding options.");

    try {
      setPolicyAction("Security-Malicious",
          firewallOnboardingOptionsDTO.supplyChainAttacksProtectionEnabled ? FailActionType.ID : null);
      setPolicyAction("Integrity-Rating",
          firewallOnboardingOptionsDTO.supplyChainAttacksProtectionEnabled ? FailActionType.ID : null);

      setPolicyAction("Security-Namespace Conflict",
          firewallOnboardingOptionsDTO.namespaceConfusionProtectionEnabled ? FailActionType.ID : null);
    }
    finally {
      AuditData.get().commitSubEvents();
    }
  }

  // Visible for testing
  void setPolicyAction(String policyName, String policyActionId) {
    Policy policy = policyDAO.getByOwnerIdAndName(RepositoryContainer.REPOSITORY_CONTAINER_ID, policyName);
    if (policy == null) {
      policy = policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, policyName);
      log.debug("Found policy '{}' for root organization.", policyName);
    }
    else {
      log.debug("Found policy '{}' for repository container.", policyName);
    }
    if (policy == null) {
      log.warn("Did not find policy '{}' to configure for firewall onboarding.", policyName);
      return;
    }

    Map<String, String> policyActions = policy.getActions();
    String proxyStageActionId = policyActions.get(ProxyStageType.ID);
    boolean policyUpdated = false;
    if (policyActionId != null) {
      if (!policyActionId.equals(proxyStageActionId)) {
        policy.setAction(ProxyStageType.ID, policyActionId);
        policyUpdated = true;
      }
    }
    else if (proxyStageActionId != null) {
      policyActions.remove(ProxyStageType.ID);
      policy.setActions(policyActions);
      policyUpdated = true;
    }

    if (policyUpdated) {
      log.debug("Setting action={} for proxy stage for policy '{}'.", policyActionId, policyName);
      policyDAO.update(policy);
      log.debug("Updated policy '{}' with action '{}' for the proxy stage.", policyName,
          policyActionId);

      try (AuditSession auditSession =
          AuditData.get().recordSubEvent(AuditEvent.UPDATE_POLICY, false /* independent */))
      {
        AuditData.get().setPolicyWithDetails(policy);
      }
    }
    else {
      log.debug("Policy '{}' already has action={} for proxy stage.", policyName, policyActionId);
    }
  }

  @Authorize(permission = Permission.WRITE)
  void updateName(@AuthzContext(Key.REPOSITORY_MANAGER_ID) String repositoryManagerId, String name) {
    AuditData.get().setRepositoryManagerId(repositoryManagerId);

    log.debug("Updating name of repository manager with id {} to: {}", repositoryManagerId, name);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByIdNotNull(repositoryManagerId);

    AuditData.get()
        .setRepositoryManagerInstanceId(repositoryManager.getInstanceId())
        .setRepositoryManagerName(repositoryManager.getName());

    repositoryManager.setName(name);
    repositoryManagerDAO.update(repositoryManager);

    log.debug("Repository manager with id {} updated to name: {}", repositoryManagerId, name);
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY_MANAGER)
  public List<RepositoryManager> getRepositoryManagers() {
    return repositoryManagerDAO.getAll();
  }

  ProprietaryComponentNamePatternsPage getProprietaryComponentNamePatternsByOwner(
      OwnerType ownerType,
      String ownerId,
      ProprietaryComponentNamePatternRequest request)
  {
    log.debug("Getting proprietary component name patterns for {} id {}", ownerType, ownerId);

    if (request == null) {
      throw new BadRequestException("Missing request parameters");
    }

    ProprietaryComponentNamePatternFilter filter = validateAndInitializeFilter(request);

    List<Repository> repositories;

    if (ownerType.equals(OwnerType.REPOSITORY)) {
      repositories = Collections.singletonList(repositoryDAO.getByIdNotNull(ownerId));
    }
    else if (ownerType.equals(OwnerType.REPOSITORY_MANAGER)) {
      ownerId = repositoryManagerDAO.getByIdNotNull(ownerId).getId();
      repositories = repositoryDAO.getByRepositoryManagerIdAndRepositoryType(ownerId, RepositoryType.hosted);
    }
    else if (ownerType.equals(OwnerType.REPOSITORY_CONTAINER)) {
      repositories = repositoryDAO.getByRepositoryType(RepositoryType.hosted);
    }
    else {
      throw new IllegalStateException("Invalid owner type: " + ownerType);
    }

    List<Repository> hostedRepositoriesWithReadPermission = filterRepositoriesWithReadPermission(repositories);
    Set<String> repositoryIds =
        hostedRepositoriesWithReadPermission.stream().map(Repository::getId).collect(Collectors.toSet());
    List<ProprietaryComponentNamePatternDTO> proprietaryComponentNamePatterns =
        proprietaryComponentNamePatternDAO.getByFilter(repositoryIds, filter);

    ProprietaryComponentNamePatternsPage result = new ProprietaryComponentNamePatternsPage();

    int iPattern = 1;
    for (ProprietaryComponentNamePatternDTO proprietaryComponentNamePattern : proprietaryComponentNamePatterns) {
      if (iPattern <= request.pageSize) {
        result.proprietaryComponentNamePatterns.add(proprietaryComponentNamePattern);
      }
      else {
        result.hasNextPage = true;
        break;
      }

      iPattern++;
    }

    return result;
  }
}
