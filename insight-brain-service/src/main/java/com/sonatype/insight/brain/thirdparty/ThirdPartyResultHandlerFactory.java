/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.sonatype.insight.scan.model.ItemContentType;

public class ThirdPartyResultHandlerFactory
{
  public static ThirdPartyScanResultHandler newHandler(ItemContentType itemContentType) {
    if (ItemContentType.CLAIR_SCANNER.equals(itemContentType)) {
      return new ClairScannerResultHandler();
    }
    else if (ItemContentType.SBOM.equals(itemContentType)) {
      return new SbomResultHandler();
    }
    else if (ItemContentType.CONTAINER_URI.equals(itemContentType)) {
      return new ContainerResultHandler();
    }
    throw new IllegalArgumentException("unsupported third party content type " + itemContentType);
  }
}
