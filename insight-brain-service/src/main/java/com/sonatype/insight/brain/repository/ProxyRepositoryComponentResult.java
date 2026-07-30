/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

public class ProxyRepositoryComponentResult
{
  private ComponentIdentifier identifier;

  private String sha1;

  public ProxyRepositoryComponentResult(final ComponentIdentifier identifier, final String sha1) {
    this.identifier = identifier;
    this.sha1 = sha1;
  }

  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  public void setIdentifier(final ComponentIdentifier identifier) {
    this.identifier = identifier;
  }

  public String getSha1() {
    return sha1;
  }

  public void setSha1(final String sha1) {
    this.sha1 = sha1;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProxyRepositoryComponentResult that = (ProxyRepositoryComponentResult) o;
    return Objects.equals(identifier, that.identifier) && Objects.equals(sha1, that.sha1);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, sha1);
  }
}
