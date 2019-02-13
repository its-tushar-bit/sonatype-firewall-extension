/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.48
 */
@Named
@Singleton
public class ApiComponentLabelServiceV2
{
  private final ComponentLabelDAO componentLabelDAO;

  private final LabelDAO labelDAO;

  @Inject
  public ApiComponentLabelServiceV2(final LabelDAO labelDAO, final ComponentLabelDAO componentLabelDAO) {
    this.labelDAO = labelDAO;
    this.componentLabelDAO = componentLabelDAO;
  }

  /**
   * Assigns an existing label to a component identified by hash in a given app.
   */
  @Authorize(permission = Permission.WRITE)
  public void setApplicationComponentLabel(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
                                           final String componentHash,
                                           final String labelName)
  {
    Label label = getLabel(applicationId, labelName);
    String truncatedComponentHash = HashHelper.truncateHash(componentHash);
    AuditData.get().setComponentHash(truncatedComponentHash).setLabel(label);
    ComponentLabel componentLabel = new ComponentLabel(applicationId, label.getId(), truncatedComponentHash);
    componentLabelDAO.insert(componentLabel);
  }

  /**
   * Deletes the component label identified by hash in a given app.
   */
  @Authorize(permission = Permission.WRITE)
  public void deleteApplicationComponentLabel(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
                                              final String componentHash,
                                              final String labelName)
  {
    Label label = getLabel(applicationId, labelName);
    String truncatedComponentHash = HashHelper.truncateHash(componentHash);
    AuditData.get().setComponentHash(truncatedComponentHash).setLabel(label);
    ComponentLabel componentLabel = componentLabelDAO
        .getByOwnerIdAndHashAndLabelId(applicationId, truncatedComponentHash, label.getId());
    componentLabelDAO.delete(componentLabel);
  }

  private Label getLabel(final String applicationId, final String labelName) {
    List<Label> labels = labelDAO.getByOwnerId(applicationId, true);
    for (Label label : labels) {
      if (label.getLabel().equalsIgnoreCase(labelName)) {
        return label;
      }
    }
    throw new NotFoundException(
        "Could not find a label with name '" + labelName + "' for application with ID " + applicationId + ".");
  }
}
