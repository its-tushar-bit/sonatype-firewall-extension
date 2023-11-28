/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;

import org.cyclonedx.model.Swid;

public class ThirdPartyBillOfMaterialsRowDTO
    extends BillOfMaterialsRowDTO
{
  public ThirdPartyBillOfMaterialsRowDTO() {
    super(null, null);
  }

  public ThirdPartyBillOfMaterialsRowDTO(
      final ComponentIdentifier componentIdentifier,
      final String hash)
  {
    super(componentIdentifier, hash);
  }

  public String cpe;

  public Swid swid;
}
