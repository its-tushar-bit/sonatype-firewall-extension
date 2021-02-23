/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.license.model.ProductLicenseDetails.PRODUCT_FIREWALL;
import static com.sonatype.insight.license.model.ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION;

/**
 * @since 1.17.0
 */
@Named
@Singleton
public class RepositoryService extends AbstractRepositoryService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

  @Inject
  public RepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator,
                           ProprietaryComponentNameDetector proprietaryComponentNameDetector,
                           ProductLicense productLicense,
                           PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    super(repositoryPolicyEvaluator, proprietaryComponentNameDetector, productLicense, policyViolationLoggerFactory,
        LicensedFeature.FIREWALL);
  }

  /**
   * @since 1.89
   */
  RepositoryComponentEvaluationDataList evaluateComponentsAdhoc(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      String clientUserAgent)
  {
    checkLicenseProduct();
    auditRepoComponentEvalList(componentEvaluationDataRequestList);
    Repository repository = getOrCreateRepository(repositoryManagerInstanceId, repositoryPublicId);

    log.debug("Evaluating components for repository {}:{} ({})", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());

    return evaluateComponents(repository, repositoryManagerInstanceId, componentEvaluationDataRequestList,
        false, false, clientUserAgent);
  }

  private Repository getOrCreateRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    RepositoryDAO repositoryDAO = new RepositoryDAO();
    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManagerInstanceId, repositoryPublicId);

    if (repository == null) {
      repository = new Repository(null, repositoryPublicId);
      RepositoryManager repositoryManager = getOrCreateRepositoryManager(repositoryManagerInstanceId);
      repository.setRepositoryManagerId(repositoryManager.getId());
      repository.setEnabled(false);
      repositoryDAO.insert(repository);
    }
    AuditData.get().setRepository(repository);

    return repository;
  }

  private void checkLicenseProduct() {
    if (!(productLicense.hasProduct(PRODUCT_RISK_AND_REMEDIATION) || productLicense.hasProduct(PRODUCT_FIREWALL))) {
      throw new InvalidLicenseException();
    }
  }
}
