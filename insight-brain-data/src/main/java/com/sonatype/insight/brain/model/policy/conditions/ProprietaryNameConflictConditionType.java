/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.106
 */
@Singleton
@Named
public class ProprietaryNameConflictConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "ProprietaryNameConflict";

  public static final String OP_IS_PRESENT = "is present";

  public static final String OP_IS_NOT_PRESENT = "is not present";

  private static List<String> supportedOperators =
      Collections.unmodifiableList(Arrays.asList(OP_IS_PRESENT, OP_IS_NOT_PRESENT));

  private final RepositoryDAO repositoryDAO;

  @Inject
  public ProprietaryNameConflictConditionType(final RepositoryDAO repositoryDAO) {
    this.repositoryDAO = repositoryDAO;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Proprietary Name Conflict";
  }

  @Override
  public PolicyThreatCategory getThreatCategory() {
    return PolicyThreatCategory.SECURITY;
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return null;
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    Optional<ProprietaryComponentName> conflict = matchFact.getComponent().getConflictingProprietaryName();
    if (conflict.isPresent()) {
      ProprietaryComponentName name = conflict.get();
      Repository repository = repositoryDAO.getByIdNotNull(name.getRepositoryId());
      return "Component name conflicts with proprietary component " + name.getProprietaryNamePattern() + " from "
          + repository.getPublicId();
    }
    else {
      return "Component name does not conflict with any proprietary component";
    }
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
  }

  @Override
  public String getValueTypeId() {
    return null;
  }

  @Override
  protected boolean isApplicable(Component component) {
    return component.getConflictingProprietaryName() != null;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    boolean conflictPresent = component.getConflictingProprietaryName().isPresent();
    return OP_IS_PRESENT.equals(operator) ? conflictPresent : !conflictPresent;
  }
}
