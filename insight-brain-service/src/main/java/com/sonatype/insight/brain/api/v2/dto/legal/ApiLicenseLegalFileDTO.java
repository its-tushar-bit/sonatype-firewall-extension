/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Objects;

import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;

public class ApiLicenseLegalFileDTO
{
  public String id;

  public String relPath;

  public String content;

  public String originalContentHash;

  public ComponentLegalPartStatus status;

  public ApiLicenseLegalFileDTO() {
    // for jackson
  }

  public ApiLicenseLegalFileDTO(
      final String id,
      final String relPath,
      final String content,
      final String originalContentHash,
      final ComponentLegalPartStatus status)
  {
    this.id = id;
    this.relPath = relPath;
    this.content = content;
    this.originalContentHash = originalContentHash;
    this.status = status;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiLicenseLegalFileDTO that = (ApiLicenseLegalFileDTO) o;
    return Objects.equals(id, that.id) && Objects.equals(relPath, that.relPath) &&
        Objects.equals(content, that.content) &&
        Objects.equals(originalContentHash, that.originalContentHash) &&
        Objects.equals(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, relPath, content, originalContentHash, status);
  }

  @Override
  public String toString() {
    return "ApiLicenseLegalFileDTO{" +
        "id='" + id + '\'' +
        ", relPath='" + relPath + '\'' +
        ", content='" + content + '\'' +
        ", originalContentHash='" + originalContentHash + '\'' +
        ", status='" + status + '\'' +
        '}';
  }
}
