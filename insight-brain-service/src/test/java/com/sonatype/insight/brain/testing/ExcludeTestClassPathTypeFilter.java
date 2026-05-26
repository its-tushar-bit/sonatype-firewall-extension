/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import java.io.IOException;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Excludes classes loaded from test outputs and test jars from ambient component scanning.
 * Test-specific configurations must be added explicitly via builder.sources(...).
 */
public class ExcludeTestClassPathTypeFilter
    implements TypeFilter
{
  @Override
  public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
    String resourcePath = metadataReader.getResource().getURL().toString();
    return resourcePath.contains("/test-classes/") || resourcePath.contains("-tests.jar!/");
  }
}
