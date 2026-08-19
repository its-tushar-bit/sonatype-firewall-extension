/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.dataaccess.TransactionContext;

public class LabelValueType
    implements ConditionValueType<Label>
{
  public static final String ID = "LabelValueType";

  private final TransactionContext tx;

  private final String ownerId;

  private final LabelDAO labelDAO;

  public LabelValueType(String ownerId, LabelDAO labelDAO) {
    this(null, ownerId, labelDAO);
  }

  public LabelValueType(TransactionContext tx, String ownerId, LabelDAO labelDAO) {
    this.tx = tx;
    this.ownerId = ownerId;
    this.labelDAO = labelDAO;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "Label";
  }

  @Override
  public boolean isAllowMultiple() {
    return false;
  }

  @Override
  public List<Label> getAvailableValues() {
    return tx != null ? labelDAO.getByOwnerIdWithHierarchy(tx, ownerId) : labelDAO.getByOwnerIdWithHierarchy(ownerId);
  }
}
