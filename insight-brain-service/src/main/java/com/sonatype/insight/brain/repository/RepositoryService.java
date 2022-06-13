/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositoryService
{
  public static final int MAX_REPOSITORY_EVALUATION_REQUEST_SIZE = 100;

  private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

  private static final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private static final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  public RepositoryService(
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
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

  public RepositoryPolicyThreatDTO getPolicyThreats(final String repositoryId, final String pathname) {
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    return getPolicyThreats(repository, pathname);
  }

  private void auditComponentPath(final String pathname) {
    AuditData.get().setData("componentPathname", pathname);
  }

  @Authorize(permission = Permission.READ)
  RepositoryPolicyThreatDTO getPolicyThreats(@AuthzContext(Key.REPOSITORY) final Repository repository,
                                             final String pathname)
  {
    auditComponentPath(pathname);
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    if (repositoryComponent == null) {
      throw new NotFoundException("Cannot find a component with path " + pathname + " in repository with ID "
          + repository.getId() + ".");
    }
    AuditData.get()
        .setComponentIdentifier(repositoryComponent.getComponentIdentifier())
        .setComponentHash(repositoryComponent.getHash());

    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathnameAndWaived(repository.getId(), repositoryComponent.getPathname(), false);

    List<RepositoryPolicyViolationDTO> activeRepositoryViolationDTOs = new ArrayList<>();
    for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
      List<PolicyThreats.PolicyConstraint> constraints =
          PolicyThreatsAdapter.toPolicyThreatsPolicyConstraints(repositoryPolicyViolation.getConstraintFacts());
      activeRepositoryViolationDTOs.add(new RepositoryPolicyViolationDTO(repositoryPolicyViolation.getPolicyId(),
          repositoryPolicyViolation.getPolicyName(), repositoryPolicyViolation.getThreatLevel(),
          Action.ID_FAIL.equals(repositoryPolicyViolation.getActionTypeId()), constraints,
          repositoryPolicyViolation.getConstraintFactsJson()));
    }

    return new RepositoryPolicyThreatDTO(activeRepositoryViolationDTOs);
  }

  public RepositoryReportSummary getReportSummary(String repositoryId) {
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

    log.debug("Get report summary for repository {}:{} ({})", repository.getRepositoryManagerId(),
        repository.getPublicId(), repositoryId);

    return getReportSummary(repository);
  }

  @Authorize(permission = Permission.READ)
  RepositoryReportSummary getReportSummary(@AuthzContext(Key.REPOSITORY) Repository repository) {
    RepositoryReportSummary summary = new RepositoryReportSummary();
    summary.knownComponentCount = repositoryComponentDAO.getKnownComponentCountByRepositoryId(repository.getId());
    summary.totalComponentCount = repositoryComponentDAO.getComponentCountByRepositoryId(repository.getId());

    RepositoryPolicyEvaluationSummary policyEvalSummary = this.getPolicyEvaluationSummaryInternal(repository);
    summary.criticalComponentCount = policyEvalSummary.getCriticalComponentCount();
    summary.severeComponentCount = policyEvalSummary.getSevereComponentCount();
    summary.moderateComponentCount = policyEvalSummary.getModerateComponentCount();
    summary.affectedComponentCount = policyEvalSummary.getAffectedComponentCount();
    summary.quarantinedComponentCount = policyEvalSummary.getQuarantinedComponentCount();

    return summary;
  }

  private RepositoryPolicyEvaluationSummary getPolicyEvaluationSummaryInternal(final Repository repository) {
    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndNotWaived(repository.getId());

    final Map<String, Integer> componentThreatLevels = new HashMap<>();
    for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
      String pathname = repositoryPolicyViolation.getPathname();
      Integer threatLevel = componentThreatLevels.get(pathname);
      if (threatLevel == null || threatLevel < repositoryPolicyViolation.getThreatLevel()) {
        componentThreatLevels.put(pathname, repositoryPolicyViolation.getThreatLevel());
      }
    }
    int criticalCount = 0;
    int severeCount = 0;
    int moderateCount = 0;
    for (final int level : componentThreatLevels.values()) {
      if (level >= 8) {
        criticalCount++;
      }
      else if (level >= 4) {
        severeCount++;
      }
      else if (level >= 2) {
        moderateCount++;
      }
    }

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = new RepositoryPolicyEvaluationSummary();
    policyEvaluationSummary.setCriticalComponentCount(criticalCount);
    policyEvaluationSummary.setSevereComponentCount(severeCount);
    policyEvaluationSummary.setModerateComponentCount(moderateCount);
    policyEvaluationSummary.setAffectedComponentCount(criticalCount + severeCount + moderateCount);
    policyEvaluationSummary.setQuarantinedComponentCount(repositoryComponentDAO
        .getQuarantinedComponentCountByRepositoryId(repository.getId()));

    policyEvaluationSummary.setReportUrl(UserInterfaceLinksHelper.getRepositoryReportUrl(repository.getId()));

    return policyEvaluationSummary;
  }

  public List<RepositoryReportDetail> getReportDetails(final String repositoryId, String hash, String pathname) {
    final Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

    log.debug("Get report details for repository {}:{} ({})", repository.getRepositoryManagerId(),
        repository.getPublicId(), repository.getId());

    return getReportDetails(repository, hash, pathname);
  }

  @Authorize(permission = Permission.READ)
  List<RepositoryReportDetail> getReportDetails(@AuthzContext(Key.REPOSITORY) final Repository repository,
                                                String hash,
                                                String pathname)
  {
    final List<RepositoryReportDetail> details = new ArrayList<>();

    final List<RepositoryComponent> componentList;
    if (hash != null) {
      if (pathname != null) {
        throw new BadRequestException("Either a pathname or a hash is supported, not both.");
      }
      componentList = repositoryComponentDAO.getByRepositoryIdAndHash(repository.getId(), hash);
    }
    else if (pathname != null) {
      componentList = Collections
          .singletonList(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname));
    }
    else {
      componentList = repositoryComponentDAO.getByRepositoryId(repository.getId());
    }

    for (final RepositoryComponent component : componentList) {

      final List<RepositoryPolicyViolation> componentViolations = repositoryPolicyViolationDAO
          // violations are sorted by 'ThreatLevel DESC, policyId', so highestThreatLevel per component is first
          .getActiveByRepositoryIdAndPathname(repository.getId(), component.getPathname());
      boolean highestThreatLevel = true;

      if (componentViolations.size() > 0) {
        boolean allWaived = true;
        for (final RepositoryPolicyViolation violation : componentViolations) {
          details.add(RepositoryReportDetail.create(component, violation, highestThreatLevel));
          // like the CI report, we choose one of the violations and use it as the highest.
          highestThreatLevel = violation.isWaived() ? highestThreatLevel : false;
          allWaived = allWaived && violation.isWaived();
        }
        // if all violations of this component are waived, we still want to return a 'no violation' entry
        if (allWaived) {
          details.add(RepositoryReportDetail.create(component));
        }
      }
      else {
        details.add(RepositoryReportDetail.create(component));
      }
    }

    // sort by threatLevel DESC, pathname ASC
    // note the UI is dependant on this sort order
    details.sort(THREAT_LEVEL_DESC_PATHNAME_ASC);

    return details;
  }

  /**
   * Sort by threatLevel DESC, pathname ASC.
   */
  static final Comparator<RepositoryReportDetail> THREAT_LEVEL_DESC_PATHNAME_ASC = (detail1, detail2) -> {
    // sort ThreatLevel Descending
    final int cmpThreatLevel = detail2.getThreatLevel() - detail1.getThreatLevel();
    if (cmpThreatLevel != 0) {
      return cmpThreatLevel;
    }

    // sort pathname Ascending
    return detail1.getPathname().compareTo(detail2.getPathname());
  };

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
    ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 5L, TimeUnit.SECONDS,
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

    if (repository.isEnabled()) {
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

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY)
  List<Repository> getRepositoriesWithReadPermission() {
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
}
