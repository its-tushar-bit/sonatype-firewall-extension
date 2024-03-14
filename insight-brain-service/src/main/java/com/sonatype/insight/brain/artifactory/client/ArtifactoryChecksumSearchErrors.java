/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory.client;

import java.util.ArrayList;
import java.util.List;

public class ArtifactoryChecksumSearchErrors
{
  public List<ArtifactoryChecksumSearchError> errors = new ArrayList<>();

  public static ArtifactoryChecksumSearchErrors create(int status, String message) {
    ArtifactoryChecksumSearchErrors errors = new ArtifactoryChecksumSearchErrors();
    ArtifactoryChecksumSearchError error = new ArtifactoryChecksumSearchError();
    error.status = status;
    error.message = message;
    errors.errors.add(error);
    return errors;
  }
}
