/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.PackageUrlValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.purl.InvalidPackageURLException;
import com.sonatype.insight.brain.purl.PackageUrlIdentifier;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.69
 */
public class PackageUrlConditionType
    extends AbstractComponentConditionType<String>
{
  public static final String ID = "Package URL";

  private static List<String> supportedOperators;

  static {
    supportedOperators = new ArrayList<>();
    supportedOperators.add("matches");
    supportedOperators.add("does not match");
    supportedOperators = Collections.unmodifiableList(supportedOperators);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Package URL";
  }

  @Override
  public List<String> getSupportedOperators() {
    return supportedOperators;
  }

  @Override
  public String generateDroolsConditionValue(TransactionContext tx, String value) {
    return asDroolsString(value);
  }

  @Override
  public String explainMatch(final Condition condition, final MatchFact matchFact) {
    return "Package URL was " + matchFact.getComponent().getDisplayName();
  }

  @Override
  public String convertIfNeeded(final String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }
    return new PackageUrlIdentifier(value).wildcardPackageUrl();
  }

  @Override
  public String getValueTypeId() {
    return PackageUrlValueType.ID;
  }

  @Override
  protected boolean internalEvaluateCondition(Component component, String operator, String value) {
    PackageUrlIdentifier packageUrl = new PackageUrlIdentifier(value);
    boolean match =
        new ArtifactCoordinate(packageUrl.toComponentIdentifier()).matches(component.getComponentIdentifier());
    return "matches".equals(operator) == match;
  }

  @Override
  public void validateCondition(TransactionContext tx, Condition condition, String ownerId)
      throws InvalidConditionException
  {
    try {
      String value = condition.getValue();
      if (StringUtils.isBlank(value)) {
        throw new InvalidConditionException(condition, "missing package URL");
      }
      super.validateCondition(tx, condition, ownerId);
    }
    catch (InvalidPackageURLException e) {
      throw new InvalidConditionException(condition, "invalid package URL");
    }
  }
}
