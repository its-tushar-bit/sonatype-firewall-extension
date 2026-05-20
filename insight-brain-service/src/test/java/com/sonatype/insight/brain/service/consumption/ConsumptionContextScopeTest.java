/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicensingModel;
import com.sonatype.insight.brain.service.consumption.ConsumptionContext.Scope;
import com.sonatype.insight.brain.service.consumption.ConsumptionContext.Snapshot;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsumptionContextScopeTest
{
  private ProductLicense productLicense;

  private SystemConfigurationPropertyDAO mockSystemConfigDao;

  private Object originalSystemConfigDao;

  private final AtomicReference<SystemConfigurationProperty> featureFlagState = new AtomicReference<>(null);

  @Before
  public void setUp() throws Exception {
    productLicense = mock(ProductLicense.class);
    when(productLicense.getLicensingModels()).thenReturn(Set.of(ProductLicensingModel.APP_BASED));
    injectMockSystemConfigurationPropertyDAO();
  }

  @After
  public void tearDown() throws Exception {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
    ConsumptionContext.clear();
    restoreSystemConfigurationPropertyDAO();
  }

  private void injectMockSystemConfigurationPropertyDAO() throws Exception {
    mockSystemConfigDao = mock(SystemConfigurationPropertyDAO.class);
    Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
    field.setAccessible(true);
    originalSystemConfigDao = field.get(null);
    field.set(null, mockSystemConfigDao);

    TransactionContext mockTx = mock(TransactionContext.class);
    lenient().when(mockSystemConfigDao.createTransactionContext()).thenReturn(mockTx);
    lenient().when(mockTx.dsl()).thenReturn(DSL.using(SQLDialect.POSTGRES));
    lenient().when(mockSystemConfigDao.getByName(any(), any())).thenAnswer(inv -> featureFlagState.get());
    lenient().doAnswer(inv -> {
      String value = inv.getArgument(2);
      featureFlagState.set(value == null ? null : new SystemConfigurationProperty(inv.getArgument(1), value));
      return null;
    }).when(mockSystemConfigDao).set(any(), any(), any());
  }

  private void restoreSystemConfigurationPropertyDAO() throws Exception {
    Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
    field.setAccessible(true);
    field.set(null, originalSystemConfigDao);
  }

  @Test
  public void scopeBackgroundJob_isNoOpWhenFeatureFlagIsOff() {
    try (Scope consumptionCtx = ConsumptionContext.scopeBackgroundJob(productLicense, "app-1")) {
      assertThat(ConsumptionContext.get()).isNull();
    }
    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void scopeBackgroundJob_closeRestoresPreviouslySetContext() {
    ConsumptionContext.set("org-1", "APP_BASED", "UI");
    assertThat(ConsumptionContext.get()).isNotNull();

    try (Scope consumptionCtx = ConsumptionContext.scopeBackgroundJob(productLicense, "app-1")) {
      // body runs
    }

    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("org-1");
    assertThat(ctx.getTier()).isEqualTo("APP_BASED");
    assertThat(ctx.getSource()).isEqualTo("UI");
  }

  @Test
  public void scopeBackgroundJob_noArgOverload_closeRestoresContext() {
    ConsumptionContext.set("org-1", "APP_BASED", "UI");

    try (Scope consumptionCtx = ConsumptionContext.scopeBackgroundJob(productLicense)) {
      // body runs
    }

    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("org-1");
  }

  @Test
  public void scopeBackgroundJob_closeRemovesContext_whenNoPrevious() {
    assertThat(ConsumptionContext.get()).isNull();

    try (Scope consumptionCtx = ConsumptionContext.scopeBackgroundJob(productLicense, "app-1")) {
      // body runs
    }

    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void scopeRestored_installsSnapshotAndAppliesOverrides() {
    Snapshot snapshot = new Snapshot("org-1", ProductLicensingModel.APP_BASED.name(), "UI", false);

    try (Scope consumptionCtx = ConsumptionContext.scopeRestored(snapshot, "app-42", "scan-99")) {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo("org-1");
      assertThat(ctx.getTier()).isEqualTo("APP_BASED");
      assertThat(ctx.getSource()).isEqualTo("UI");
      assertThat(ctx.getAppId()).isEqualTo("app-42");
      assertThat(ctx.getScanId()).isEqualTo("scan-99");
    }

    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void scopeRestored_nullSnapshotIsNoOpOnInstall_restoresOnClose() {
    ConsumptionContext.set("org-1", "APP_BASED", "UI");

    try (Scope consumptionCtx = ConsumptionContext.scopeRestored(null, "app-42", "scan-99")) {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getOrgId()).isEqualTo("org-1");
    }

    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("org-1");
  }

  @Test
  public void scope_closeIsIdempotent() {
    Scope scope = ConsumptionContext.scopeBackgroundJob(productLicense, "app-1");
    scope.close();
    scope.close();
    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void scope_nested_innerCloseRestoresOuterContext() {
    ConsumptionContext.set("outer-org", "APP_BASED", "UI");

    Snapshot innerSnap = new Snapshot("inner-org", "APP_BASED", "BACKGROUND_JOB", false);
    try (Scope inner = ConsumptionContext.scopeRestored(innerSnap, "app-42", "scan-99")) {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx.getOrgId()).isEqualTo("inner-org");
      assertThat(ctx.getSource()).isEqualTo("BACKGROUND_JOB");
    }

    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("outer-org");
    assertThat(ctx.getSource()).isEqualTo("UI");
  }

  @Test
  public void scopeBackgroundJob_resolvesTierFromProductLicense() {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);
    when(productLicense.getLicensingModels()).thenReturn(Set.of(ProductLicensingModel.LEGACY));

    try (Scope consumptionCtx = ConsumptionContext.scopeBackgroundJob(productLicense, "app-1")) {
      ConsumptionContext ctx = ConsumptionContext.get();
      assertThat(ctx).isNotNull();
      assertThat(ctx.getTier()).isEqualTo("LEGACY");
      assertThat(ctx.getAppId()).isEqualTo("app-1");
      assertThat(ctx.getSource()).isEqualTo("CONTINUOUS_MONITOR");
    }
  }

  @Test
  public void scopeBackgroundJob_failsOpenWhenProductLicenseThrows() {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);
    when(productLicense.getLicensingModels()).thenThrow(new RuntimeException("license unavailable"));

    try (Scope consumptionCtx = ConsumptionContext.scopeBackgroundJob(productLicense, "app-1")) {
      assertThat(ConsumptionContext.get()).isNull();
    }
    assertThat(ConsumptionContext.get()).isNull();
  }

  @Test
  public void snapshot_capturesCurrentContext() {
    ConsumptionContext.set("org-1", "APP_BASED", "API");
    Snapshot snap = ConsumptionContext.snapshot();
    assertThat(snap).isNotNull();
    assertThat(snap.orgId()).isEqualTo("org-1");
    assertThat(snap.tier()).isEqualTo("APP_BASED");
    assertThat(snap.source()).isEqualTo("API");
    assertThat(snap.directApiRequest()).isFalse();
  }

  @Test
  public void snapshot_capturesDirectApiRequestFlag_whenSet() {
    ConsumptionContext.set("org-1", "APP_BASED", "API", true);
    Snapshot snap = ConsumptionContext.snapshot();
    assertThat(snap).isNotNull();
    assertThat(snap.directApiRequest()).isTrue();
  }

  @Test
  public void snapshot_returnsNull_whenNoContext() {
    ConsumptionContext.clear();
    assertThat(ConsumptionContext.snapshot()).isNull();
  }

  @Test
  public void restore_installsSnapshot() {
    Snapshot snap = new Snapshot("org-2", "SBOM_BASED", "CLI", false);
    ConsumptionContext.restore(snap);
    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("org-2");
    assertThat(ctx.getTier()).isEqualTo("SBOM_BASED");
    assertThat(ctx.getSource()).isEqualTo("CLI");
    assertThat(ctx.isDirectApiRequest()).isFalse();
  }

  @Test
  public void restore_nullSnapshot_isNoOp() {
    ConsumptionContext.set("org-1", "APP_BASED", "API");
    ConsumptionContext.restore(null);
    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("org-1");
    assertThat(ctx.getTier()).isEqualTo("APP_BASED");
    assertThat(ctx.getSource()).isEqualTo("API");
  }

  @Test
  public void restoreAndScope_appliesAppIdAndScanId() {
    Snapshot snap = new Snapshot("org-1", "APP_BASED", "API", false);
    ConsumptionContext.restoreAndScope(snap, "app-42", "scan-99");
    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("org-1");
    assertThat(ctx.getAppId()).isEqualTo("app-42");
    assertThat(ctx.getScanId()).isEqualTo("scan-99");
  }

  @Test
  public void restoreAndScope_nullSnapshot_isNoOp() {
    ConsumptionContext.set("org-1", "APP_BASED", "API");
    ConsumptionContext.restoreAndScope(null, "app-42", "scan-99");
    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getAppId()).isNull();
    assertThat(ctx.getScanId()).isNull();
  }

  @Test
  public void restoreAndScope_nullOverrides_keepsSnapshotState() {
    Snapshot snap = new Snapshot("org-1", "APP_BASED", "API", false);
    ConsumptionContext.restoreAndScope(snap, null, null);
    ConsumptionContext ctx = ConsumptionContext.get();
    assertThat(ctx).isNotNull();
    assertThat(ctx.getOrgId()).isEqualTo("org-1");
    assertThat(ctx.getAppId()).isNull();
    assertThat(ctx.getScanId()).isNull();
  }
}
