/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
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
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

@Named
public class RepositoryService
{
  public static final int MAX_REPOSITORY_EVALUATION_REQUEST_SIZE = 100;

  private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

  private static final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private static final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private static final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO =
      new ProprietaryComponentNamePatternDAO();

  private static final PolicyDAO policyDAO = new PolicyDAO();

  private static final OwnerDAO ownerDAO = new OwnerDAO();

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  @Inject
  public RepositoryService(
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      FirewallIgnorePatternService firewallIgnorePatternService)
  {
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
  }

  /**
   * @since 1.19.0
   */
  @Authorize(permission = Permission.WRITE)
  public void unquarantineComponent(
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
    summary.quarantinedComponentCount =
        repositoryComponentDAO.getQuarantinedComponentCountByRepositoryId(repositoryId);

    log.debug("Got summary for repository {}:{} ({}) in {} ms", repository.getRepositoryManagerId(),
        repository.getPublicId(), repositoryId, System.currentTimeMillis() - start);

    return summary;
  }

  /**
   * @since 1.18.0
   */
  @Authorize(permission = Permission.READ)
  public RepositoryDTO getRepositoryById(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    RepositoryDTO repositoryDTO = convertRepository(repositoryDAO.getByIdNotNull(repositoryId));
    Date evaluationTime = repositoryComponentDAO.getOldestComponentEvaluationTimeByRepositoryId(repositoryId);
    if (evaluationTime != null) {
      repositoryDTO.oldestEvalTimestamp = evaluationTime.getTime();
    }
    return repositoryDTO;
  }

  private final Executor reevalExecutor = createReevaluationExecutor();

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public void reevaluateRepository(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    AuditData.get().continueAsync(reevalExecutor, new RepositoryReevaluationTask(repository, repositoryPolicyEvaluator,
        reevalExecutor, MAX_REPOSITORY_EVALUATION_REQUEST_SIZE));
  }

  private static Executor createReevaluationExecutor() {
    ThreadPoolExecutor executor = new TenantThreadPoolExecutor(20, 20, 5L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setDaemon(true)
            .setNameFormat("ReevaluateRepository-%s").build());

    executor.allowCoreThreadTimeOut(true);

    return executor;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteRepository(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    repositoryDAO.delete(repository);
    AuditData.get().setData("repositoryManagerInstanceId",
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
    List<RepositoryDTO> repositoryDTOs = new ArrayList<>(repositories.size());
    for (Repository repository : repositories) {
      repositoryDTOs.add(convertRepository(repository));
    }
    return new RepositoriesDTO(repositoryDTOs);
  }

  public Set<Repository> getRepositoriesByIds(final Set<String> repositoryIds) {
    Set<Repository> allRepositories = new HashSet<>(getRepositoriesWithReadPermission());

    if (CollectionUtils.isEmpty(repositoryIds)) {
      return allRepositories;
    }

    return allRepositories
        .stream()
        .filter(repository -> repositoryIds.contains(repository.getId()))
        .collect(Collectors.toSet());
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY)
  public List<Repository> getRepositoriesWithReadPermission() {
    return repositoryDAO.getAll();
  }

  private RepositoryDTO convertRepository(Repository repository) {
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.repository = repository;
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    repositoryDTO.managerInstanceId = repositoryManager.getInstanceId();
    return repositoryDTO;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public void reevaluateComponent(@AuthzContext(Key.REPOSITORY_ID) String repositoryId,
                                  String hash,
                                  final String clientUserAgent)
  {
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    List<RepositoryComponent> components = repositoryComponentDAO.getByRepositoryIdAndHash(repository.getId(), hash);
    AuditData.get().setData("componentCount", components.size())
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
   * Used by the web UI to display various timestamps related to policy evaluations.
   * The UI calls this method for component versions for which it only has a component identifier (no hash or pathname).
   *
   * @since 1.139
   */
  @Authorize(permission = Permission.READ)
  PolicyEvaluationTimestampsDTO getPolicyEvaluationTimestamps(
      @AuthzContext(Key.REPOSITORY_ID) String repositoryId,
      ComponentIdentifier componentIdentifier)
  {
    PolicyEvaluationTimestampsDTO policyEvaluationTimestampsDTO = new PolicyEvaluationTimestampsDTO();

    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndComponentIdentifier(repositoryId, componentIdentifier);

    if (repositoryComponent == null) {
      return policyEvaluationTimestampsDTO;
    }

    policyEvaluationTimestampsDTO.firstPolicyEvaluationTime = repositoryComponent.getTime();
    policyEvaluationTimestampsDTO.latestPolicyEvaluationTime = repositoryComponent.getLastEvaluationTime();
    policyEvaluationTimestampsDTO.quarantineTime = repositoryComponent.getQuarantineTime();
    policyEvaluationTimestampsDTO.unquarantineTime = repositoryComponent.getUnquarantineTime();
    policyEvaluationTimestampsDTO.autoUnquarantined = repositoryComponent.getAutoUnquarantined();

    return policyEvaluationTimestampsDTO;
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
    AuditData.get().setComponentIdentifier(repositoryComponent.getComponentIdentifier())
        .setComponentHash(repositoryComponent.getHash());

    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathname(repositoryId, repositoryComponent.getPathname());

    List<RepositoryPolicyViolationDTO> repositoryPolicyViolationDTOs =
        repositoryPolicyViolations.stream().map(RepositoryService::toRepositoryPolicyViolationDTO).collect(toList());

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

    auditComponentPath(repositoryPolicyViolation.getPathname());
    AuditData.get().setComponentIdentifier(repositoryPolicyViolation.getComponentIdentifier())
        .setComponentHash(repositoryPolicyViolation.getHash());

    return RepositoryService.toRepositoryPolicyViolationDTO(repositoryPolicyViolation);
  }

  public static RepositoryPolicyViolationDTO toRepositoryPolicyViolationDTO(
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

  public ProprietaryComponentNamePatternsPage getProprietaryComponentNamePatterns(
      ProprietaryComponentNamePatternRequest request)
  {
    checkReadPermission(RepositoryContainer.SINGLETON);

    log.debug("Getting proprietary component name patterns");

    if (request == null) {
      throw new BadRequestException("Missing request parameters");
    }

    ProprietaryComponentNamePatternFilter filter = validateAndInitializeFilter(request);

    List<ProprietaryComponentNamePatternDTO> proprietaryComponentNamePatterns =
        proprietaryComponentNamePatternDAO.getByFilter(filter);

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

  public void updateProprietaryComponentNamePattern(
      ProprietaryComponentNamePatternDTO proprietaryComponentNamePatternDTO)
  {
    checkWritePermission(RepositoryContainer.SINGLETON);

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

    // Only the enabled flag can be updated
    proprietaryComponentNamePattern.setEnabled(proprietaryComponentNamePatternDTO.enabled);
    proprietaryComponentNamePatternDAO.update(proprietaryComponentNamePattern);
  }

  List<Repository> getSupportedRepositories(String repositoryManagerId) {
    checkReadPermission(RepositoryContainer.SINGLETON);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManagerId);

    if (repositories.isEmpty()) {
      return Collections.emptyList();
    }

    FirewallIgnorePatterns firewallIgnorePatterns = firewallIgnorePatternService.getIgnorePatterns();
    Set<String> supportedFormats = firewallIgnorePatterns.regexpsByRepositoryFormat.keySet();

    List<Repository> supportedRepositories = new ArrayList<>();

    for (Repository repository : repositories) {
      if (supportedFormats.contains(repository.getFormat())) {
        supportedRepositories.add(repository);
      }
    }
    return supportedRepositories;
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
    // This method is called by the UI to determine if the Firewall Onboarding UI should be shown for the current user.
    // Although it is a getter, if the user doesn't have WRITE permission, s/he cannot finish the Firewall Onboarding.
    checkWritePermission(RepositoryContainer.SINGLETON);

    if (!SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.isEnabled()) {
      return Collections.emptyList();
    }

    return repositoryManagerDAO.getUnconfigured();
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
  void configureRepositories(String repositoryManagerId, List<Repository> repositories) {
    AuditData.get().setRepositoryManagerId(repositoryManagerId);

    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repositoryManagerId);

    if (repositoryManager != null) {
      AuditData.get().setRepositoryManagerInstanceId(repositoryManager.getInstanceId());
    }

    checkWritePermission(RepositoryContainer.SINGLETON);

    if (repositoryManager == null) {
      throw new NotFoundException("Cannot find a repository manager with ID " + repositoryManagerId + ".");
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
                  "it does not exist.", repositoryManager.getId(), repository.getPublicId());
          continue;
        }

        boolean updated = false;

        if (existingRepository.isAuditEnabled() != repository.isAuditEnabled()) {
          updated = true;
        }
        if (existingRepository.isQuarantineEnabled() != repository.isQuarantineEnabled()) {
          updated = true;
        }
        if (existingRepository.isPolicyCompliantComponentSelectionEnabled() != repository
            .isPolicyCompliantComponentSelectionEnabled()) {
          updated = true;
        }
        if (existingRepository.isNamespaceConfusionProtectionEnabled() !=
            repository.isNamespaceConfusionProtectionEnabled()) {
          updated = true;
        }

        if (updated) {
          repository.setLastManualConfigureTime(new Date());
          try {
            repositoryDAO.update(repository);
            auditConfigureRepository(repository, null /* errorMessage */);
          }
          catch (RuntimeException e) {
            String errorMessage = String.format("Error updating repository %s (%s): %s", repository.getName(),
                repository.getId(), e.getMessage());
            log.error(errorMessage, e);
            auditConfigureRepository(repository, errorMessage);
          }
        }
      }
    }
    finally {
      AuditData.get().commitSubEvents();
    }
  }
}
