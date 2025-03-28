/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils;
import com.sonatype.insight.cpe.CPEIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Component;
import us.springett.parsers.cpe.util.Validate;

public class SbomCommonUtils
{
  public static String newFilteredScanFileName(String scanId) {
    if (scanId == null) {
      scanId = UUID.randomUUID().toString().replace("-", "");
    }
    return "scan-" + scanId + "-filtered.xml.gz";
  }

  public static PackageUrlIdentifier getPackageUrlIdentifierFromCpe(String cpe) {
    if (cpe == null || !Validate.cpe(cpe).isValid()) {
      return null;
    }
    return CPEIdentifier.fromCpeString(cpe).toPackageUrlIdentifier();
  }

  public static ComponentIdentifier getComponentIdentifier(
      final PackageUrlIdentifier packageUrlIdentifier,
      final Component component)
  {
    ComponentIdentifier componentIdentifier;
    component.setPurl(ThirdPartyScanResultUtils.getTruncatedPurl(packageUrlIdentifier.getPackageUrl()));
    componentIdentifier = packageUrlIdentifier.toComponentIdentifier();
    componentIdentifier.ensureComplete();
    component.setName(packageUrlIdentifier.getName());
    component.setVersion(packageUrlIdentifier.getVersion());
    String namespace = packageUrlIdentifier.getNamespace();
    if (StringUtils.isNotBlank(namespace)) {
      component.setGroup(namespace);
    }
    return componentIdentifier;
  }
}
