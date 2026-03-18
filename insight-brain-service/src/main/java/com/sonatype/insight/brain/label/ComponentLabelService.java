/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
public class ComponentLabelService
{
  private final LabelDAO labelDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final OwnerDAO ownerDAO;

  private final IdUtils idUtils;

  @Inject
  public ComponentLabelService(
      final LabelDAO labelDAO,
      final ComponentLabelDAO componentLabelDAO,
      final OwnerDAO ownerDAO,
      final IdUtils idUtils)
  {
    this.labelDAO = labelDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.ownerDAO = ownerDAO;
    this.idUtils = idUtils;
  }

  /**
   * Enables visualization of applied component labels. Most notably, the returned DTO holds the names of relevant
   * entities and public IDs as opposed to internal IDs to facilitate follow-up REST requests like deletion.
   */
  @Authorize(permission = Permission.READ)
  public AppliedLabels getComponentLabels(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final String hash)
  {
    return getComponentLabelsNoAuth(ownerType, ownerId, hash);
  }

  public AppliedLabels getComponentLabelsNoAuth(final OwnerType ownerType, String ownerId, final String hash) {
    AuditData.get().setComponentHash(hash);
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    AppliedLabels result = new AppliedLabels();

    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      String restOwnerId = ownerType == OwnerType.APPLICATION ? owner.getPublicId() : owner.getId();
      result.add(restOwnerId, owner.getName(), owner.getType(), labelDAO.getByOwnerIdAndHash(owner.getId(), hash));
    }

    return result;
  }

  /**
   * Assigns an existing label to a component identified by hash in a given context (org/app).
   */
  @Authorize(permission = Permission.WRITE)
  public void setComponentLabel(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String hash,
      Label label)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    label = labelDAO.getByIdNotNull(label.getId());
    ComponentLabel componentLabel = new ComponentLabel(internalOwnerId, label.getId(), hash);
    componentLabelDAO.insert(componentLabel);
    AuditData.get().setComponentHash(componentLabel.getHash()).setLabel(label);
  }

  /**
   * Deletes the component label given by the owning context and label id.
   */
  @Authorize(permission = Permission.WRITE)
  public void deleteComponentLabel(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String hash,
      final String labelId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    Label label = labelDAO.getByIdNotNull(labelId);
    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(internalOwnerId, hash, labelId);
    if (componentLabel == null) {
      throw new NotFoundException("Cannot find the label with ID " + labelId + " for " + ownerType + " ID " + ownerId
          + " on the component " + hash + ".");
    }
    componentLabelDAO.delete(componentLabel);
    AuditData.get().setComponentHash(componentLabel.getHash()).setLabel(label);
  }

  /**
   * Enumerates the labels applied to a given component in a way that allows the clients to identify at which point in
   * the organizational hierarchy the label has been applied.
   */
  public static class AppliedLabels
  {
    public List<LabelsByOwner> labelsByOwner = new ArrayList<>();

    void add(String ownerId, String ownerName, OwnerType ownerType, List<Label> labels) {
      if (labels == null || labels.isEmpty()) {
        return;
      }
      for (Label label : labels) {
        label.setOwnerId(ownerId);
      }
      LabelsByOwner lbo = new LabelsByOwner();
      lbo.ownerId = ownerId;
      lbo.ownerName = ownerName;
      lbo.ownerType = ownerType;
      lbo.labels = labels;
      labelsByOwner.add(lbo);
    }
  }

  /**
   * Enumerates the component labels contributed from a given context (app/org) along with basic identifying info about
   * the context itself, suitable for future REST requests to manage the component labels.
   *
   * @since 1.6
   */
  public static class LabelsByOwner
  {
    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public List<Label> labels;
  }
}
