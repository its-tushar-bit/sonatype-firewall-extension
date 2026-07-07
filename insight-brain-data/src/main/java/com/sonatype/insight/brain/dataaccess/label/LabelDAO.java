/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
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

import org.jooq.Field;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentLabel.COMPONENT_LABEL;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Label.LABEL;

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
    return tx.dsl()
        .selectFrom(LABEL)
        .where(LABEL.OWNER_ID.eq(ownerId))
        .orderBy(LABEL.LABEL_LOWERCASE)
        .fetchInto(Label.class);
  }

  public List<Label> getByOwnerIdWithHierarchy(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdWithHierarchy(tx, ownerId);
    }
  }

  public List<Label> getByOwnerIdWithHierarchy(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .select(LABEL.fields())
        .from(LABEL)
        .join(OWNER_ANCESTOR)
        .on(LABEL.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
        .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE, LABEL.LABEL_LOWERCASE)
        .fetchInto(Label.class);
  }

  public List<Label> getByOwnerIds(Collection<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIds(tx, ownerIds);
    }
  }

  public List<Label> getByOwnerIds(TransactionContext tx, Collection<String> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return Collections.emptyList();
    }
    return getListWithSqlInClause(ownerIds,
        ids -> tx.dsl()
            .selectFrom(LABEL)
            .where(LABEL.OWNER_ID.in(ids))
            .orderBy(LABEL.LABEL_LOWERCASE)
            .fetchInto(Label.class),
        getDataStore());
  }

  /**
   * Gets the labels applied to a component in a given context (org/app), inheritance is not considered. Note that the
   * supplied ownerId denotes the owner/scope of the component label, not the owner of the label definition (an
   * org-level label can be used for app-level component labels).
   *
   * @since 1.6
   */
  public List<Label> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(LABEL.fields())
          .from(LABEL)
          .join(COMPONENT_LABEL)
          .on(LABEL.LABEL_ID.eq(COMPONENT_LABEL.LABEL_ID))
          .where(COMPONENT_LABEL.OWNER_ID.eq(ownerId))
          .and(COMPONENT_LABEL.HASH.eq(hash))
          .orderBy(LABEL.LABEL_LOWERCASE)
          .fetchInto(Label.class);
    }
  }

  // Keyed by COMPONENT_LABEL.OWNER_ID.
  public Map<String, List<Label>> getByOwnerIdsAndHash(Collection<String> ownerIds, String hash) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return Collections.emptyMap();
    }
    try (TransactionContext tx = createTransactionContext()) {
      Field<String> componentOwnerField = COMPONENT_LABEL.OWNER_ID.as("component_owner_id");
      List<Field<?>> selectFields = new ArrayList<>(Arrays.asList(LABEL.fields()));
      selectFields.add(componentOwnerField);
      List<Map.Entry<String, Label>> ownerLabels = getListWithSqlInClause(ownerIds,
          chunk -> tx.dsl()
              .select(selectFields)
              .from(LABEL)
              .join(COMPONENT_LABEL)
              .on(LABEL.LABEL_ID.eq(COMPONENT_LABEL.LABEL_ID))
              .where(COMPONENT_LABEL.OWNER_ID.in(chunk))
              .and(COMPONENT_LABEL.HASH.eq(hash))
              .orderBy(LABEL.LABEL_LOWERCASE)
              .fetch(record -> Map.entry(record.get(componentOwnerField), record.into(Label.class))));
      Map<String, List<Label>> result = new HashMap<>();
      ownerLabels.forEach(entry -> result
          .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
          .add(entry.getValue()));
      return result;
    }
  }

  public Label getByOwnerIdAndLabel(TransactionContext tx, String ownerId, String label) {
    return tx.dsl()
        .selectFrom(LABEL)
        .where(LABEL.OWNER_ID.eq(ownerId))
        .and(LABEL.LABEL_LOWERCASE.eq(Label.normalizeLabel(label)))
        .fetchOneInto(Label.class);
  }

  public Label getByLabelWithHierarchy(String label, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(LABEL.fields())
          .from(LABEL)
          .join(OWNER_ANCESTOR)
          .on(LABEL.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
          .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
          .and(LABEL.LABEL_LOWERCASE.eq(Label.normalizeLabel(label)))
          .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE)
          .limit(1)
          .fetchOneInto(Label.class);
    }
  }

  @Override
  public void delete(TransactionContext tx, Label label) {
    ComponentLabelDAO componentLabelDAO = componentLabelDAOProvider.get();
    List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelId(tx, label.getId());
    for (ComponentLabel componentLabel : componentLabels) {
      componentLabelDAO.delete(tx, componentLabel);
    }
    tx.dsl()
        .deleteFrom(LABEL)
        .where(LABEL.LABEL_ID.eq(label.getId()))
        .execute();
    super.delete(tx, label); // Record search index change
  }

  private void validateLabelText(String label) {
    NameHelper.validate("Label name", label, MAX_NAME_SIZE);
  }

  @Override
  public int insert(TransactionContext tx, Label label) {
    validateLabelText(label.getLabel());
    validateLabelUnique(tx, label, false);
    validateLabelDescription(label.getDescription());
    validateLabelColor(label.getColor());

    return super.insert(tx, label);
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
  public int update(TransactionContext tx, Label label) {
    // If the label text hasn't changed, don't validate it. This check is for older labels that may have had non-alpha
    // numeric characters.
    Label existingLabel = getById(tx, label.getId());
    if (existingLabel == null || !existingLabel.getLabel().equals(label.getLabel())) {
      validateLabelText(label.getLabel());
    }
    validateLabelUnique(tx, label, true);
    validateLabelDescription(label.getDescription());
    validateLabelColor(label.getColor());

    return super.update(tx, label);
  }

  @Override
  protected SearchIndexChange newSearchIndexChange(Label entity) {
    return new SearchIndexChange(ChangeType.LABEL, entity.getId());
  }

  @Override
  public Table<?> getJooqTable() {
    return LABEL;
  }

  @Override
  public Class<Label> getEntityClass() {
    return Label.class;
  }
}
