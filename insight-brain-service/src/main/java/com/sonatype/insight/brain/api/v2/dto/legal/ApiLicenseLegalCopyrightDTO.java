/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Objects;

/**
 * @since 1.106
 */
public class ApiLicenseLegalCopyrightDTO
{
  public String id;

  public String content;

  public String originalContentHash;

  public ApiLicenseLegalCopyrightDTO() {
    //for jackson
  }

  public ApiLicenseLegalCopyrightDTO(final String id, final String content, final String originalContentHash) {
    this.id = id;
    this.content = content;
    this.originalContentHash = originalContentHash;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiLicenseLegalCopyrightDTO that = (ApiLicenseLegalCopyrightDTO) o;
    return Objects.equals(id, that.id) && Objects.equals(content, that.content) &&
        Objects.equals(originalContentHash, that.originalContentHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content, originalContentHash);
  }

  @Override
  public String toString() {
    return "ApiLicenseLegalCopyrightDTO{" +
        "id='" + id + '\'' +
        ", content='" + content + '\'' +
        ", originalContentHash='" + originalContentHash + '\'' +
        '}';
  }
}
