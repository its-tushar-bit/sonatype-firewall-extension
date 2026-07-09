/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static java.util.stream.Collectors.toMap;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationStageDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaivedPolicyViolationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.76
 */
@Named
@Singleton
public class ApiComponentsWithWaiversReportingService
{
  private static final String APPLICATION_COMPONENTS_AUDIT_KEY = "numberOfApplicationComponents";

  private static final String REPOSITORY_COMPONENTS_AUDIT_KEY = "numberOfRepositoryComponents";

  private static final String POLICY_WAIVER_NOT_FOUND_MSG = "Related policy waiver not found. Please re-evaluate.";

  private static final String POLICY_WAIVER_OWNER_NOT_IN_SCOPE_MSG =
      "Related policy waiver owner is not in scope. Please re-evaluate.";

  private final PolicyViolationLoader policyViolationLoader;

  private final RepositoryService repositoryService;

  private final PolicyViolationDAO policyViolationDao;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDao;

  private final PolicyWaiverDAO policyWaiverDao;

  private static final Logger log = LoggerFactory.getLogger(ApiComponentsWithWaiversReportingService.class);

  private final ApplicationService applicationService;

  private final OwnerDAO ownerDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  public ApiComponentsWithWaiversReportingService(
      ApplicationService applicationService,
      PolicyViolationLoader policyViolationLoader,
      RepositoryService repositoryService,
      PolicyViolationDAO policyViolationDao,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDao,
      PolicyWaiverDAO policyWaiverDao,
      OwnerDAO ownerDAO,
      PolicyWaiverReasonDAO policyWaiverReasonDAO)
  {
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
    this.repositoryService = repositoryService;
    this.policyViolationDao = policyViolationDao;
    this.repositoryPolicyViolationDao = repositoryPolicyViolationDao;
    this.policyWaiverDao = policyWaiverDao;
    this.ownerDAO = ownerDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
  }

  public ApiComponentWaiversDTO getComponentsWithWaivers(String format) {
    final var waiverReasonIdToReasonMap = policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    return getComponentsWithWaivers(waiverReasonIdToReasonMap, format, 1000);
  }

  @VisibleForTesting
  ApiComponentWaiversDTO getComponentsWithWaivers(
      Map<String, PolicyWaiverReason> waiverReasonIdToReasonMap,
      String format,
      int appBatchSize)
  {
    final AtomicInteger componentsWithWaiversCount = new AtomicInteger(0);
    final AtomicInteger applicationComponentsWithWaiversCount = new AtomicInteger(0);

    Predicate<AbstractPolicyViolation> statePredicate =
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED).asPolicyViolationPredicate();

    Predicate<AbstractPolicyViolation> formatPredicate = violation -> {
      ComponentIdentifier componentId = violation.getComponentIdentifier();
      return componentId != null && componentId.getFormat().equals(format);
    };

    final Predicate<AbstractPolicyViolation> overallPredicate =
        format == null ? statePredicate : statePredicate.and(formatPredicate);

    ApiComponentWaiversDTO componentWaiversDTO = new ApiComponentWaiversDTO();
    componentWaiversDTO.applicationWaivers = Collections.synchronizedList(new ArrayList<>());

    for (List<Application> applications : ListUtils.partition(applicationService.getApplications(), appBatchSize)) {
      buildApplicationWaiverDTOs(
          policyViolationLoader.getViolations(applications, null, false, overallPredicate),
          componentsWithWaiversCount,
          applicationComponentsWithWaiversCount,
          componentWaiversDTO.applicationWaivers::add);
    }

    final List<RepositoryDTO> repositoryDTOs = repositoryService.getRepositories().repositories;

    componentWaiversDTO.repositoryWaivers =
        buildRepositoryWaiverDTOs(
            waiverReasonIdToReasonMap,
            repositoryDTOs,
            overallPredicate,
            componentsWithWaiversCount);

    log.debug("getComponentsWithWaivers: Processed {} components with waived policy violations.",
        componentsWithWaiversCount);

    AuditData.get().setData(APPLICATION_COMPONENTS_AUDIT_KEY, applicationComponentsWithWaiversCount);

    return componentWaiversDTO;
  }

  private void buildApplicationWaiverDTOs(
      final Collection<ApplicationView> appViews,
      final AtomicInteger componentsWithWaiversCount,
      final AtomicInteger applicationComponentsWithWaiversCount,
      final Consumer<ApiApplicationWaiverDTO> waiverConsumer)
  {
    final var waiverReasonIdToReasonMap = policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    appViews
        .stream()
        .map(appView -> {
          // Rebind the caller's tenant on the shared pool worker; workers may retain an invalidated tenant
          // from a prior request, which would cause downstream tenant-scoped DAO calls to throw.
          return CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> {
            Application app = appView.getApplication();

            boolean anyAppPolicyViolations = false;

            ApiApplicationWaiverDTO applicationWaiverDTO = new ApiApplicationWaiverDTO();
            applicationWaiverDTO.application = buildApplicationBaseDTO(app);

            // We need to report on the latest evaluation for EACH stage.
            for (ApplicationStageView appStageView : appView.getStageViews()) {
              Collection<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
              if (policyViolations.isEmpty()) {
                continue;
              }
              else {
                anyAppPolicyViolations = true;
              }

              final ApiPolicyViolationStageDTO policyViolationStageDTO =
                  buildPolicyViolationStageDTO(applicationWaiverDTO, appStageView.getStageType().getId());

              final List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs = new ArrayList<>();
              policyViolationStageDTO.componentPolicyViolations = componentPolicyViolationDTOs;

              /*
               * Since our dto model consists of list of waived policy violations associated with a single
               * component identifier, we filter and group the waived policy violations by non-null component identifier
               * and process accordingly.
               */
              List<PolicyViolation> filteredPolicyViolations = policyViolations
                  .stream()
                  .filter(p -> p.getComponentIdentifier() != null)
                  .toList();
              policyViolationDao.loadConstraintFacts(filteredPolicyViolations);
              filteredPolicyViolations
                  .stream()
                  .collect(Collectors.groupingBy(PolicyViolation::getComponentIdentifier))
                  .forEach((componentIdentifier, policyViolationsByComponent) -> {
                    applicationComponentsWithWaiversCount.incrementAndGet();
                    // for this component identifier create a dto list of all the waived policy violations
                    componentPolicyViolationDTOs.add(buildComponentPolicyViolationDTO(
                        waiverReasonIdToReasonMap,
                        policyViolationsByComponent,
                        componentIdentifier,
                        policyViolationsByComponent.get(0).getHash(),
                        app.getId()));
                  });

              // Filter and group policy violations by hash where the component identifier is null but does have a hash
              filteredPolicyViolations = policyViolations.stream()
                  .filter(p -> p.getComponentIdentifier() == null && p.getHash() != null)
                  .toList();
              policyViolationDao.loadConstraintFacts(filteredPolicyViolations);
              filteredPolicyViolations
                  .stream()
                  .collect(Collectors.groupingBy(PolicyViolation::getHash))
                  .forEach((hash, policyViolationsByHash) -> {
                    applicationComponentsWithWaiversCount.incrementAndGet();
                    // for this hash create a dto list of all the waived policy violations
                    componentPolicyViolationDTOs.add(buildComponentPolicyViolationDTO(
                        waiverReasonIdToReasonMap,
                        policyViolationsByHash,
                        null,
                        hash,
                        app.getId()));
                  });
            }

            if (anyAppPolicyViolations) {
              return applicationWaiverDTO;
            }
            else {
              return null;
            }
          }), ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL));
        })
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .forEach(waiverConsumer);

    componentsWithWaiversCount.addAndGet(applicationComponentsWithWaiversCount.get());
  }

  private List<ApiRepositoryWaiverDTO> buildRepositoryWaiverDTOs(
      Map<String, PolicyWaiverReason> waiverReasonIdToReasonMap,
      List<RepositoryDTO> repositoryDTOs,
      Predicate<? super RepositoryPolicyViolation> violationFilterPredicate,
      final AtomicInteger componentsWithWaiversCount)
  {
    List<ApiRepositoryWaiverDTO> repositoryWaiverDTOs = new ArrayList<>();
    MutableInt repositoryComponentsWithWaiversCount = new MutableInt(0);

    if (repositoryDTOs != null && !repositoryDTOs.isEmpty()) {
      Map<String, Repository> idToRepositoryMap = repositoryDTOs.stream()
          .collect(toMap(repositoryDTO -> repositoryDTO.repository.getId(),
              repositoryDTO -> repositoryDTO.repository));

      List<RepositoryPolicyViolation> repositoryPolicyViolations =
          repositoryPolicyViolationDao.getActiveWaivedRepositoryPolicyViolations(idToRepositoryMap.keySet());
      repositoryPolicyViolationDao.loadConstraintFacts(repositoryPolicyViolations);

      repositoryPolicyViolations.stream()
          .filter(violationFilterPredicate)
          .collect(Collectors.groupingBy(RepositoryPolicyViolation::getRepositoryId))
          .forEach((repositoryId, policyViolations) -> {
            ApiRepositoryDTO repositoryDTO =
                ApiRepositoryAdapter.convert(idToRepositoryMap.get(repositoryId));
            List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs = new ArrayList<>();

            // Filter and group policy violations by non-null component identifier
            getGroupedRepositoryPolicyViolationsByComponentIdentifier(policyViolations)
                .forEach((componentIdentifier, policyViolationsByComponent) -> {
                  repositoryComponentsWithWaiversCount.increment();
                  // Grab the first hash it should be the same for all violations
                  String hash = policyViolationsByComponent.get(0).getHash();
                  // for this component identifier a dto list of all the waived policy violations
                  ApiComponentPolicyViolationDTO componentPolicyViolationDTO = buildComponentPolicyViolationDTO(
                      waiverReasonIdToReasonMap,
                      policyViolationsByComponent,
                      componentIdentifier,
                      hash,
                      repositoryId);

                  componentPolicyViolationDTOs.add(componentPolicyViolationDTO);
                });

            // Filter and group policy violations by hash where the component identifier is null
            getGroupedRepositoryPolicyViolationsByHash(policyViolations)
                .forEach((hash, policyViolationsByHash) -> {
                  repositoryComponentsWithWaiversCount.increment();
                  // for this hash create a dto list of all the waived policy violations
                  ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
                      buildComponentPolicyViolationDTO(
                          waiverReasonIdToReasonMap,
                          policyViolationsByHash,
                          null,
                          hash,
                          repositoryId);

                  componentPolicyViolationDTOs.add(componentPolicyViolationDTO);
                });

            ApiPolicyViolationStageDTO policyViolationStageDTO =
                buildPolicyViolationStageDTO(componentPolicyViolationDTOs);

            ApiRepositoryWaiverDTO repositoryWaiverDTO = new ApiRepositoryWaiverDTO();
            repositoryWaiverDTO.repository = repositoryDTO;
            repositoryWaiverDTO.stages = Arrays.asList(policyViolationStageDTO);
            repositoryWaiverDTOs.add(repositoryWaiverDTO);
          });
    }

    AuditData.get().setData(REPOSITORY_COMPONENTS_AUDIT_KEY, repositoryComponentsWithWaiversCount.intValue());
    componentsWithWaiversCount.addAndGet(repositoryComponentsWithWaiversCount.intValue());

    return repositoryWaiverDTOs;
  }

  private ApiComponentPolicyViolationDTO buildComponentPolicyViolationDTO(
      Map<String, PolicyWaiverReason> waiverReasonIdToReasonMap,
      List<? extends AbstractPolicyViolation> policyViolations,
      ComponentIdentifier componentIdentifier,
      String hash,
      String ownerId)
  {
    ApiComponentPolicyViolationDTO componentPolicyViolationDTO = new ApiComponentPolicyViolationDTO();
    componentPolicyViolationDTO.component = buildComponentDTO(componentIdentifier, hash);

    componentPolicyViolationDTO.waivedPolicyViolations =
        policyViolations.stream()
            .map(policyViolation -> buildWaivedPolicyViolationDTO(waiverReasonIdToReasonMap, policyViolation, ownerId))
            .collect(Collectors.toList());

    return componentPolicyViolationDTO;
  }

  private static ApiApplicationBaseDTO buildApplicationBaseDTO(Application app) {
    ApiApplicationBaseDTO application = new ApiApplicationBaseDTO();
    application.id = app.getId();
    application.publicId = app.getPublicId();
    application.name = app.getName();
    application.organizationId = app.getOrganizationId();
    application.contactUserName = app.getContactInternalName();

    return application;
  }

  private static ApiPolicyViolationStageDTO buildPolicyViolationStageDTO(
      ApiApplicationWaiverDTO applicationWaiverDTO,
      String stageId)
  {
    ApiPolicyViolationStageDTO policyViolationStageDTO = new ApiPolicyViolationStageDTO();
    policyViolationStageDTO.stageId = stageId;
    applicationWaiverDTO.stages.add(policyViolationStageDTO);

    return policyViolationStageDTO;
  }

  private static ApiComponentDTOV2 buildComponentDTO(ComponentIdentifier componentIdentifier, String hash) {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    if (componentIdentifier != null) {
      componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
      componentDTOV2.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
      ComponentDisplayName componentDisplayName =
          ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
      componentDTOV2.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;
    }
    componentDTOV2.hash = hash;
    componentDTOV2.proprietary = null;

    return componentDTOV2;
  }

  private ApiWaivedPolicyViolationDTO buildWaivedPolicyViolationDTO(
      Map<String, PolicyWaiverReason> waiverReasonIdToReasonMap,
      AbstractPolicyViolation policyViolation,
      String ownerId)
  {
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = new ApiWaivedPolicyViolationDTO();
    waivedPolicyViolationDTO.policyId = policyViolation.getPolicyId();
    waivedPolicyViolationDTO.policyName = policyViolation.getPolicyName();
    waivedPolicyViolationDTO.policyViolationId = policyViolation.getId();
    waivedPolicyViolationDTO.openTime = policyViolation.getOpenTime();
    waivedPolicyViolationDTO.waiveTime = policyViolation.getWaiveTime();
    if (policyViolation instanceof PolicyViolation pv) {
      waivedPolicyViolationDTO.fixTime = pv.getFixTime();
      waivedPolicyViolationDTO.legacyViolationTime = pv.getLegacyViolationTime();
    }
    waivedPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
    waivedPolicyViolationDTO.constraintViolations = PolicyViolationAdapter.convert(policyViolation);

    ApiPolicyWaiverDTO policyWaiverDTO;
    String policyWaiverId = policyViolation.getPolicyWaiverId();
    final PolicyWaiver policyWaiver = policyWaiverId != null ? policyWaiverDao.getById(policyWaiverId) : null;

    if (policyWaiver == null) {
      policyWaiverDTO = new ApiPolicyWaiverDTO();
      policyWaiverDTO.isObsolete = true;
      policyWaiverDTO.comment = POLICY_WAIVER_NOT_FOUND_MSG;
    }
    else {
      // Walking the hierarchy to determine if the policy waiver scope is still visible to the current owner's location.
      // This can occur if an app is moved to a different org than the original policy waiver was defined for.
      Owner waiverOwner = StreamSupport.stream(ownerDAO.walkHierarchy(ownerId).spliterator(), false /* parallel */)
          .filter(owner -> owner.getId().equals(policyWaiver.getOwnerId()))
          .findFirst()
          .orElse(null);

      if (waiverOwner != null) {
        policyWaiverDTO = ApiPolicyWaiverDTO.toDto(
            policyWaiver,
            waiverReasonIdToReasonMap.get(policyWaiver.getWaiverReasonId()),
            waiverOwner);
        policyWaiverDTO.isObsolete = false;
      }
      else {
        policyWaiverDTO = new ApiPolicyWaiverDTO();
        policyWaiverDTO.isObsolete = true;
        policyWaiverDTO.comment = POLICY_WAIVER_OWNER_NOT_IN_SCOPE_MSG;
      }

      policyWaiverDTO.policyWaiverId = policyWaiver.getId();
      policyWaiverDTO.createTime = policyWaiver.getCreateTime();
    }

    waivedPolicyViolationDTO.policyWaiver = policyWaiverDTO;

    return waivedPolicyViolationDTO;
  }

  private ApiPolicyViolationStageDTO buildPolicyViolationStageDTO(
      List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs)
  {
    ApiPolicyViolationStageDTO policyViolationStageDTO = new ApiPolicyViolationStageDTO();
    policyViolationStageDTO.stageId = Stage.ID_PROXY;
    policyViolationStageDTO.componentPolicyViolations = componentPolicyViolationDTOs;

    return policyViolationStageDTO;
  }

  private Map<ComponentIdentifier, List<RepositoryPolicyViolation>> getGroupedRepositoryPolicyViolationsByComponentIdentifier(
      List<RepositoryPolicyViolation> repositoryPolicyViolations)
  {
    return repositoryPolicyViolations.stream()
        .filter(r -> r.getComponentIdentifier() != null)
        .collect(Collectors.groupingBy(RepositoryPolicyViolation::getComponentIdentifier));
  }

  private Map<String, List<RepositoryPolicyViolation>> getGroupedRepositoryPolicyViolationsByHash(
      List<RepositoryPolicyViolation> repositoryPolicyViolations)
  {
    return repositoryPolicyViolations.stream()
        .filter(r -> r.getComponentIdentifier() == null)
        .collect(Collectors.groupingBy(RepositoryPolicyViolation::getHash));
  }
}
