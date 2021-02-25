/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.scan.RowWithComponentIdentifierDTO;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;

/**
 * @since 1.75
 */
public class ThirdPartyReportComponentDTO
    extends RowWithComponentIdentifierDTO
{
  public ThirdPartyBillOfMaterialsRowDTO bomRow;

  public List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows = new ArrayList<>();

  public ThirdPartyLicenseRowDTO licensesRow = new ThirdPartyLicenseRowDTO();

  public ThirdPartyReportComponentDTO(ThirdPartyBillOfMaterialsRowDTO bomRow) {
    super(bomRow.componentIdentifier);
    this.bomRow = bomRow;
  }
}
