/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsRequestListDto.ApiComponentNearestFixedVersionsRequestDto;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class ApiComponentNearestFixedVersionsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiComponentNearestFixedVersionsService service;

  @Mock
  private HdsClient hdsClientMock;

  @Captor
  private ArgumentCaptor<Set<ComponentIdentifier>> compomentIdentifierSetCaptor;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  @Test
  public void testGetNearestFixedVersions_nullComponentsList() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getNearestFixedVersions(null))
        .withMessage("No components provided in the request");
  }

  @Test
  public void testGetNearestFixedVersions_emptyComponentsList() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getNearestFixedVersions(new ApiComponentNearestFixedVersionsRequestListDto()))
        .withMessage("No components provided in the request");
  }

  @Test
  public void testGetNearestFixedVersions_invalidPackageUrl() {
    ApiComponentNearestFixedVersionsRequestListDto listDto = new ApiComponentNearestFixedVersionsRequestListDto();
    ApiComponentNearestFixedVersionsRequestDto dto = new ApiComponentNearestFixedVersionsRequestDto();
    dto.setPackageUrl("pkg:maven/g/a@v");
    listDto.getComponents().add(dto);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getNearestFixedVersions(listDto))
        .withMessage(
            "Invalid packageUrl: pkg:maven/g/a@v. The following coordinates are missing for given format: [type]");
  }

  @Test
  public void testGetNearestFixedVersions() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");

    ApiComponentNearestFixedVersionsRequestListDto listDto = new ApiComponentNearestFixedVersionsRequestListDto();

    ApiComponentNearestFixedVersionsRequestDto dto1 = new ApiComponentNearestFixedVersionsRequestDto();
    dto1.setPackageUrl(PackageUrlIdentifier.toPackageUrl(componentIdentifier1));

    ApiComponentNearestFixedVersionsRequestDto dto2 = new ApiComponentNearestFixedVersionsRequestDto();
    dto2.setPackageUrl(PackageUrlIdentifier.toPackageUrl(componentIdentifier2));

    listDto.getComponents().add(dto1);
    listDto.getComponents().add(dto2);

    ComponentNearestFixedVersions fixedVersion1 = new ComponentNearestFixedVersions();
    fixedVersion1.setComponentIdentifier(componentIdentifier1);

    ComponentNearestFixedVersions fixedVersion2 = new ComponentNearestFixedVersions();
    fixedVersion2.setComponentIdentifier(componentIdentifier2);

    when(hdsClientMock.post(eq(ComponentNearestFixedVersions[].class),
        eq(ApiComponentNearestFixedVersionsService.HDS_COMPONENT_NEAREST_FIXED_VERSION_PATH),
        compomentIdentifierSetCaptor.capture()))
            .thenReturn(new ComponentNearestFixedVersions[]{fixedVersion1, fixedVersion2});

    assertThat(service.getNearestFixedVersions(listDto))
        .extracting(ComponentNearestFixedVersions::getComponentIdentifier)
        .containsExactlyInAnyOrder(componentIdentifier1, componentIdentifier2);

    assertThat(compomentIdentifierSetCaptor.getValue())
        .containsExactlyInAnyOrder(componentIdentifier1, componentIdentifier2);
  }
}
