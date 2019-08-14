/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.sonatype.insight.scan.model.ItemContentType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ThirdPartyScanResultHandlerFactoryTest
{
  @Test
  public void testGetHandler_ClairScanner() {
    ThirdPartyScanResultHandler handler = ThirdPartyResultHandlerFactory.newHandler(ItemContentType.CLAIR_SCANNER);
    assertThat(handler).isInstanceOf(ClairScannerResultHandler.class);
  }

  @Test
  public void testGetHandler_UnsupportedType() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ThirdPartyResultHandlerFactory.newHandler(ItemContentType.GO_MODULE))
        .withMessage("unsupported third party content type GO_MODULE");
  }
}
