/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.purl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.model.policy.conditions.ArtifactCoordinate;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.*;

/**
 * Represents a <a href="https://github.com/package-url/purl-spec">purl-spec</a> identifier of a component
 */
public class PackageUrlIdentifier
{
  public static final String GENERIC_NAME = "name";

  public static final String GENERIC_NAMESPACE = "namespace";

  public static final String PURL_MAVEN_CLASSIFIER = MAVEN_CLASSIFIER;

  public static final String PURL_MAVEN_EXTENSION = "type";

  public static final String PURL_RPM_ARCHITECTURE = "arch";

  public static final String PURL_RUBYGEMS_PLATFORM = RUBYGEMS_PLATFORM;

  // There is no clear documentation on what are the exact purl pypi qualifiers param names. So sticking with our names
  public static final String PURL_PYPI_QUALIFIER = PYPI_QUALIFIER;

  public static final String PURL_PYPI_EXTENSION = PYPI_EXTENSION;

  private static final LinkedHashSet<String> KNOWN_NAME_IDENTIFIERS =
      new LinkedHashSet<>(Arrays.asList(GENERIC_NAME, "artifactId", "packageId"));

  private static final LinkedHashSet<String> KNOWN_NAMESPACE_IDENTIFIERS =
      new LinkedHashSet<>(Arrays.asList(GENERIC_NAMESPACE, "groupId"));

  private final PackageURL packageUrl;

  public static String toPackageUrl(final ComponentIdentifier componentIdentifier) {
    PackageUrlIdentifier packageURLIdentifier = fromComponentIdentifier(componentIdentifier);
    return packageURLIdentifier != null ? packageURLIdentifier.getPackageUrl() : null;
  }

  public static PackageUrlIdentifier fromComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }

    String format = componentIdentifier.getFormat();
    PackageURLBuilder builder = PackageURLBuilder.aPackageURL();
    switch (format) {
      case FORMAT_MAVEN:
        resolveMavenPackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      case FORMAT_NPM:
        resolveNpmPackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      case FORMAT_NUGET:
        resolveNugetPackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      case FORMAT_ANAME:
        resolveAnamePackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      case FORMAT_PYPI:
        resolvePypiPackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      case FORMAT_RPM:
        resolveRpmPackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      case FORMAT_RUBYGEMS:
        resolveRubyGemsPackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      case FORMAT_GOLANG:
        resolveGolangPackageUrl(componentIdentifier.getCoordinates(), builder);
        break;
      default:
        resolveGenericPackageUrl(format, new LinkedHashMap<>(componentIdentifier.getCoordinates()), builder);
        break;
    }
    try {
      return new PackageUrlIdentifier(builder.build());
    }
    catch (MalformedPackageURLException e) {
      throw new RuntimeException(e);
    }
  }

  public PackageUrlIdentifier(final String packageUrl) {
    try {
      this.packageUrl = new PackageURL(packageUrl);
    }
    catch (MalformedPackageURLException e) {
      throw new InvalidPackageURLException("Invalid package url", e);
    }
  }

  private PackageUrlIdentifier(final PackageURL packageUrl) {
    this.packageUrl = packageUrl;
  }

  public String getPackageUrl() {
    return packageUrl.canonicalize();
  }

  public ComponentIdentifier toComponentIdentifier() {
    String format = this.packageUrl.getType();
    switch (format) {
      case FORMAT_MAVEN:
        return createMavenIdentifier();
      case FORMAT_NPM:
        return createNpmIdentifier();
      case FORMAT_NUGET:
        return createNugetIdentifier();
      case FORMAT_ANAME:
        return createAnameIdentifier();
      case FORMAT_PYPI:
        return createPypiIdentifier();
      case FORMAT_RPM:
        return createRpmIdentifier();
      case FORMAT_RUBYGEMS:
        return createRubyGemsIdentifier();
      case FORMAT_GOLANG:
        return createGolangIdentifier();
      default:
        return createGenericIdentifier();
    }
  }

  public ComponentIdentifier ensureCompleteIdentifier() {
    ComponentIdentifier componentIdentifier = toComponentIdentifier();
    try {
      componentIdentifier.ensureComplete();
      return componentIdentifier;
    }
    catch (InvalidComponentIdentifierException e) {
      String message = e.getMessage();
      switch (componentIdentifier.getFormat()) {
        case FORMAT_MAVEN:
          message = message.replace(MAVEN_EXTENSION, PURL_MAVEN_EXTENSION);
          break;
        case FORMAT_RPM:
          message = message.replace(RPM_ARCHITECTURE, PURL_RPM_ARCHITECTURE);
          break;
        default:
          //noop
      }
      throw new InvalidPackageURLException(message, e);
    }
  }
  
  public String wildcardPackageUrl() {
    String format = this.packageUrl.getType();
    PackageURLBuilder builder = PackageURLBuilder.aPackageURL();
    switch (format) {
      case FORMAT_MAVEN:
        wildCardMaven(builder);
        break;
      case FORMAT_PYPI:
        wildCardPypi(builder);
        break;
      case FORMAT_ANAME:
        wildCardAName(builder);
        break;
      case FORMAT_RUBYGEMS:
        wildCardRubygems(builder);
        break;
      case FORMAT_RPM:
        wildCardRpm(builder);
        break;
      case FORMAT_GOLANG:
        wildCardGolang(builder);
        break;
      case FORMAT_NPM:
        wildCardNpm(builder);
        break;
      case FORMAT_NUGET:
        wildCardNuget(builder);
        break;
      default:
        wildCardUnknown(builder);
    }
    try {
      return new PackageUrlIdentifier(builder.build()).getPackageUrl();
    }
    catch (MalformedPackageURLException e) {
      throw new InvalidPackageURLException("Invalid package url", e);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PackageUrlIdentifier that = (PackageUrlIdentifier) o;
    return Objects.equals(packageUrl.canonicalize(), that.packageUrl.canonicalize());
  }

  @Override
  public int hashCode() {
    return Objects.hash(packageUrl.canonicalize());
  }

  @Override
  public String toString() {
    return "PackageUrlIdentifier{" +
        "packageUrl=" + packageUrl +
        '}';
  }

  private static void resolveGenericPackageUrl(final String format,
                                               final Map<String, String> coordinates,
                                               final PackageURLBuilder builder)
  {
    builder.withType(format);
    for (String namespaceIdentifier : KNOWN_NAMESPACE_IDENTIFIERS) {
      if (coordinates.containsKey(namespaceIdentifier)) {
        builder.withNamespace(coordinates.get(namespaceIdentifier));
        coordinates.remove(namespaceIdentifier);
        break;
      }
    }

    for (String nameIdentifier : KNOWN_NAME_IDENTIFIERS) {
      if (coordinates.containsKey(nameIdentifier)) {
        builder.withName(coordinates.get(nameIdentifier));
        coordinates.remove(nameIdentifier);
        break;
      }
    }

    if (coordinates.containsKey(VERSION)) {
      builder.withVersion(coordinates.get(VERSION));
      coordinates.remove(VERSION);
    }

    if (!coordinates.isEmpty()) {
      for (Entry<String, String> entry : coordinates.entrySet()) {
        builder.withQualifier(entry.getKey(), entry.getValue());
      }
    }
  }

  private static void resolveGolangPackageUrl(final Map<String, String> coordinates, final PackageURLBuilder builder) {
    builder.withType(FORMAT_GOLANG)
        .withVersion(coordinates.get(VERSION));
    resolveNameAndNamespace(builder, coordinates.get(GOLANG_NAME));
  }

  private static void resolveRubyGemsPackageUrl(final Map<String, String> coordinates,
                                                final PackageURLBuilder builder)
  {
    builder.withType(FORMAT_RUBYGEMS)
        .withVersion(coordinates.get(VERSION));
    resolveNameAndNamespace(builder, coordinates.get(RUBYGEMS_NAME));
    addQualiferIfExists(coordinates, builder, RUBYGEMS_PLATFORM, PURL_RUBYGEMS_PLATFORM);
  }

  private static void resolveRpmPackageUrl(final Map<String, String> coordinates, final PackageURLBuilder builder) {
    builder.withType(FORMAT_RPM)
        .withVersion(coordinates.get(VERSION));
    resolveNameAndNamespace(builder, coordinates.get(RPM_NAME));

    addQualiferIfExists(coordinates, builder, RPM_ARCHITECTURE, PURL_RPM_ARCHITECTURE);
  }

  private static void resolvePypiPackageUrl(final Map<String, String> coordinates, final PackageURLBuilder builder) {
    builder.withType(FORMAT_PYPI)
        .withVersion(coordinates.get(VERSION));
    resolveNameAndNamespace(builder, coordinates.get(PYPI_NAME));
    addQualiferIfExists(coordinates, builder, PYPI_QUALIFIER, PURL_PYPI_QUALIFIER);
    addQualiferIfExists(coordinates, builder, PYPI_EXTENSION, PURL_PYPI_EXTENSION);
  }

  private static void resolveAnamePackageUrl(final Map<String, String> coordinates, final PackageURLBuilder builder) {
    builder.withType(FORMAT_ANAME)
        .withVersion(coordinates.get(VERSION));
    resolveNameAndNamespace(builder, coordinates.get(ANAME_NAME));
    addQualiferIfExists(coordinates, builder, ANAME_QUALIFIER, ANAME_QUALIFIER);
  }

  private static void resolveNugetPackageUrl(final Map<String, String> coordinates, final PackageURLBuilder builder) {
    builder.withType(FORMAT_NUGET)
        .withVersion(coordinates.get(VERSION));
    resolveNameAndNamespace(builder, coordinates.get(NUGET_PACKAGE_ID));
  }

  private static void resolveNpmPackageUrl(final Map<String, String> coordinates, final PackageURLBuilder builder) {
    builder.withType(FORMAT_NPM)
        .withVersion(coordinates.get(VERSION));
    resolveNameAndNamespace(builder, coordinates.get(NPM_PACKAGE_ID));
  }

  private static void resolveMavenPackageUrl(Map<String, String> coordinates, final PackageURLBuilder builder) {
    builder.withType(FORMAT_MAVEN)
        .withName(coordinates.get(MAVEN_ARTIFACT_ID))
        .withNamespace(coordinates.get(MAVEN_GROUP_ID))
        .withVersion(coordinates.get(VERSION));
    addQualiferIfExists(coordinates, builder, MAVEN_EXTENSION, PURL_MAVEN_EXTENSION);
    addQualiferIfExists(coordinates, builder, MAVEN_CLASSIFIER, PURL_MAVEN_CLASSIFIER);
  }

  private ComponentIdentifier createGenericIdentifier() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(GENERIC_NAME, packageUrl.getName());
    coordinates.put(VERSION, packageUrl.getVersion());
    if (packageUrl.getNamespace() != null) {
      coordinates.put(GENERIC_NAMESPACE, packageUrl.getNamespace());
    }
    if (packageUrl.getQualifiers() != null) {
      coordinates.putAll(packageUrl.getQualifiers());
    }
    return new ComponentIdentifier(packageUrl.getType(), coordinates);
  }

  private ComponentIdentifier createGolangIdentifier() {
    return ComponentIdentifier
        .createGolangCoordinates(concat(packageUrl.getNamespace(), packageUrl.getName()), packageUrl.getVersion());
  }

  private ComponentIdentifier createRubyGemsIdentifier() {
    String platform = null;
    if (packageUrl.getQualifiers() != null) {
      platform = packageUrl.getQualifiers().get(PURL_RUBYGEMS_PLATFORM);
    }
    return ComponentIdentifier
        .createRubyGemsCoordinates(concat(packageUrl.getNamespace(), packageUrl.getName()), packageUrl.getVersion(),
            platform);
  }

  private ComponentIdentifier createRpmIdentifier() {
    String architecture = null;
    if (packageUrl.getQualifiers() != null) {
      architecture = packageUrl.getQualifiers().get(PURL_RPM_ARCHITECTURE);
    }
    return ComponentIdentifier
        .createRpmCoordinates(concat(packageUrl.getNamespace(), packageUrl.getName()), packageUrl.getVersion(),
            architecture);
  }

  private ComponentIdentifier createPypiIdentifier() {
    String qualifier = null;
    String extension = null;
    if (packageUrl.getQualifiers() != null) {
      qualifier = packageUrl.getQualifiers().get(PURL_PYPI_QUALIFIER);
      extension = packageUrl.getQualifiers().get(PURL_PYPI_EXTENSION);
    }
    return ComponentIdentifier
        .createPypiCoordinates(concat(packageUrl.getNamespace(), packageUrl.getName()), packageUrl.getVersion(),
            qualifier, extension);
  }

  private ComponentIdentifier createAnameIdentifier() {
    String qualifier = null;
    if (packageUrl.getQualifiers() != null) {
      qualifier = packageUrl.getQualifiers().get(ANAME_QUALIFIER);
    }
    return ComponentIdentifier
        .createAnameCoordinates(concat(packageUrl.getNamespace(), packageUrl.getName()), qualifier,
            packageUrl.getVersion());
  }

  private ComponentIdentifier createNugetIdentifier() {
    return ComponentIdentifier
        .createNugetCoordinates(concat(packageUrl.getNamespace(), packageUrl.getName()), packageUrl.getVersion());
  }

  private ComponentIdentifier createNpmIdentifier() {
    return ComponentIdentifier
        .createNpmCoordinates(concat(packageUrl.getNamespace(), packageUrl.getName()), packageUrl.getVersion());
  }

  private ComponentIdentifier createMavenIdentifier() {
    String classifier = null;
    String extension = null;
    if (packageUrl.getQualifiers() != null) {
      classifier = packageUrl.getQualifiers().get(PURL_MAVEN_CLASSIFIER);
      extension = packageUrl.getQualifiers().get(PURL_MAVEN_EXTENSION);
    }
    return ComponentIdentifier.createMavenCoordinates(packageUrl.getNamespace(), packageUrl.getName(),
        packageUrl.getVersion(), classifier, extension);
  }

  private String concat(final String part1, final String part2) {
    StringBuilder builder = new StringBuilder();
    if (StringUtils.isNotBlank(part1)) {
      builder.append(part1).append("/");
    }
    if (StringUtils.isNotBlank(part2)) {
      builder.append(part2);
    }
    return builder.toString();
  }

  private static void addQualiferIfExists(final Map<String, String> coordinates,
                                          final PackageURLBuilder builder,
                                          final String coordinateName,
                                          final String purlCoordinateName)
  {
    if (coordinates.get(coordinateName) != null && !coordinates.get(coordinateName).isEmpty()) {
      builder.withQualifier(purlCoordinateName, coordinates.get(coordinateName));
    }
  }

  private static void resolveNameAndNamespace(final PackageURLBuilder builder, String packageId) {
    // strip off any leading or trailing slashes
    packageId = packageId.replaceAll("(^/+)|(/+$)", "");
    String namespace = null;
    if (packageId.contains("/")) {
      namespace = packageId.substring(0, packageId.lastIndexOf("/"));
      packageId = packageId.substring(packageId.lastIndexOf("/") + 1);
    }
    builder.withName(packageId);
    if (namespace != null) {
      builder.withNamespace(namespace);
    }
  }

  private void wildCardMaven(PackageURLBuilder builder) {
    builder.withType(FORMAT_MAVEN).withName(packageUrl.getName());
    wildcardNamespace(builder);
    wildcardVersion(builder);

    wildcardQualifier(builder, PURL_MAVEN_EXTENSION);
    wildcardQualifier(builder, PURL_MAVEN_CLASSIFIER);
  }

  private void wildCardPypi(PackageURLBuilder builder) {
    builder.withType(FORMAT_PYPI).withName(packageUrl.getName());

    wildcardVersion(builder);
    wildcardQualifier(builder, PURL_PYPI_EXTENSION);
    wildcardQualifier(builder, PURL_PYPI_QUALIFIER);
  }

  private void wildCardAName(PackageURLBuilder builder) {
    builder.withType(FORMAT_ANAME).withName(packageUrl.getName());
    addNamespaceIfExists(builder);
    wildcardVersion(builder);
    wildcardQualifier(builder, ANAME_QUALIFIER);
  }
  
  private void wildCardRpm(PackageURLBuilder builder) {
    builder.withType(FORMAT_RPM).withName(packageUrl.getName());
    addNamespaceIfExists(builder);
    wildcardVersion(builder);
    wildcardQualifier(builder, PURL_RPM_ARCHITECTURE);
  }

  private void wildCardRubygems(PackageURLBuilder builder) {
    builder.withType(FORMAT_RUBYGEMS).withName(packageUrl.getName());
    addNamespaceIfExists(builder);
    wildcardVersion(builder);
    wildcardQualifier(builder, PURL_RUBYGEMS_PLATFORM);
  }

  private void wildCardGolang(PackageURLBuilder builder) {
    builder.withType(FORMAT_GOLANG).withName(packageUrl.getName());
    addNamespaceIfExists(builder);
    wildcardVersion(builder);
  }

  private void wildCardNpm(PackageURLBuilder builder) {
    builder.withType(FORMAT_NPM).withName(packageUrl.getName());
    addNamespaceIfExists(builder);
    wildcardVersion(builder);
  }

  private void wildCardNuget(PackageURLBuilder builder) {
    builder.withType(FORMAT_NUGET);
    addNamespaceIfExists(builder);
    wildcardName(builder);
    wildcardVersion(builder);
  }

  private void wildCardUnknown(PackageURLBuilder builder) {
    builder.withType(packageUrl.getType()).withName(packageUrl.getName());
    addNamespaceIfExists(builder);
    wildcardVersion(builder);

    Map<String, String> qualifiers = packageUrl.getQualifiers();
    if (qualifiers != null && !qualifiers.isEmpty()) {
      for (Entry<String, String> entry : qualifiers.entrySet()) {
        builder.withQualifier(entry.getKey(), entry.getValue());
      }
    }
  }

  private void wildcardVersion(PackageURLBuilder builder) {
    builder.withVersion(wildcardElement(packageUrl.getVersion()));
  }
  
  private void wildcardQualifier(PackageURLBuilder builder, String coordinateName) {
    Map<String, String> qualifiers = packageUrl.getQualifiers();
    if (qualifiers == null || qualifiers.isEmpty() || !qualifiers.containsKey(coordinateName)) {
      builder.withQualifier(coordinateName, ArtifactCoordinate.PLACEHOLDER);
    }
    else {
      builder.withQualifier(coordinateName, qualifiers.get(coordinateName));
    }
  }

  private void wildcardName(PackageURLBuilder builder) {
    builder.withName(wildcardElement(packageUrl.getName()));
  }

  private void wildcardNamespace(PackageURLBuilder builder) {
    builder.withNamespace(wildcardElement(packageUrl.getNamespace()));
  }
  
  private void addNamespaceIfExists(PackageURLBuilder builder) {
    if (!StringUtils.isBlank(packageUrl.getNamespace())) {
      builder.withNamespace(packageUrl.getNamespace());
    }
  }

  private String wildcardElement(String element) {
    String newElement;
    if (!StringUtils.isBlank(element)) {
      newElement = element;
    }
    else {
      newElement = ArtifactCoordinate.PLACEHOLDER;
    }
    return newElement;
  }
}
