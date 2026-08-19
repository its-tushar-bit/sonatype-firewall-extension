/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;

/**
 * A generated support bundle backed by a file on disk. The support ZIP is written straight to
 * {@link #supportInfoFile} so that large bundles (e.g. a tenant with a multi-GiB waiver set) do
 * not have to fit in a single {@code byte[]}, which is bounded by {@code Integer.MAX_VALUE - 8}.
 */
public class SupportInfo
{
  private final File supportInfoFile;

  private final String supportInfoName;

  public SupportInfo(File supportInfoFile, String supportInfoName) {
    this.supportInfoFile = supportInfoFile;
    this.supportInfoName = supportInfoName;
  }

  public File getSupportInfoFile() {
    return supportInfoFile;
  }

  public String getSupportInfoName() {
    return supportInfoName;
  }
}
