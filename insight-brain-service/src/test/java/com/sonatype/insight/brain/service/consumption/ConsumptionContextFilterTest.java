/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicensingModel;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConsumptionContextFilterTest
{
  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private FilterChain mockChain;

  @Mock
  private SystemConfigurationPropertyDAO mockSystemConfigDao;

  @Mock
  private ProductLicense mockProductLicense;

  private ConsumptionContextFilter filter;

  private Object originalSystemConfigDao;

  @Before
  public void setUp() throws Exception {
    injectMockSystemConfigurationPropertyDAO();
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);

    filter = new ConsumptionContextFilter(mockProductLicense);
  }

  @After
  public void tearDown() throws Exception {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
    ConsumptionContext.clear();
    restoreSystemConfigurationPropertyDAO();
  }

  @Test
  public void doFilter_apiPath_setsSourceToApi() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/api/v2/components");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getSource()).isEqualTo("API");
      assertThat(ctx.isDirectApiRequest()).isTrue();
    });
  }

  @Test
  public void resolveSource_cliUserAgent_returnsCli() throws Exception {
    lenient().when(mockRequest.getRequestURI()).thenReturn("/rest/scan");
    when(mockRequest.getHeader("User-Agent")).thenReturn("Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getSource()).isEqualTo("CLI");
      assertThat(ctx.isDirectApiRequest()).isFalse();
    });
  }

  @Test
  public void resolveSource_jenkinsUserAgent_returnsCiCd() throws Exception {
    lenient().when(mockRequest.getRequestURI()).thenReturn("/rest/scan");
    when(mockRequest.getHeader("User-Agent")).thenReturn("Jenkins/2.400");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getSource()).isEqualTo("CI_CD");
      assertThat(ctx.isDirectApiRequest()).isFalse();
    });
  }

  @Test
  public void resolveSource_browserUserAgent_returnsUi() throws Exception {
    lenient().when(mockRequest.getRequestURI()).thenReturn("/rest/component/details");
    when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (X11; Linux) AppleWebKit/537.36");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getSource()).isEqualTo("UI");
      assertThat(ctx.isDirectApiRequest()).isFalse();
    });
  }

  @Test
  public void resolveSource_apiPathWithCliAgent_returnsCli() throws Exception {
    lenient().when(mockRequest.getRequestURI()).thenReturn("/api/v2/scan");
    when(mockRequest.getHeader("User-Agent")).thenReturn("Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getSource()).isEqualTo("CLI");
      assertThat(ctx.isDirectApiRequest()).isFalse();
    });
  }

  @Test
  public void doFilter_clearsContextInFinallyBlock() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/rest/component/details");

    filter.doFilter(mockRequest, mockResponse, mockChain);

    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void doFilter_clearsContextWhenChainThrows() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/rest/component/details");
    ServletException expected = new ServletException("test error");
    doThrow(expected).when(mockChain).doFilter(any(), any());

    try {
      filter.doFilter(mockRequest, mockResponse, mockChain);
    }
    catch (ServletException e) {
      // expected
    }

    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void doFilter_clearsStaleThreadLocalState() throws Exception {
    ConsumptionContext.set("stale-tenant", "APP_BASED", "BACKGROUND_JOB");
    assertThat(ConsumptionContext.get()).isNotNull();
    lenient().when(mockRequest.getRequestURI()).thenReturn("/api/v2/components");

    filter.doFilter(mockRequest, mockResponse, mockChain);

    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void doFilter_globalTenant_skipsContextPopulation() throws Exception {
    when(mockRequest.getRequestURI()).thenReturn("/api/v2/components");

    try (MockedStatic<ConsumptionOrgIdResolver> resolver = mockStatic(ConsumptionOrgIdResolver.class)) {
      resolver.when(ConsumptionOrgIdResolver::resolveForRequest).thenReturn(null);

      filter.doFilter(mockRequest, mockResponse,
          (req, res) -> assertThat(ConsumptionContext.get()).isNull());
    }
  }

  @Test
  public void resolveTier_usesProductLicenseModel() throws Exception {
    when(mockProductLicense.getLicensingModels()).thenReturn(Set.of(ProductLicensingModel.SBOM_BASED));
    when(mockRequest.getRequestURI()).thenReturn("/rest/component/details");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getTier()).isEqualTo("SBOM_BASED");
    });
  }

  @Test
  public void resolveTier_fallsBackToAppBased_whenLicenseModelsEmpty() throws Exception {
    when(mockProductLicense.getLicensingModels()).thenReturn(Set.of());
    when(mockRequest.getRequestURI()).thenReturn("/rest/component/details");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getTier()).isEqualTo("APP_BASED");
    });
  }

  @Test
  public void resolveTier_fallsBackToAppBased_whenLicenseModelsNull() throws Exception {
    when(mockProductLicense.getLicensingModels()).thenReturn(null);
    when(mockRequest.getRequestURI()).thenReturn("/rest/component/details");

    filter.doFilter(mockRequest, mockResponse, (req, res) -> {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      assertThat(ctx.getTier()).isEqualTo("APP_BASED");
    });
  }

  @Test
  public void doFilter_populateContextThrows_proceedsWithFilterChain() throws Exception {
    when(mockProductLicense.getLicensingModels())
        .thenThrow(new RuntimeException("simulated license fetch failure"));
    lenient().when(mockRequest.getRequestURI()).thenReturn("/rest/component/details");

    filter.doFilter(mockRequest, mockResponse, mockChain);

    verify(mockChain).doFilter(mockRequest, mockResponse);
    assertThat(ConsumptionContext.get()).isNull();
  }

  // sessionId is no longer populated in the filter — Shiro auth runs after the filter
  // (FilterOrder.CONSUMPTION_CONTEXT = 12). Lazy population happens in HdsClient.emitEvent
  // and is verified live via the manual-testing TC4/TC8 evidence under
  // docs/superpowers/manual-testing/runs/2026-06-11-runtime/.

  private final AtomicReference<SystemConfigurationProperty> featureFlagState = new AtomicReference<>(null);

  private void injectMockSystemConfigurationPropertyDAO() throws Exception {
    Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
    field.setAccessible(true);
    originalSystemConfigDao = field.get(null);
    field.set(null, mockSystemConfigDao);

    TransactionContext mockTx = mock(TransactionContext.class);
    lenient().when(mockSystemConfigDao.createTransactionContext()).thenReturn(mockTx);
    lenient().when(mockTx.dsl()).thenReturn(DSL.using(SQLDialect.POSTGRES));

    lenient().when(mockSystemConfigDao.getByName(any(), any()))
        .thenAnswer(invocation -> featureFlagState.get());

    lenient().doAnswer(invocation -> {
      String value = invocation.getArgument(2);
      if (value == null) {
        featureFlagState.set(null);
      }
      else {
        featureFlagState.set(new SystemConfigurationProperty(invocation.getArgument(1), value));
      }
      return null;
    }).when(mockSystemConfigDao).set(any(), any(), any());
  }

  private void restoreSystemConfigurationPropertyDAO() throws Exception {
    Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
    field.setAccessible(true);
    field.set(null, originalSystemConfigDao);
  }

}
