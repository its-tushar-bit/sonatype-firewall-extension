/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.dataaccess.TransactionContext;
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
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId, inherit);
    }
  }

  public List<Label> getByOwnerId(TransactionContext tx, String ownerId) {
    return getByOwnerId(tx, ownerId, false);
  }

  /**
   * @param inherit inherit boolean if {@code true} the returned list will include labels inherited from organization
   *          hierarchy
   */
  public List<Label> getByOwnerId(TransactionContext tx, String ownerId, boolean inherit) {
    final String sQuery = "SELECT label FROM Label label" + //
        " WHERE label.ownerId=?1" + //
        " ORDER BY label.labelLowercase";
    final List<Label> labels = new ArrayList<Label>();
    if (inherit) {
      final ApplicationDAO applicationDAO = new ApplicationDAO();
      final Application application = applicationDAO.getById(ownerId);
      if (application != null) {
        labels.addAll(getList(tx, sQuery, application.getOrganizationId()));
      }
    }
    labels.addAll(getList(tx, sQuery, ownerId));
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

  public Label getByOwnerIdAndLabelLowercase(TransactionContext tx, String ownerId, String labelLowercase) {
    return getByOwnerIdAndLabelLowercase(tx, ownerId, labelLowercase, false);
  }

  private Label getByOwnerIdAndLabelLowercase(TransactionContext tx, String ownerId, String labelLowercase, boolean inherit)
  {
    final String sQuery = "SELECT label FROM Label label" + //
        " WHERE  label.ownerId=?1 AND label.labelLowercase=?2";
    Label label = null;
    if (inherit) {
      final ApplicationDAO applicationDAO = new ApplicationDAO();
      final Application application = applicationDAO.getById(tx, ownerId);
      if (application != null) {
        label = get(tx, sQuery, application.getOrganizationId(), labelLowercase);
      }
    }
    if (label == null) {
      label = get(tx, sQuery, ownerId, labelLowercase);
    }
    return label;
  }

  @Override
  protected Label getById(TransactionContext tx, String id) {
    String sQuery = "SELECT label FROM Label label" + //
        " WHERE label.id=?1";
    return get(tx, sQuery, id);
  }

  Label getByIdNotNull(TransactionContext tx, String id) {
    Label label = getById(tx, id);
    if (label == null) {
      throw new NotFoundException("Cannot find a label with ID " + id + ".");
    }
    return label;
  }

  public Label getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  @Override
  public void delete(TransactionContext tx, Label label) {
    ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
    List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelId(tx, label.getId());
    for (ComponentLabel componentLabel : componentLabels) {
      componentLabelDAO.delete(tx, componentLabel);
    }
    super.delete(tx, label);
  }

  private void validateLabelText(String label) {
    NameHelper.validate("Label name", label, MAX_NAME_SIZE);
  }

  @Override
  public void insert(TransactionContext tx, Label label) {
    validateLabelText(label.getLabel());
    validateLabelUnique(tx, label, false);
    validateLabelDescription(label.getDescription());
    validateLabelColor(label.getColor());
    super.insert(tx, label);
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

  private void validateLabelUnique(TransactionContext tx, Label label, boolean update) throws InvalidLabelException {
    // igorf: references to other entities ain't exactly pretty, but I this LabelDAO is the right place to enforce
    // label uniqueness constraints
    final ApplicationDAO appDAO = new ApplicationDAO();
    final OrganizationDAO orgDAO = new OrganizationDAO();

    // first, check the same label does not exist in for the same owner
    // this is enforced by db unique key, but checking in java gives nicer error message
    Label otherLabel = getByOwnerIdAndLabelLowercase(tx, label.getOwnerId(), label.getLabelLowercase(), false);
    if (otherLabel != null && (!update || !otherLabel.getId().equals(label.getId()))) {
      final Application app = appDAO.getById(tx, label.getOwnerId());
      if (app != null) {
        final String message = String.format("A label with name '%s' already exists in application '%s'.",
            otherLabel.getLabel(), app.getName());
        throw new InvalidLabelException(message);
      }

      Organization org = orgDAO.getById(tx, label.getOwnerId());
      final String message = String.format("A label with name '%s' already exists in organization '%s'.",
          otherLabel.getLabel(), org.getName());
      throw new InvalidLabelException(message);
    }

    // owner can be an org, make sure none of org's apps have this label already
    final List<Application> apps = appDAO.getByOrganizationIdAndLabelLowercase(tx, label.getOwnerId(),
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
    final Application app = appDAO.getById(tx, label.getOwnerId());
    if (app != null) {
      otherLabel = getByOwnerIdAndLabelLowercase(tx, app.getOrganizationId(), label.getLabelLowercase(), false);
      if (otherLabel != null) {
        final Organization org = orgDAO.getById(tx, app.getOrganizationId());
        final String message = String.format("A label with name '%s' already exists in organization '%s'.",
            otherLabel.getLabel(), org.getName());
        throw new InvalidLabelException(message);
      }
    }
  }

  @Override
  public void update(TransactionContext tx, Label label) {
    // If the label text hasn't changed, don't validate it. This check is for older labels that may have had non-alpha
    // numeric characters.
    Label existingLabel = getById(tx, label.getId());
    if (existingLabel == null || !existingLabel.getLabel().equals(label.getLabel())) {
      validateLabelText(label.getLabel());
    }
    validateLabelUnique(tx, label, true);
    validateLabelDescription(label.getDescription());
    validateLabelColor(label.getColor());
    super.update(tx, label);
  }
}
