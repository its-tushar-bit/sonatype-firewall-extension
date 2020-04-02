/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.PackageUrlValueType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURL.StandardTypes;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.ANAME_QUALIFIER;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_ANAME;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_GOLANG;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_NPM;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_NUGET;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_RPM;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_RUBYGEMS;

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
    return "Coordinates were " + matchFact.getComponent().getDisplayNameFromIdentifier() + " (" +
        condition.getOperator() + " package URL " + condition.getValue() + ")";
  }

  @Override
  public String convertIfNeeded(final String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }
    new PackageUrlIdentifier(value);
    return wildcardPackageUrl(value);
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

  private String wildcardPackageUrl(String packageUrl) {
    PackageURLBuilder builder = PackageURLBuilder.aPackageURL();
    try {
      // Since package URL lower cases namespaces and names for some types which are case sensitive
      // Using generic type for wildcard conversion, so they are not changed and mixed cases are kept
      String format = StringUtils.substringBetween(packageUrl, ":", "/");
      String genericPackageUrl = StringUtils.replaceIgnoreCase(packageUrl, format, StandardTypes.GENERIC, 1);
      PackageURL newPackageUrl = new PackageURL(genericPackageUrl);
      builder.withType(StandardTypes.GENERIC);

      switch (format) {
        case FORMAT_MAVEN:
          wildCardMaven(builder, newPackageUrl);
          break;
        case FORMAT_PYPI:
          wildCardPypi(builder, newPackageUrl);
          break;
        case FORMAT_ANAME:
          wildCardAName(builder, newPackageUrl);
          break;
        case FORMAT_RUBYGEMS:
          wildCardRubygems(builder, newPackageUrl);
          break;
        case FORMAT_RPM:
          wildCardRpm(builder, newPackageUrl);
          break;
        case FORMAT_GOLANG:
          wildCardGolang(builder, newPackageUrl);
          break;
        case FORMAT_NPM:
          wildCardNpm(builder, newPackageUrl);
          break;
        case FORMAT_NUGET:
          wildCardNuget(builder, newPackageUrl);
          break;
        default:
          wildCardUnknown(builder, newPackageUrl);
      }
      genericPackageUrl = builder.build().canonicalize();
      return StringUtils.replaceIgnoreCase(genericPackageUrl, StandardTypes.GENERIC, format, 1);
    }
    catch (Exception e) {
      throw new InvalidPackageURLException("Invalid package url", e);
    }
  }

  private void wildCardMaven(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    wildcardNamespace(builder, packageUrl);
    wildcardVersion(builder, packageUrl);

    wildcardQualifier(builder, PackageUrlIdentifier.PURL_MAVEN_EXTENSION, packageUrl);
    wildcardQualifier(builder, PackageUrlIdentifier.PURL_MAVEN_CLASSIFIER, packageUrl);
  }

  private void wildCardPypi(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    addNamespaceIfExists(builder, packageUrl);
    wildcardVersion(builder, packageUrl);
    wildcardQualifier(builder, PackageUrlIdentifier.PURL_PYPI_EXTENSION, packageUrl);
    wildcardQualifier(builder, PackageUrlIdentifier.PURL_PYPI_QUALIFIER, packageUrl);
  }

  private void wildCardAName(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    addNamespaceIfExists(builder, packageUrl);
    wildcardVersion(builder, packageUrl);
    wildcardQualifier(builder, ANAME_QUALIFIER, packageUrl);
  }

  private void wildCardRpm(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    addNamespaceIfExists(builder, packageUrl);
    wildcardVersion(builder, packageUrl);
    wildcardQualifier(builder, PackageUrlIdentifier.PURL_RPM_ARCHITECTURE, packageUrl);
  }

  private void wildCardRubygems(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    addNamespaceIfExists(builder, packageUrl);
    wildcardVersion(builder, packageUrl);
    wildcardQualifier(builder, PackageUrlIdentifier.PURL_RUBYGEMS_PLATFORM, packageUrl);
  }

  private void wildCardGolang(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    addNamespaceIfExists(builder, packageUrl);
    wildcardVersion(builder, packageUrl);
  }

  private void wildCardNpm(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    addNamespaceIfExists(builder, packageUrl);
    wildcardVersion(builder, packageUrl);
  }

  private void wildCardNuget(PackageURLBuilder builder, PackageURL packageUrl) {
    addNamespaceIfExists(builder, packageUrl);
    wildcardName(builder, packageUrl);
    wildcardVersion(builder, packageUrl);
  }

  private void wildCardUnknown(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(packageUrl.getName());
    addNamespaceIfExists(builder, packageUrl);
    wildcardVersion(builder, packageUrl);

    Map<String, String> qualifiers = packageUrl.getQualifiers();
    if (qualifiers != null && !qualifiers.isEmpty()) {
      for (Entry<String, String> entry : qualifiers.entrySet()) {
        builder.withQualifier(entry.getKey(), entry.getValue());
      }
    }
  }

  private void wildcardVersion(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withVersion(wildcardElement(packageUrl.getVersion()));
  }

  private void wildcardQualifier(PackageURLBuilder builder, String coordinateName, PackageURL packageUrl) {
    Map<String, String> qualifiers = packageUrl.getQualifiers();
    if (qualifiers == null || qualifiers.isEmpty() || !qualifiers.containsKey(coordinateName)) {
      builder.withQualifier(coordinateName, ArtifactCoordinate.PLACEHOLDER);
    }
    else {
      builder.withQualifier(coordinateName, qualifiers.get(coordinateName));
    }
  }

  private void wildcardName(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withName(wildcardElement(packageUrl.getName()));
  }

  private void wildcardNamespace(PackageURLBuilder builder, PackageURL packageUrl) {
    builder.withNamespace(wildcardElement(packageUrl.getNamespace()));
  }

  private void addNamespaceIfExists(PackageURLBuilder builder, PackageURL packageUrl) {
    if (StringUtils.isNotBlank(packageUrl.getNamespace())) {
      builder.withNamespace(packageUrl.getNamespace());
    }
  }

  private String wildcardElement(String element) {
    String newElement;
    if (StringUtils.isNotBlank(element)) {
      newElement = element;
    }
    else {
      newElement = ArtifactCoordinate.PLACEHOLDER;
    }
    return newElement;
  }
}
