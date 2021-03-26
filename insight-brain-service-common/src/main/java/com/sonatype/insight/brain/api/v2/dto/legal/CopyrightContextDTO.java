/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @since 1.108
 */
public class CopyrightContextDTO
{
  private String content;

  private List<String> filePaths;

  private Set<String> copyrightContentHashes;

  public CopyrightContextDTO() {
  }

  public CopyrightContextDTO(
      final String content,
      final Collection<String> filePaths,
      final Set<String> copyrightContentHashes)
  {
    this.content = content;
    this.filePaths = new ArrayList<>(filePaths);
    this.filePaths.sort(Comparator.naturalOrder());
    this.copyrightContentHashes = copyrightContentHashes;
  }

  public String getContent() {
    return content;
  }

  public List<String> getFilePaths() {
    return filePaths;
  }

  public Set<String> getCopyrightContentHashes() {
    return copyrightContentHashes;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final CopyrightContextDTO that = (CopyrightContextDTO) o;
    return Objects.equals(content, that.content) && Objects.equals(filePaths, that.filePaths) &&
        Objects.equals(copyrightContentHashes, that.copyrightContentHashes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, filePaths, copyrightContentHashes);
  }

  @Override
  public String toString() {
    return "CopyrightContextDTO{" +
        "content='" + content + '\'' +
        ", filePaths=" + filePaths +
        ", copyrightContentHashes=" + copyrightContentHashes +
        '}';
  }
}
