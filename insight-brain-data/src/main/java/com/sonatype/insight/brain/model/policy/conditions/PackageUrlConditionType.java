/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.PackageUrlValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.PackageURL.StandardTypes;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.69
 */
@Singleton
@Named
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
    return "Coordinates were " + matchFact.getComponent().getDisplayNameFromIdentifier() + " (" +
        condition.getOperator() + " package URL " + condition.getValue() + ")";
  }

  @Override
  public String convertIfNeeded(final String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }

    PackageUrlIdentifier originalPurl = new PackageUrlIdentifier(value);
    String genericPackageUrl;
    String format;

    if (value.contains(PackageUrlIdentifier.PURL_NEXUS_TYPE) && value.contains(StandardTypes.GENERIC)) {
      //It means it's already a generic purl and the format is part of the of the qualifiers
      format = originalPurl.getQualifiers().get(PackageUrlIdentifier.PURL_NEXUS_TYPE);
      genericPackageUrl = value;
    }
    else {
      // Since package URL lower cases namespaces and names for some types which are case sensitive
      // Using generic type for wildcard conversion, so they are not changed and mixed cases are kept
      format = originalPurl.getFormat();
      genericPackageUrl = StringUtils.replaceIgnoreCase(value, format, StandardTypes.GENERIC, 1);
    }
    return new PackageUrlIdentifier(genericPackageUrl).toWildcardedForm(format);
  }

  @Override
  public boolean isAutoUnquarantineSupported() {
    return false;
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

  @Override
  protected boolean isApplicable(Component component) {
    return super.isApplicable(component) || component.getComponentIdentifier() != null;
  }
}
