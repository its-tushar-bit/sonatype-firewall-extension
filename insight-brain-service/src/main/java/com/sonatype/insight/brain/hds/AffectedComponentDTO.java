/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * DTO representing a component affected by a CVE vulnerability. Maps to the response from HDS endpoint: GET
 * /rest/vulnerability/cve/{cveId}
 */
public class AffectedComponentDTO
{
  private String format;

  private String namespace;

  private String name;

  private String version;

  public AffectedComponentDTO() {
    // Default constructor for Jackson
  }

  public AffectedComponentDTO(
      String format,
      String namespace,
      String name,
      String version)
  {
    this.format = format;
    this.namespace = namespace;
    this.name = name;
    this.version = version;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "AffectedComponentDTO{" +
        "format='" + format + '\'' +
        ", namespace='" + namespace + '\'' +
        ", name='" + name + '\'' +
        ", version='" + version +
        '}';
  }

  public boolean equalByComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null || !Objects.equals(format, componentIdentifier.getFormat())) {
      return false;
    }

    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

    if (!Objects.equals(name, purlIdentifier.getName()) ||
        !Objects.equals(version, purlIdentifier.getVersion())) {
      return false;
    }

    return isNamespaceEqual(namespace, purlIdentifier.getNamespace());
  }

  private boolean isNamespaceEqual(String namespace1, String namespace2) {
    String ns1 = (namespace1 == null || namespace1.isEmpty()) ? null : namespace1;
    String ns2 = (namespace2 == null || namespace2.isEmpty()) ? null : namespace2;
    return Objects.equals(ns1, ns2);
  }
}
