/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory.client;

import java.util.ArrayList;
import java.util.List;

public class ArtifactoryChecksumSearchResults
{
  public List<ArtifactoryChecksumSearchResult> results = new ArrayList<>();

  public static ArtifactoryChecksumSearchResults create(String... uris) {
    ArtifactoryChecksumSearchResults results = new ArtifactoryChecksumSearchResults();
    for (String uri : uris) {
      ArtifactoryChecksumSearchResult result = new ArtifactoryChecksumSearchResult();
      result.uri = uri;
      results.results.add(result);
    }
    return results;
  }
}
