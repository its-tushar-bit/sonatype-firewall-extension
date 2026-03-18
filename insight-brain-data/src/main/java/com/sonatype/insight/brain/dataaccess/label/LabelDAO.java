/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class LabelDAO
    extends AbstractOperationalSqlDAO<Label>
{
  public static final int MAX_NAME_SIZE = 50;

  public static final int MAX_DESC_SIZE = 255;

  private final OrganizationDAO orgDAO;

  private final OwnerDAO ownerDAO;

  private final Provider<ComponentLabelDAO> componentLabelDAOProvider;

  private final ApplicationDAO appDAO;

  @Inject
  public LabelDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager,
      final OrganizationDAO orgDAO,
      final OwnerDAO ownerDAO,
      final Provider<ComponentLabelDAO> componentLabelDAOProvider,
      final ApplicationDAO appDAO)
  {
    super(operationalDataStore, searchIndexManager);
    this.orgDAO = orgDAO;
    this.ownerDAO = ownerDAO;
    this.componentLabelDAOProvider = componentLabelDAOProvider;
    this.appDAO = appDAO;
  }

  public List<Label> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<Label> getByOwnerId(TransactionContext tx, String ownerId) {
    final String sQuery = "SELECT label FROM Label label" + //
        " WHERE label.ownerId=?1" + //
        " ORDER BY label.labelLowercase";
    return getList(tx, sQuery, ownerId);
  }

  public List<Label> getByOwnerIdWithHierarchy(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdWithHierarchy(tx, ownerId);
    }
  }

  public List<Label> getByOwnerIdWithHierarchy(TransactionContext tx, String ownerId) {
    final List<Label> labels = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(tx, ownerId)) {
      labels.addAll(getByOwnerId(tx, owner.getId()));
    }
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

  public Label getByOwnerIdAndLabel(TransactionContext tx, String ownerId, String label) {
    final String sQuery = "SELECT label FROM Label label" + //
        " WHERE  label.ownerId=?1 AND label.labelLowercase=?2";
    return get(tx, sQuery, ownerId, Label.normalizeLabel(label));
  }

  public Label getByLabelWithHierarchy(String label, String ownerId) {
    Label entity = null;
    try (TransactionContext tx = createTransactionContext()) {
      for (Owner owner : ownerDAO.walkHierarchy(tx, ownerId)) {
        entity = getByOwnerIdAndLabel(tx, owner.getId(), label);
        if (entity != null) {
          break;
        }
      }
    }
    return entity;
  }

  @Override
  public void delete(TransactionContext tx, Label label) {
    ComponentLabelDAO componentLabelDAO = componentLabelDAOProvider.get();
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

    if (color.isLegacy()) {
      throw new InvalidLabelException("The label color " + color.toValue() + " is invalid.");
    }
  }

  private void validateLabelUnique(TransactionContext tx, Label label, boolean update) throws InvalidLabelException {
    // first, check the same label does not exist in for the same owner
    // this is enforced by db unique key, but checking in java gives nicer error message
    Label otherLabel = getByOwnerIdAndLabel(tx, label.getOwnerId(), label.getLabelLowercase());
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

    Owner owner = ownerDAO.getById(tx, label.getOwnerId());
    validateNameWithinHierarchyDown(tx, owner, label);
    validateNameWithinHierarchyUp(tx, owner.getParentOwnerId(), label);
  }

  private void validateNameWithinHierarchyDown(final TransactionContext tx, final Owner owner, final Label label) {
    Map<OwnerType, Set<String>> childrenWithDuplicatesByType = new EnumMap<>(OwnerType.class);
    getDuplicateLabels(tx, childrenWithDuplicatesByType, owner, label);
    if (!childrenWithDuplicatesByType.isEmpty()) {
      final StringBuilder message = new StringBuilder();
      message.append("A label with name '").append(label.getLabel()).append("' already exists in");

      for (OwnerType ownerType : childrenWithDuplicatesByType.keySet()) {
        Set<String> ownersWithDups = childrenWithDuplicatesByType.get(ownerType);
        message.append(" ").append(ownerType).append("(s)");
        for (String ownerWithDup : ownersWithDups) {
          message.append(" '").append(ownerWithDup).append('\'');
        }
      }
      message.append('.');
      throw new InvalidLabelException(message.toString());
    }
  }

  private void getDuplicateLabels(
      final TransactionContext tx,
      final Map<OwnerType, Set<String>> childrenWithDuplicatesByType,
      final Owner owner,
      final Label label)
  {

    if (!owner.canHaveChildren()) {
      return;
    }

    List<Owner> children = ownerDAO.getChildOwners(tx, owner);
    for (Owner child : children) {
      Label otherLabel = getByOwnerIdAndLabel(tx, child.getId(), label.getLabelLowercase());
      if (otherLabel != null) {
        getOwnersForType(childrenWithDuplicatesByType, child.getType()).add(child.getName());
      }
      getDuplicateLabels(tx, childrenWithDuplicatesByType, child, label);
    }
  }

  private void validateNameWithinHierarchyUp(final TransactionContext tx, final String parentId, final Label label) {
    if (parentId == null) {
      return;
    }
    Organization parentOrganization = orgDAO.getByIdNotNull(parentId);
    Label otherLabel = getByOwnerIdAndLabel(tx, parentOrganization.getId(), label.getLabelLowercase());
    if (otherLabel != null) {
      final String message = String.format("A label with name '%s' already exists in organization '%s'.",
          otherLabel.getLabel(), parentOrganization.getName());
      throw new InvalidLabelException(message);
    }

    validateNameWithinHierarchyUp(tx, parentOrganization.getParentOrganizationId(), label);
  }

  private Set<String> getOwnersForType(final Map<OwnerType, Set<String>> ownerNamesByTypeMap, final OwnerType type) {
    Set<String> ownerNames = ownerNamesByTypeMap.get(type);
    if (ownerNames == null) {
      ownerNames = new TreeSet<>();
      ownerNamesByTypeMap.put(type, ownerNames);
    }
    return ownerNames;
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

  @Override
  protected SearchIndexChange newSearchIndexChange(Label entity) {
    return new SearchIndexChange(ChangeType.LABEL, entity.getId());
  }
}
