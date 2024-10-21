/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
@Singleton
public class PolicyEvaluationUtil
{
  private final ProductLicense productLicense;

  private final StageTypeService stageTypeService;

  @Inject
  public PolicyEvaluationUtil(final ProductLicense productLicense, final StageTypeService stageTypeService) {
    this.productLicense = productLicense;
    this.stageTypeService = stageTypeService;
  }

  public void validateEvaluationTypeAndFeature(IntegrationType integrationType, Stage stage) {
    if (integrationType.equals(IntegrationType.CLI)) {
      productLicense.validateFeature(LicensedFeature.CLI_INTEGRATION);
    }
    else if (integrationType.equals(IntegrationType.CI)) {
      productLicense.validateFeature(LicensedFeature.CI_INTEGRATION);
    }
    else if (integrationType.equals(IntegrationType.RM)) {
      productLicense.validateFeature(LicensedFeature.RM_STAGING_INTEGRATION);
    }

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException(stage.getStageTypeId());
    }

    if (!stageTypeService.getLicensedStageTypes().contains(StageTypes.getById(stage.getStageTypeId()))) {
      throw new InvalidLicenseException("Stage '" + stage.getStageTypeId() + "' is not supported by your license.");
    }
  }
}
