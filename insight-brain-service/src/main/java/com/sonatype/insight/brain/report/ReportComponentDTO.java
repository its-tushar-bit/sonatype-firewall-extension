/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.scan.HealthCheckReportRowDTO;
import com.sonatype.insight.scan.HealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.RowWithComponentIdentifierDTO;
import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;

public class ReportComponentDTO
    extends RowWithComponentIdentifierDTO
{
  public BillOfMaterialsRowDTO bomRow;

  public List<HealthCheckReportSecurityRowDTO> securityRows = new ArrayList<>();

  public HealthCheckReportRowDTO licenseRow;

  public ReportComponentDTO(final BillOfMaterialsRowDTO bomRow) {
    this.componentIdentifier = bomRow.componentIdentifier;
    this.bomRow = bomRow;
    licenseRow = new HealthCheckReportRowDTO(bomRow.componentIdentifier, bomRow.hash);
  }
}
