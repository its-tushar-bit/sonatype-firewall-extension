/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.repository.CascadeReevaluationTask;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing cascade re-evaluation operations across repository hierarchies.
 *
 * @since 1.196
 */
@Named
@Singleton
public class ApiFirewallCascadeService
{
  private static final Logger log = LoggerFactory.getLogger(ApiFirewallCascadeService.class);

  private final ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO;

  private final ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final CurrentUser currentUser;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ProductLicense productLicense;

  @Inject
  public ApiFirewallCascadeService(
      final ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO,
      final ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final CurrentUser currentUser,
      final RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      final ProductLicense productLicense)
  {
    this.reevaluateCascadeRequestDAO = reevaluateCascadeRequestDAO;
    this.reevaluateCascadeProgressDAO = reevaluateCascadeProgressDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.currentUser = currentUser;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.productLicense = productLicense;
  }

  /**
   * Initiates a cascade re-evaluation for the given component hash across all accessible repositories.
   */
  public CascadeReevaluateTicketDTO initiateCascadeReevaluation(final String componentHash) {
    checkProductLicense();
    checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);
    validateInputs(componentHash);

    String currentUsername = currentUser.getUserPrincipal().getUsername();

    ReevaluateCascadeRequest cascadeRequest =
        new ReevaluateCascadeRequest(componentHash, currentUsername, ReevaluateCascadeRequestStatus.PENDING);

    reevaluateCascadeRequestDAO.insert(cascadeRequest);

    String cascadeRequestId = cascadeRequest.getId();

    launchAsyncProcessing(cascadeRequestId, componentHash);

    CascadeReevaluateTicketDTO responseDTO = new CascadeReevaluateTicketDTO();
    responseDTO.statusUrl = PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/" + cascadeRequestId;

    log.info("Initiated cascade re-evaluation for component {}. Request ID: {}",
        componentHash, cascadeRequestId);

    return responseDTO;
  }

  private void validateInputs(final String componentHash) {
    if (StringUtils.isBlank(componentHash)) {
      throw new BadRequestException("Component hash is required");
    }
  }

  private void launchAsyncProcessing(final String cascadeRequestId, final String componentHash) {
    Executor executor = ExecutorThreadPools.getInstance().getThreadPool(ExecutorThreadPools.ThreadPools.GENERAL);

    AuditData.get().continueAsync(executor,
        new CascadeReevaluationTask(cascadeRequestId, componentHash,
            reevaluateCascadeProgressDAO, reevaluateCascadeRequestDAO,
            repositoryComponentDAO, repositoryPolicyEvaluator));
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkEvaluateComponentPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  private void checkProductLicense() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      throw new InvalidLicenseException();
    }
  }
}
