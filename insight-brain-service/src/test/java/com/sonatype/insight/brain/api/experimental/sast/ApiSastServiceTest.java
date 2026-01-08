/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import javax.inject.Inject;

import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Test;
import com.google.common.collect.ImmutableSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiSastServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSastService apiSastService;

  private final ProductLicense productLicense = mock(ProductLicense.class);

  private final FeaturesService featuresService = mock(FeaturesService.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicense);
    binder.bind(FeaturesService.class).toInstance(featuresService);
    super.configure(binder);
  }

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
