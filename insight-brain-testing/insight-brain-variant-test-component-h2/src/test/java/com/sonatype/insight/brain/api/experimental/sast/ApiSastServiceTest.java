/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ApiSastServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiSastService apiSastService;

  private final ProductLicense productLicense = mock(ProductLicense.class);

  private final FeaturesService featuresService = mock(FeaturesService.class);

  @Test
  public void testValidate_withValidLicense_butNotDeveloperLicense() {
    when(productLicense.isValid()).thenReturn(true);
    when(featuresService.getFeatures()).thenReturn(ImmutableSet.of(LicensedFeature.DASHBOARD));
    assertThat(apiSastService.validate().isValid()).isFalse();
  }

  @Test
  public void testValidate_withValidDeveloperLicense() {
    when(productLicense.isValid()).thenReturn(true);
    when(featuresService.getFeatures()).thenReturn(ImmutableSet.of(LicensedFeature.DEVELOPER_DASHBOARD));
    assertThat(apiSastService.validate().isValid()).isTrue();
  }

  @Test
  public void testValidate_withInValidLicense() {
    when(productLicense.isValid()).thenReturn(false);
    assertThat(apiSastService.validate().isValid()).isFalse();
  }
}
