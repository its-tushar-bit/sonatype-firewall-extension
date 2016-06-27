/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.ArrayList;

import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.22.0
 */
@Named
public class ProprietaryConfigService
{
  private ProprietaryConfigDAO dao = new ProprietaryConfigDAO();

  private OwnerDAO ownerDAO = new OwnerDAO();

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  ProprietaryConfig getConfigApplicationEvaluator(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    return getConfig(OwnerType.APPLICATION, applicationPublicId);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  ProprietaryConfig getConfigComponentEvaluator(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    return getConfig(OwnerType.APPLICATION, applicationPublicId);
  }

  /**
   * NOTE: Permissions are NOT checked for this call
   */
  public ProprietaryConfig getConfig(OwnerType ownerType, String publicOwnerId) {
    String ownerId = IdUtils.getInternalOwnerId(ownerType, publicOwnerId);

    ProprietaryConfig result = new ProprietaryConfig();
    result.setPackages(new ArrayList<String>());
    result.setRegexes(new ArrayList<String>());

    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      com.sonatype.insight.brain.configuration.ProprietaryConfig ownerConfig = dao.getByOwnerId(owner.getId());
      if (ownerConfig != null) {
        result.getPackages().addAll(ownerConfig.getPackages());
        result.getRegexes().addAll(ownerConfig.getRegexes());
      }
    }

    return result;
  }

  public ProprietaryConfig getConfig(Goal goal, String applicationPublicId) {
    switch (goal) {
      case EVALUATE_APPLICATION:
        return getConfigApplicationEvaluator(applicationPublicId);
      case EVALUATE_COMPONENT:
        return getConfigComponentEvaluator(applicationPublicId);
      default:
        throw new BadRequestException("Proprietary Configuration requested for invalid goal: " + goal);
    }
  }
}
