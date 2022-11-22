/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;

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
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.76
 */
public class ApiComponentsWithWaiversReportingService
{
  private static final String APPLICATION_COMPONENTS_AUDIT_KEY = "numberOfApplicationComponents";

  private static final String REPOSITORY_COMPONENTS_AUDIT_KEY = "numberOfRepositoryComponents";

  private static final String POLICY_WAIVER_NOT_FOUND_MSG = "Related policy waiver not found. Please re-evaluate.";

  private static final String POLICY_WAIVER_OWNER_NOT_IN_SCOPE_MSG =
      "Related policy waiver owner is not in scope. Please re-evaluate.";

  private final PolicyViolationLoader policyViolationLoader;

  private final RepositoryService repositoryService;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDao;

  private final PolicyWaiverDAO policyWaiverDao;

  private static final Logger log = LoggerFactory.getLogger(ApiComponentsWithWaiversReportingService.class);

  private final ApplicationService applicationService;

  private final OwnerDAO ownerDAO;

  @Inject
  public ApiComponentsWithWaiversReportingService(
      ApplicationService applicationService,
      PolicyViolationLoader policyViolationLoader,
      RepositoryService repositoryService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDao,
      PolicyWaiverDAO policyWaiverDao,
      OwnerDAO ownerDAO)
  {
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
    this.repositoryService = repositoryService;
    this.repositoryPolicyViolationDao = repositoryPolicyViolationDao;
    this.policyWaiverDao = policyWaiverDao;
    this.ownerDAO = ownerDAO;
  }

  public ApiComponentWaiversDTO getComponentsWithWaivers(String format) {
    final AtomicInteger componentsWithWaiversCount = new AtomicInteger(0);

    Predicate<AbstractPolicyViolation> statePredicate =
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED).asPolicyViolationPredicate();

    Predicate<AbstractPolicyViolation> formatPredicate = violation -> {
      ComponentIdentifier componentId = violation.getComponentIdentifier();
      return componentId != null && componentId.getFormat().equals(format);
    };

    Predicate<AbstractPolicyViolation> overallPredicate =
        format == null ? statePredicate : statePredicate.and(formatPredicate);

    List<Application> applications = applicationService.getApplications();
    Collection<ApplicationView> appViews =
        policyViolationLoader.getViolations(applications, null, false, overallPredicate);

    List<RepositoryDTO> repositoryDTOs = repositoryService.getRepositories().repositories;

    ApiComponentWaiversDTO componentWaiversDTO = new ApiComponentWaiversDTO();
    componentWaiversDTO.applicationWaivers = buildApplicationWaiverDTOs(appViews, componentsWithWaiversCount);
    componentWaiversDTO.repositoryWaivers =
        buildRepositoryWaiverDTOs(repositoryDTOs, overallPredicate, componentsWithWaiversCount);

    log.debug("getComponentsWithWaivers: Processed {} components with waived policy violations.",
        componentsWithWaiversCount);

    return componentWaiversDTO;
  }

  private List<ApiApplicationWaiverDTO> buildApplicationWaiverDTOs(
      Collection<ApplicationView> appViews,
      final AtomicInteger componentsWithWaiversCount)
  {
    List<ApiApplicationWaiverDTO> applicationWaiverDTOs = new ArrayList<>();
    final AtomicInteger applicationComponentsWithWaiversCount = new AtomicInteger(0);

    List<CompletableFuture<List<ApiApplicationWaiverDTO>>> dtoFutures = appViews.stream()
        .map(appView -> {
          return CompletableFuture.supplyAsync(() -> {
            Application app = appView.getApplication();

            List<PolicyViolation> allAppPolicyViolations = new ArrayList<>();
            List<ApiApplicationWaiverDTO> localDTOs = new ArrayList<>();

            ApiApplicationWaiverDTO applicationWaiverDTO = new ApiApplicationWaiverDTO();
            applicationWaiverDTO.application = buildApplicationBaseDTO(app);

            // We need to report on the latest evaluation for EACH stage.
            for (ApplicationStageView appStageView : appView.getStageViews()) {
              Collection<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
              if (policyViolations.isEmpty()) {
                continue;
              }
              allAppPolicyViolations.addAll(policyViolations);

              ApiPolicyViolationStageDTO policyViolationStageDTO =
                  buildPolicyViolationStageDTO(applicationWaiverDTO, appStageView.getStageType().getId());

              List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs = new ArrayList<>();
              policyViolationStageDTO.componentPolicyViolations = componentPolicyViolationDTOs;

              /*
               * Since our dto model consists of list of waived policy violations associated with a single
               * component identifier, we filter and group the waived policy violations by non-null component identifier
               * and process accordingly.
               */
              policyViolations.stream()
                  .filter(p -> p.getComponentIdentifier() != null)
                  .collect(Collectors.groupingBy(PolicyViolation::getComponentIdentifier))
                  .forEach((componentIdentifier, policyViolationsByComponent) -> {
                    applicationComponentsWithWaiversCount.incrementAndGet();

                    // Grab the first hash it should be the same for all violations
                    String hash = policyViolationsByComponent.get(0).getHash();
                    // for this component identifier create a dto list of all the waived policy violations
                    ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
                        buildComponentPolicyViolationDTO(policyViolationsByComponent, componentIdentifier, hash,
                            app.getId());

                    componentPolicyViolationDTOs.add(componentPolicyViolationDTO);
                  });

              // Filter and group policy violations by hash where the component identifier is null but does have a hash
              policyViolations.stream()
                  .filter(p -> p.getComponentIdentifier() == null && p.getHash() != null)
                  .collect(Collectors.groupingBy(PolicyViolation::getHash))
                  .forEach((hash, policyViolationsByHash) -> {
                    applicationComponentsWithWaiversCount.incrementAndGet();
                    // for this hash create a dto list of all the waived policy violations
                    ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
                        buildComponentPolicyViolationDTO(policyViolationsByHash, null, hash, app.getId());

                    componentPolicyViolationDTOs.add(componentPolicyViolationDTO);
                  });
            }

            if (!allAppPolicyViolations.isEmpty()) {
              localDTOs.add(applicationWaiverDTO);
            }
            return localDTOs;
          }, ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL));
        }).collect(toList());

    dtoFutures.stream().map(CompletableFuture::join).forEach(applicationWaiverDTOs::addAll);

    AuditData.get().setData(APPLICATION_COMPONENTS_AUDIT_KEY, applicationComponentsWithWaiversCount);
    componentsWithWaiversCount.addAndGet(applicationComponentsWithWaiversCount.get());

    return applicationWaiverDTOs;
  }

  private List<ApiRepositoryWaiverDTO> buildRepositoryWaiverDTOs(
      List<RepositoryDTO> repositoryDTOs,
      Predicate<? super RepositoryPolicyViolation> violationFilterPredicate,
      final AtomicInteger componentsWithWaiversCount)
  {
    List<ApiRepositoryWaiverDTO> repositoryWaiverDTOs = new ArrayList<>();
    MutableInt repositoryComponentsWithWaiversCount = new MutableInt(0);

    if (repositoryDTOs != null && !repositoryDTOs.isEmpty()) {
      Map<String, Repository> idToRepositoryMap = repositoryDTOs.stream()
          .collect(Collectors.toMap(repositoryDTO -> repositoryDTO.repository.getId(),
              repositoryDTO -> repositoryDTO.repository));

      List<RepositoryPolicyViolation> repositoryPolicyViolations =
          repositoryPolicyViolationDao.getActiveWaivedRepositoryPolicyViolations(idToRepositoryMap.keySet());

      repositoryPolicyViolations.stream()
          .filter(violationFilterPredicate)
          .collect(Collectors.groupingBy(RepositoryPolicyViolation::getRepositoryId))
          .forEach((repositoryId, policyViolations) -> {
            ApiRepositoryDTO repositoryDTO =
                buildRepositoryDTO(idToRepositoryMap.get(repositoryId));
            List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs = new ArrayList<>();

            // Filter and group policy violations by non-null component identifier
            getGroupedRepositoryPolicyViolationsByComponentIdentifier(policyViolations)
                .forEach((componentIdentifier, policyViolationsByComponent) -> {
                  repositoryComponentsWithWaiversCount.increment();
                  // Grab the first hash it should be the same for all violations
                  String hash = policyViolationsByComponent.get(0).getHash();
                  // for this component identifier a dto list of all the waived policy violations
                  ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
                      buildComponentPolicyViolationDTO(policyViolationsByComponent, componentIdentifier, hash,
                          repositoryId);

                  componentPolicyViolationDTOs.add(componentPolicyViolationDTO);
                });

            // Filter and group policy violations by hash where the component identifier is null
            getGroupedRepositoryPolicyViolationsByHash(policyViolations)
                .forEach((hash, policyViolationsByHash) -> {
                  repositoryComponentsWithWaiversCount.increment();
                  // for this hash create a dto list of all the waived policy violations
                  ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
                      buildComponentPolicyViolationDTO(policyViolationsByHash, null, hash, repositoryId);

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
      List<? extends AbstractPolicyViolation> policyViolations,
      ComponentIdentifier componentIdentifier,
      String hash,
      String ownerId)
  {
    ApiComponentPolicyViolationDTO componentPolicyViolationDTO = new ApiComponentPolicyViolationDTO();
    componentPolicyViolationDTO.component = buildComponentDTO(componentIdentifier, hash);

    componentPolicyViolationDTO.waivedPolicyViolations =
        policyViolations.stream()
            .map(policyViolation -> buildWaivedPolicyViolationDTO(policyViolation, ownerId))
            .collect(Collectors.toList());

    return componentPolicyViolationDTO;
  }

  private ApiApplicationBaseDTO buildApplicationBaseDTO(Application app) {
    ApiApplicationBaseDTO application = new ApiApplicationBaseDTO();
    application.id = app.getId();
    application.publicId = app.getPublicId();
    application.name = app.getName();
    application.organizationId = app.getOrganizationId();
    application.contactUserName = app.getContactInternalName();

    return application;
  }

  private ApiPolicyViolationStageDTO buildPolicyViolationStageDTO(
      ApiApplicationWaiverDTO applicationWaiverDTO,
      String stageId)
  {
    ApiPolicyViolationStageDTO policyViolationStageDTO = new ApiPolicyViolationStageDTO();
    policyViolationStageDTO.stageId = stageId;
    applicationWaiverDTO.stages.add(policyViolationStageDTO);

    return policyViolationStageDTO;
  }

  private ApiComponentDTOV2 buildComponentDTO(ComponentIdentifier componentIdentifier, String hash) {
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

  private ApiRepositoryDTO buildRepositoryDTO(Repository repository) {
    ApiRepositoryDTO repositoryDTO = new ApiRepositoryDTO();
    repositoryDTO.repositoryId = repository.getId();
    repositoryDTO.publicId = repository.getPublicId();
    repositoryDTO.format = repository.getFormat();

    return repositoryDTO;
  }

  private ApiWaivedPolicyViolationDTO buildWaivedPolicyViolationDTO(
      AbstractPolicyViolation policyViolation,
      String ownerId)
  {
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = new ApiWaivedPolicyViolationDTO();
    waivedPolicyViolationDTO.policyId = policyViolation.getPolicyId();
    waivedPolicyViolationDTO.policyName = policyViolation.getPolicyName();
    waivedPolicyViolationDTO.policyViolationId = policyViolation.getId();
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
      Owner waiverOwner = StreamSupport.stream(ownerDAO.walkHierarchy(ownerId).spliterator(), false)
          .filter(owner -> owner.getId().equals(policyWaiver.getOwnerId())).findFirst().orElse(null);
      if (waiverOwner != null) {
        policyWaiverDTO = ApiPolicyWaiverDTO.toDto(policyWaiver, waiverOwner);
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

  private Map<ComponentIdentifier, List<RepositoryPolicyViolation>>
        getGroupedRepositoryPolicyViolationsByComponentIdentifier(
      List<RepositoryPolicyViolation> repositoryPolicyViolations)
  {
    return repositoryPolicyViolations.stream()
        .filter(r -> r.getComponentIdentifier() != null)
        .collect(Collectors.groupingBy(RepositoryPolicyViolation::getComponentIdentifier));
  }

  private Map<String, List<RepositoryPolicyViolation>>
      getGroupedRepositoryPolicyViolationsByHash(List<RepositoryPolicyViolation> repositoryPolicyViolations)
  {
    return repositoryPolicyViolations.stream()
        .filter(r -> r.getComponentIdentifier() == null)
        .collect(Collectors.groupingBy(RepositoryPolicyViolation::getHash));
  }
}
