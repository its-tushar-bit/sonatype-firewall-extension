/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.error.exception.NotFoundException;

public class LabelDAO
    extends AbstractOperationalSqlDAO<Label>
{

  public static final int MAX_NAME_SIZE = 50;

  public static final int MAX_DESC_SIZE = 255;

  public List<Label> getByOwnerId(String ownerId) {
    return getByOwnerId(ownerId, false);
  }

  public List<Label> getByOwnerId(String ownerId, boolean inherit) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId, inherit);
    }
    finally {
      close(em);
    }
  }

  public List<Label> getByOwnerId(EntityManager em, String ownerId) {
    return getByOwnerId(em, ownerId, false);
  }

  /**
   * @param inherit inherit boolean if {@code true} the returned list will include labels inherited from organization
   *          hierarchy
   */
  public List<Label> getByOwnerId(EntityManager em, String ownerId, boolean inherit) {
    final String sQuery = "SELECT label FROM Label label" + //
        " WHERE label.ownerId=?1" + //
        " ORDER BY label.labelLowercase";
    final List<Label> labels = new ArrayList<Label>();
    if (inherit) {
      final ApplicationDAO applicationDAO = new ApplicationDAO();
      final Application application = applicationDAO.getById(ownerId);
      if (application != null) {
        labels.addAll(getList(em, sQuery, application.getOrganizationId()));
      }
    }
    labels.addAll(getList(em, sQuery, ownerId));
    return labels;
  }

  /**
   * Gets the labels applied to a component in a given context (org/app), inheritance is not considered. Note that the
   * supplied ownerId denotes the owner/scope of the component label, not the owner of the label definition (an
   * org-level label can be used for app-level component labels).
   * 
   * @since 1.6
   */
  public List<Label> getByOwnerIdAndHash(String ownerId, String hash) {
    final String sQuery = "SELECT label FROM Label label, ComponentLabel componentLabel" + //
        " WHERE label.id=componentLabel.labelId" + //
        " AND componentLabel.ownerId=?1 AND componentLabel.hash=?2" + //
        " ORDER BY label.labelLowercase";
    return getList(sQuery, ownerId, hash);
  }

  public Label getByOwnerIdAndLabelLowercase(EntityManager em, String ownerId, String labelLowercase) {
    return getByOwnerIdAndLabelLowercase(em, ownerId, labelLowercase, false);
  }

  private Label getByOwnerIdAndLabelLowercase(EntityManager em, String ownerId, String labelLowercase, boolean inherit)
  {
    final String sQuery = "SELECT label FROM Label label" + //
        " WHERE  label.ownerId=?1 AND label.labelLowercase=?2";
    Label label = null;
    if (inherit) {
      final ApplicationDAO applicationDAO = new ApplicationDAO();
      final Application application = applicationDAO.getById(em, ownerId);
      if (application != null) {
        label = get(em, sQuery, application.getOrganizationId(), labelLowercase);
      }
    }
    if (label == null) {
      label = get(em, sQuery, ownerId, labelLowercase);
    }
    return label;
  }

  @Override
  protected Label getById(EntityManager em, String id) {
    String sQuery = "SELECT label FROM Label label" + //
        " WHERE label.id=?1";
    return get(em, sQuery, id);
  }

  Label getByIdNotNull(EntityManager em, String id) {
    Label label = getById(em, id);
    if (label == null) {
      throw new NotFoundException("Cannot find a label with id " + id);
    }
    return label;
  }

  public Label getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }

  @Override
  public void delete(EntityManager em, Label label) {
    ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
    List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelId(em, label.getId());
    for (ComponentLabel componentLabel : componentLabels) {
      componentLabelDAO.delete(em, componentLabel);
    }
    super.delete(em, label);
  }

  private void validateLabelText(String label) {
    if (label == null || label.isEmpty()) {
      throw new InvalidLabelException("The label text cannot be null or empty");
    }
    if (label.contains(" ")) {
      throw new InvalidLabelException("The label text cannot contain spaces");
    }
    if (label.contains("\t")) {
      throw new InvalidLabelException("The label text cannot contain tabs");
    }
    if (label.length() > MAX_NAME_SIZE) {
      throw new InvalidLabelException("The label text must not exceed 50 characters");
    }
  }

  @Override
  public void insert(EntityManager em, Label label) {
    validateLabelText(label.getLabel());
    validateLabelUnique(em, label, false);
    validateLabelDescription(label.getDescription());
    validateLabelColor(label.getColor());
    super.insert(em, label);
  }

  private void validateLabelDescription(String description) {
    if (description != null && description.length() > MAX_DESC_SIZE) {
      throw new InvalidLabelException("The label description can't be longer than " + MAX_DESC_SIZE
          + " characters, the one supplied has " + description.length() + " characters. ");
    }
  }
  
  private void validateLabelColor(Color color) {
    if (color == null) {
      throw new InvalidLabelException("The label color must be assigned.");
    }
  }

  private void validateLabelUnique(EntityManager em, Label label, boolean update) throws InvalidLabelException {
    // igorf: references to other entities ain't exactly pretty, but I this LabelDAO is the right place to enforce
    // label uniqueness constraints
    final ApplicationDAO appDAO = new ApplicationDAO();
    final OrganizationDAO orgDAO = new OrganizationDAO();

    // first, check the same label does not exist in for the same owner
    // this is enforced by db unique key, but checking in java gives nicer error message
    Label otherLabel = getByOwnerIdAndLabelLowercase(em, label.getOwnerId(), label.getLabelLowercase(), false);
    if (otherLabel != null && (!update || !otherLabel.getId().equals(label.getId()))) {
      final Application app = appDAO.getById(em, label.getOwnerId());
      if (app != null) {
        final String message = String.format("A label with name '%s' already exists in application '%s'.",
            otherLabel.getLabel(), app.getName());
        throw new InvalidLabelException(message);
      }

      Organization org = orgDAO.getById(em, label.getOwnerId());
      final String message = String.format("A label with name '%s' already exists in organization '%s'.",
          otherLabel.getLabel(), org.getName());
      throw new InvalidLabelException(message);
    }

    // owner can be an org, make sure none of org's apps have this label already
    final List<Application> apps = appDAO.getByOrganizationIdAndLabelLowercase(em, label.getOwnerId(),
        label.getLabelLowercase());
    if (!apps.isEmpty()) {
      final StringBuilder message = new StringBuilder();
      message.append("A label with name '").append(label.getLabel()).append("' already exists in application(s)");
      for (Application app : apps) {
        message.append(" '").append(app.getName()).append('\'');
      }
      message.append('.');
      throw new InvalidLabelException(message.toString());
    }

    // owner can be an app, make sure organization does not have this label already
    final Application app = appDAO.getById(em, label.getOwnerId());
    if (app != null) {
      otherLabel = getByOwnerIdAndLabelLowercase(em, app.getOrganizationId(), label.getLabelLowercase(), false);
      if (otherLabel != null) {
        final Organization org = orgDAO.getById(em, app.getOrganizationId());
        final String message = String.format("A label with name '%s' already exists in organization '%s'.",
            otherLabel.getLabel(), org.getName());
        throw new InvalidLabelException(message);
      }
    }
  }

  @Override
  public void update(EntityManager em, Label label) {
    validateLabelText(label.getLabel());
    validateLabelUnique(em, label, true);
    validateLabelDescription(label.getDescription());
    validateLabelColor(label.getColor());
    super.update(em, label);
  }
}
