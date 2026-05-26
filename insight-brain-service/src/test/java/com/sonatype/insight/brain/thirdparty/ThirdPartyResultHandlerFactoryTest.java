/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ItemContentType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThirdPartyResultHandlerFactoryTest
{
  @Mock
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Mock
  private DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  @Mock
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Mock
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Mock
  private MultiLicenseDAO multiLicenseDAO;

  @Mock
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Mock
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO;

  @Mock
  private TelemetryUtils telemetryUtils;

  @Mock
  private TelemetrySender telemetrySender;

  @Mock
  private ProductLicense productLicense;

  private SystemConfigurationPropertyDAO mockConfigDao;

  private ThirdPartyResultHandlerFactory factory;

  @Before
  public void setUp() {
    mockConfigDao = mock(SystemConfigurationPropertyDAO.class);
    when(mockConfigDao.createTransactionContext()).thenReturn(mock(TransactionContext.class));
    // Default: no rows → CONTAINER_IMAGES_EVAL_ENABLED is enabled (enabledWhenAbsent=true)
    when(mockConfigDao.getByName(any(), any(String.class))).thenReturn(null);
    SystemConfigurationPropertyFeature.injectDependencies(mockConfigDao);

    com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler spdx3VersionHandler =
        mock(com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler.class);
    factory = new ThirdPartyResultHandlerFactory(
        thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO,
        thirdPartyVexDAO, telemetryUtils, telemetrySender, productLicense, spdx3VersionHandler);
  }

  @Test
  public void testContainerUriSonatype_withoutLicense_throwsInvalidLicenseException() {
    when(productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION)).thenReturn(false);

    ThirdPartyScanContext context = new ThirdPartyScanContext(null, null, null, null, null);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> factory.newHandler(ItemContentType.CONTAINER_URI_SONATYPE, context))
        .withMessageContaining("Container Images Evaluation");
  }

  @Test
  public void testContainerUriSonatype_featureDisabled_withoutLicense_returnsHandler() {
    // Row present → CONTAINER_IMAGES_EVAL_ENABLED is disabled (enabledWhenAbsent=true)
    when(mockConfigDao.getByName(any(), eq(SystemConfigurationProperty.CONTAINER_IMAGES_EVAL_ENABLED)))
        .thenReturn(mock(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.class));

    ThirdPartyScanContext context = new ThirdPartyScanContext(null, null, null, null, null);

    ThirdPartyScanResultHandler handler =
        factory.newHandler(ItemContentType.CONTAINER_URI_SONATYPE, context);
    assertThat(handler).isInstanceOf(ContainerResultHandler.class);
  }

  @Test
  public void testContainerUriSonatype_withLicense_returnsContainerResultHandler() {
    when(productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION)).thenReturn(true);

    ThirdPartyScanContext context = new ThirdPartyScanContext(null, null, null, null, null);

    ThirdPartyScanResultHandler handler =
        factory.newHandler(ItemContentType.CONTAINER_URI_SONATYPE, context);
    assertThat(handler).isInstanceOf(ContainerResultHandler.class);
  }

  @Test
  public void testContainerUri_withoutLicense_returnsHandlerNormally() {
    ThirdPartyScanContext context = new ThirdPartyScanContext(null, null, null, null, null);

    ThirdPartyScanResultHandler handler = factory.newHandler(ItemContentType.CONTAINER_URI, context);
    assertThat(handler).isInstanceOf(ContainerResultHandler.class);
  }

  @Test
  public void testNonContainerType_unaffectedByLicenseCheck() {
    ThirdPartyScanResultHandler handler = factory.newHandler(ItemContentType.CLAIR_SCANNER, null);
    assertThat(handler).isInstanceOf(ClairScannerResultHandler.class);
  }
}
