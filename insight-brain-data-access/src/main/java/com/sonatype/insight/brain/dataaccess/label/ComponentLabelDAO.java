/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.error.exception.BadRequestException;

public class ComponentLabelDAO
    extends AbstractOperationalSqlDAO<ComponentLabel>
{
  @Override
  protected ComponentLabel getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM ComponentLabel entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public List<ComponentLabel> getByLabelId(EntityManager em, String labelId) {
    String sQuery = "SELECT entity FROM ComponentLabel entity" + //
        " WHERE entity.labelId=?1";
    return getList(em, sQuery, labelId);
  }

  public List<ComponentLabel> getByOwnerIdAndHash(String ownerId, String hash) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerIdAndHash(em, ownerId, hash);
    }
    finally {
      close(em);
    }
  }

  public List<ComponentLabel> getByOwnerIdAndHash(EntityManager em, String ownerId, String hash) {
    final String sQuery = "SELECT label FROM ComponentLabel label" + //
        " WHERE label.ownerId=?1 AND label.hash=?2";
    final ApplicationDAO applicationDAO = new ApplicationDAO();
    final Application application = applicationDAO.getById(em, ownerId);
    final List<ComponentLabel> labels = new ArrayList<ComponentLabel>();
    if (application != null && application.getOrganizationId() != null) {
      labels.addAll(getList(em, sQuery, application.getOrganizationId(), hash));
    }
    labels.addAll(getList(em, sQuery, ownerId, hash));
    return labels;
  }

  /**
   * Gets the component label applied to a given component and context (org/app) using the specified label.
   * 
   * @since 1.6
   */
  public ComponentLabel getByOwnerIdAndHashAndLabelId(String ownerId, String hash, String labelId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerIdAndHashAndLabelId(em, ownerId, hash, labelId);
    }
    finally {
      close(em);
    }
  }

  private ComponentLabel getByOwnerIdAndHashAndLabelId(EntityManager em, String ownerId, String hash, String labelId) {
    String sQuery = "SELECT entity FROM ComponentLabel entity" + //
        " WHERE entity.ownerId=?1 AND entity.hash=?2 AND entity.labelId=?3";
    return get(em, sQuery, ownerId, hash, labelId);
  }

  @Override
  public void insert(EntityManager em, ComponentLabel entity) {
    validate(em, entity);
    super.insert(em, entity);
  }

  @Override
  public void update(EntityManager em, ComponentLabel entity) {
    validate(em, entity);
    super.update(em, entity);
  }

  private void validate(EntityManager em, ComponentLabel entity) {
    LabelDAO labelDAO = new LabelDAO();
    Label label = labelDAO.getByIdNotNull(em, entity.getLabelId());
    ComponentLabel other = getByOwnerIdAndHashAndLabelId(em, entity.getOwnerId(), entity.getHash(), entity.getLabelId());
    if (other != null && !other.getId().equals(entity.getId())) {
      throw new BadRequestException("The label '" + label.getLabel() + "' is already applied to the component "
          + entity.getHash());
    }
    if (!isLabelApplicable(em, label, entity.getOwnerId(), labelDAO)) {
      throw new BadRequestException("The label '" + label.getLabel() + "' is not applicable for the selected context "
          + entity.getOwnerId());
    }
  }

  private boolean isLabelApplicable(EntityManager em, Label label, String ownerId, LabelDAO labelDAO) {
    if (label.getOwnerId().equals(ownerId)) {
      return true;
    }
    for (Label applicable : labelDAO.getByOwnerId(em, ownerId, true)) {
      if (applicable.getId().equals(label.getId())) {
        return true;
      }
    }
    return false;
  }
}
