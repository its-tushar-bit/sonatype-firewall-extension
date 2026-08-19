/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * @since 66
 */
public class ApiComponentOrPurlIdentifierDTOV2
    extends ApiComponentIdentifierDTOV2
{
  private String packageUrl;

  public static ApiComponentOrPurlIdentifierDTOV2 fromComponentIdentifier(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }

    ApiComponentOrPurlIdentifierDTOV2 dto = new ApiComponentOrPurlIdentifierDTOV2();
    dto.setFormat(componentIdentifier.getFormat());
    dto.setCoordinates(componentIdentifier.getCoordinates());
    return dto;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(final String packageUrl) {
    this.packageUrl = packageUrl;
  }
}
