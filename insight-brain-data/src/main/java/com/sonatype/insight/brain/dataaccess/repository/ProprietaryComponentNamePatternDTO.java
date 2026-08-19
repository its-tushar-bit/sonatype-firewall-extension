/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

/**
 * @since 1.152
 */
public class ProprietaryComponentNamePatternDTO
{
  public ProprietaryComponentNamePatternDTO() {
  }

  public ProprietaryComponentNamePatternDTO(
      String id,
      String format,
      String namespacePattern,
      String namePattern,
      String repositoryManagerInstanceId,
      String repositoryManagerName,
      String repositoryPublicId,
      boolean enabled)
  {
    this.id = id;
    this.format = format;
    this.namespacePattern = namespacePattern;
    this.namePattern = namePattern;
    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryManagerName = repositoryManagerName;
    this.repositoryPublicId = repositoryPublicId;
    this.enabled = enabled;
  }

  public String id;

  public String format;

  public String namespacePattern;

  public String namePattern;

  public String repositoryManagerInstanceId;

  public String repositoryManagerName;

  public String repositoryPublicId;

  public boolean enabled;

  @Override
  public String toString() {
    return "ProprietaryComponentNamePatternDTO [id=" + id + ", format=" + format + ", namespacePattern="
        + namespacePattern + ", namePattern=" + namePattern + ", repositoryManagerInstanceId="
        + repositoryManagerInstanceId + ", repositoryPublicId=" + repositoryPublicId + ", enabled=" + enabled + "]";
  }
}
