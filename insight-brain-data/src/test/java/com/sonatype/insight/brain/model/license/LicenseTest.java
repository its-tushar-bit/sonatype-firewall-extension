/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseTest
{
  @Test
  public void testIsAlpObservedLicenseFormatHidden() {
    for (String format : ComponentIdentifier.getFormatsSupportedByHds()) {
      if (ComponentIdentifier.FORMAT_MAVEN.equals(format)) {
        assertThat(License.isAlpObservedLicenseFormatHidden(format))
            .withFailMessage("Expected isAlpObservedLicenseFormatHidden to be false for format 'maven':")
            .isFalse();
      }
      else {
        assertThat(License.isAlpObservedLicenseFormatHidden(format))
            .withFailMessage("Expected isAlpObservedLicenseFormatHidden to be true for format '%s':", format)
            .isTrue();
      }
    }
  }
}
