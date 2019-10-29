/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.model.HasStringId;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationCloneService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationCloneService.class);

  private final OrganizationDAO orgDAO;

  private final ApplicationDAO appDAO;

  private final LabelDAO labelDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final ApiApplicationAdapter apiAppAdapter;

  @Inject
  public ApplicationCloneService(
      OrganizationDAO orgDAO,
      ApplicationDAO appDAO,
      LabelDAO labelDAO,
      ComponentLabelDAO componentLabelDAO,
      ApiApplicationAdapter apiAppAdapter)
  {
    this.orgDAO = orgDAO;
    this.appDAO = appDAO;
    this.labelDAO = labelDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.apiAppAdapter = apiAppAdapter;
  }

  public ApiApplicationDTO cloneApplication(String sourceAppId, String clonedAppName, String clonedAppPublicId) {
    long start = System.currentTimeMillis();

    AuditData.get().setData("sourceApplicationId", sourceAppId);

    try (TransactionContext tx = appDAO.createTransactionContext()) {
      tx.begin();

      Application sourceApp = appDAO.getByIdNotNull(tx, sourceAppId);
      log.info("Cloning application {} ({})...", sourceApp.getId(), sourceApp.getName());

      AuditData.get() //
          .setData("sourceApplicationPublicId", sourceApp.getPublicId()) //
          .setData("sourceApplicationName", sourceApp.getName()) //
          .setParentOrganization(orgDAO.getById(sourceApp.getOrganizationId()));

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
    cloneLabels(tx, sourceApp, clonedApp);

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

    AuditData.get().setApplicationWithDetails(clonedApp);

    return clonedApp;
  }

  private void cloneLabels(TransactionContext tx, Application sourceApp, Application clonedApp) {
    List<Label> labels = labelDAO.getByOwnerId(tx, sourceApp.getId());
    for (Label label : labels) {
      String sourceLabelId = label.getId();
      
      detachEntity(label);
      label.setOwnerId(clonedApp.getId());
      labelDAO.insert(tx, label);
      
      List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelId(tx, sourceLabelId);
      for (ComponentLabel componentLabel : componentLabels) {
        detachEntity(componentLabel);
        componentLabel.setLabelId(label.getId());
        componentLabel.setOwnerId(clonedApp.getId());
        componentLabelDAO.insert(tx, componentLabel);
      }

      log.info("Cloned label {} ({}) to label {} ({}).", //
          sourceLabelId, label.getLabel(), //
          label.getId(), label.getLabel());
    }
  }

  private <E extends HasStringId> void detachEntity(E entity) {
    PersistenceCapable pc = (PersistenceCapable) entity;
    pc.pcSetDetachedState(null);
    pc.pcReplaceStateManager(null);
    entity.setId(null);
  }
}
