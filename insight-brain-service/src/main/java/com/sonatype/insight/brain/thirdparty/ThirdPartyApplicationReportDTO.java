/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;

public class ThirdPartyApplicationReportDTO
{
  public List<BillOfMaterialsRowDTO> billOfMaterials = new ArrayList<>();

  public List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows = new ArrayList<>();
}
