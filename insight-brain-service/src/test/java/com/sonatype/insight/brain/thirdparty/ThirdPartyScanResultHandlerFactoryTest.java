/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.scan.model.ItemContentType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ThirdPartyScanResultHandlerFactoryTest
    extends AbstractComponentTest
{
  @Inject
  private ThirdPartyResultHandlerFactory thirdPartyResultHandlerFactory;

  @Test
  public void testGetHandler_ClairScanner() {
    ThirdPartyScanResultHandler handler =
        thirdPartyResultHandlerFactory.newHandler(ItemContentType.CLAIR_SCANNER, null);
    assertThat(handler).isInstanceOf(ClairScannerResultHandler.class);
  }

  @Test
  public void testGetHandler_Sbom() {
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    ThirdPartyScanResultHandler handler =
        thirdPartyResultHandlerFactory.newHandler(ItemContentType.SBOM, thirdPartyScanContext);
    assertThat(handler).isInstanceOf(SbomResultHandler.class);
    SbomResultHandler sbomResultHandler = (SbomResultHandler) handler;
    assertThat(sbomResultHandler.thirdPartyScanContext).isEqualTo(thirdPartyScanContext);
  }

  @Test
  public void testGetHandler_Spdx() {
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    ThirdPartyScanResultHandler handler =
        thirdPartyResultHandlerFactory.newHandler(ItemContentType.SPDX, thirdPartyScanContext);
    assertThat(handler).isInstanceOf(SpdxResultHandler.class);
    SpdxResultHandler spdxResultHandler = (SpdxResultHandler) handler;
    assertThat(spdxResultHandler.thirdPartyScanContext).isEqualTo(thirdPartyScanContext);
  }

  @Test
  public void testGetHandler_Container() {
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    ThirdPartyScanResultHandler handler =
        thirdPartyResultHandlerFactory.newHandler(ItemContentType.CONTAINER_URI, thirdPartyScanContext);
    assertThat(handler).isInstanceOf(ContainerResultHandler.class);
    ContainerResultHandler containerResultHandler = (ContainerResultHandler) handler;
    assertThat(containerResultHandler.thirdPartyScanContext).isEqualTo(thirdPartyScanContext);
  }

  @Test
  public void testGetHandler_UnsupportedType() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> thirdPartyResultHandlerFactory.newHandler(ItemContentType.GO_MODULE, null))
        .withMessage("unsupported third party content type GO_MODULE");
  }
}
