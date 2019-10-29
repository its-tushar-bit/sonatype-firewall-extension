/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationCloneService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationCloneService.class);

  private final ApplicationDAO appDAO;

  private final ApiApplicationAdapter apiAppAdapter;

  @Inject
  public ApplicationCloneService(ApplicationDAO appDAO, ApiApplicationAdapter apiAppAdapter) {
    this.appDAO = appDAO;
    this.apiAppAdapter = apiAppAdapter;
  }

  public ApiApplicationDTO cloneApplication(String sourceAppId, String clonedAppName, String clonedAppPublicId) {
    long start = System.currentTimeMillis();

    try (TransactionContext tx = appDAO.createTransactionContext()) {
      tx.begin();

      Application sourceApp = appDAO.getByIdNotNull(tx, sourceAppId);
      log.info("Cloning application {} ({})...", sourceApp.getId(), sourceApp.getName());

      checkAddApplicationPermission(sourceApp.getOrganizationId());

      ApiApplicationDTO clonedApp = cloneApplication(tx, sourceApp, clonedAppName, clonedAppPublicId);

      tx.commit();

      log.info("Cloned application {} ({}) to application {} ({}) in {} ms.", //
          sourceApp.getId(), sourceApp.getName(), //
          clonedApp.id, clonedApp.name, //
          System.currentTimeMillis() - start);
      return clonedApp;
    }
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  void checkAddApplicationPermission(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
  }

  private ApiApplicationDTO cloneApplication(
      TransactionContext tx,
      Application sourceApp,
      String clonedAppName,
      String clonedAppPublicId)
  {
    if (appDAO.getByName(tx, clonedAppName) != null) {
      throw new BadRequestException("An application with name '" + clonedAppName + "' already exists.");
    }
    if (appDAO.getByPublicId(tx, clonedAppPublicId) != null) {
      throw new BadRequestException("An application with public ID '" + clonedAppPublicId + "' already exists.");
    }

    Application clonedApp = createClonedApplication(tx, sourceApp, clonedAppName, clonedAppPublicId);

    return apiAppAdapter.convertToDTO(clonedApp);
  }

  private Application createClonedApplication(
      TransactionContext tx,
      Application sourceApp,
      String clonedAppName,
      String clonedAppPublicId)
  {
    Application clonedApp = new Application(clonedAppPublicId, clonedAppName, sourceApp.getOrganizationId());
    clonedApp.setContactInternalName(sourceApp.getContactInternalName());
    // Disable policy violation grandfathering in the cloned application.
    // If grandfathering is enabled, then all policy violations will be grandfathered when the first policy evaluation
    // happens.
    clonedApp.setPolicyViolationGrandfatheringEnabled(false);
    appDAO.insert(tx, clonedApp);

    return clonedApp;
  }
}
