/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;

/**
 * @since 1.152
 */
public class ProprietaryComponentNamePatternDTO
{
  public ProprietaryComponentNamePatternDTO() {
  }

  public ProprietaryComponentNamePatternDTO(ProprietaryComponentNamePattern proprietaryComponentNamePattern) {
    id = proprietaryComponentNamePattern.getId();
    format = proprietaryComponentNamePattern.getFormat();
    namespacePattern = proprietaryComponentNamePattern.getNamespacePattern();
    namePattern = proprietaryComponentNamePattern.getNamePattern();
    repositoryManagerInstanceId = proprietaryComponentNamePattern.getRepositoryManagerInstanceId();
    repositoryPublicId = proprietaryComponentNamePattern.getRepositoryPublicId();
    enabled = proprietaryComponentNamePattern.isEnabled();
  }

  public String id;

  public String format;

  public String namespacePattern;

  public String namePattern;

  public String repositoryManagerInstanceId;

  public String repositoryPublicId;

  public boolean enabled;

  @Override
  public String toString() {
    return "ProprietaryComponentNamePatternDTO [id=" + id + ", format=" + format + ", namespacePattern="
        + namespacePattern + ", namePattern=" + namePattern + ", repositoryManagerInstanceId="
        + repositoryManagerInstanceId + ", repositoryPublicId=" + repositoryPublicId + ", enabled=" + enabled + "]";
  }
}
