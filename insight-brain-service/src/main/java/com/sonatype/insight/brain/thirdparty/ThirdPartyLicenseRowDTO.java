/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.SortedSet;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.scan.RowWithComponentIdentifierDTO;

/**
 * @since 1.81
 */
public class ThirdPartyLicenseRowDTO
    extends RowWithComponentIdentifierDTO
{
  public String hash;

  public SortedSet<ThirdPartyLicenseDTO> declaredLicenses = new TreeSet<>();

  public ThirdPartyLicenseRowDTO(final ComponentIdentifier componentIdentifier, final String hash) {
    super(componentIdentifier);
    this.hash = hash;
  }

  // for jackson
  ThirdPartyLicenseRowDTO() {
    this(null, null);
  }
}
