/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.consumption.ConsumptionContext;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaivedComponentUpgradeInspector
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(WaivedComponentUpgradeInspector.class);

  private final Configuration configuration;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final RepositoryDAO repositoryDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyDAO policyDAO;

  private final ApiComponentRemediationService apiComponentRemediationService;

  private final ProductLicense productLicense;

  private Map<String, Owner> ownersById;

  @Inject
  public WaivedComponentUpgradeInspector(
      Configuration configuration,
      OrganizationDAO organizationDAO,
      ApplicationDAO applicationDAO,
      RepositoryDAO repositoryDAO,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ApiComponentRemediationService apiComponentRemediationService,
      ProductLicense productLicense)
  {
    this.configuration = configuration;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.repositoryDAO = repositoryDAO;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.apiComponentRemediationService = apiComponentRemediationService;
    this.productLicense = productLicense;
  }

  @Override
  public void run() {
    log.info("Starting Waived Component Upgrade Inspector for tenant {}", TenantThreadLocal.getTenant());

    if (!configuration.getWaivedComponentUpgradeMonitoringEnabled()) {
      log.info("Could not run Waived Component Upgrade Inspector as upgrade monitoring is turned off");
      return;
    }

    try (ConsumptionContext.Scope consumptionCtx = ConsumptionContext.scopeBackgroundJob(productLicense)) {
      long start = System.currentTimeMillis();

      ownersById = getAllOwnersById();

      // Query by policy to reduce memory load while doing less database hits than querying by owners
      policyDAO.getAll().forEach(this::inspectWaiversForPolicy);

      log.info("Completed Waived Component Upgrade Inspector in {} ms for tenant {}",
          System.currentTimeMillis() - start, TenantThreadLocal.getTenant());
    }
  }

  private Map<String, Owner> getAllOwnersById() {
    Collector<Owner, ?, Map<String, Owner>> ownerCollector =
        Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);
    Map<String, Owner> owners = new HashMap<>();

    owners.putAll(applicationDAO.getAll().stream().collect(ownerCollector));
    owners.putAll(organizationDAO.getAll().stream().collect(ownerCollector));
    owners.putAll(repositoryDAO.getAll().stream().collect(ownerCollector));
    owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
    return owners;
  }

  private void inspectWaiversForPolicy(final Policy policy) {
    List<PolicyWaiver> waiversForRemediationInspection = getWaiversForRemediationInspection(policy);
    for (PolicyWaiver waiver : waiversForRemediationInspection) {
      try {
        if (waiverContainsUpgradeableComponent(waiver)) {
          waiver.setComponentUpgradeAvailable(true);
          if (SystemConfigurationPropertyFeature.EXPIRE_WAIVER_WHEN_REMEDIATION_AVAILABLE.isEnabled() &&
              waiver.isExpireWhenRemediationAvailable())
          {
            // Set expiry time to now in order to trigger auto expiry
            waiver.setExpiryTime(new Date());
          }
          policyWaiverDAO.update(waiver);
        }
      }
      catch (RuntimeException e) {
        log.error("Error when marking waiver as having component upgrade available. Waiver id: " + waiver.getId(), e);
      }
    }
  }

  private List<PolicyWaiver> getWaiversForRemediationInspection(final Policy policy) {
    Predicate<PolicyWaiver> componentUpgradeAvailableNotSet =
        policyWaiver -> policyWaiver.isComponentUpgradeAvailable() == null ||
            !policyWaiver.isComponentUpgradeAvailable();

    return policyWaiverDAO.getActiveByPolicyId(policy.getId())
        .stream()
        .filter(componentUpgradeAvailableNotSet)
        .filter(policyWaiver -> ComponentMatcherStrategyForWaiver.EXACT_COMPONENT.equals(
            policyWaiver.getComponentMatchStrategy()))
        .filter(policyWaiver -> policyWaiver.getAssociatedPackageUrl() != null)
        .collect(Collectors.toList());
  }

  private boolean waiverContainsUpgradeableComponent(final PolicyWaiver waiver) {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.packageUrl = waiver.getAssociatedPackageUrl();
    OwnerType waiverOwnerType = ownersById.get(waiver.getOwnerId()).getType();

    ApiComponentRemediationDTO suggestedRemediationForComponent =
        apiComponentRemediationService.getSuggestedRemediationForComponentNoAuthz(componentDTOV2, waiverOwnerType,
            waiver.getOwnerId(), null, null, null, null, false);

    return isRemediationAvailable(suggestedRemediationForComponent, waiver);
  }

  private static boolean isRemediationAvailable(
      final ApiComponentRemediationDTO suggestedRemediationForComponent,
      final PolicyWaiver waiver)
  {
    if (suggestedRemediationForComponent == null) {
      return false;
    }
    for (ApiVersionChangeOptionDTO versionChange : suggestedRemediationForComponent.remediation.versionChanges) {
      if (isRecommendationNotCurrentVersionAndIsNonViolatingVersion(waiver, versionChange)) {
        return true;
      }
    }

    return false;
  }

  /*
   * Since remediation api returns current version if it satisfies the recommendation strategy, we need to check that
   * the remediation does not suggest the same version that is already related to the waiver itself.
   */
  private static boolean isRecommendationNotCurrentVersionAndIsNonViolatingVersion(
      final PolicyWaiver waiver,
      final ApiVersionChangeOptionDTO versionChange)
  {
    return ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS.equals(versionChange.getType()) &&
        !isRecommendationSameVersionAsCurrentVersion(waiver, versionChange);
  }

  private static boolean isRecommendationSameVersionAsCurrentVersion(
      final PolicyWaiver waiver,
      final ApiVersionChangeOptionDTO versionChange)
  {
    return versionChange.getData().getComponent().packageUrl.equals(waiver.getAssociatedPackageUrl());
  }
}
