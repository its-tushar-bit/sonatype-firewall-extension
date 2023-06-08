/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantUtil.IS_MTIQ_BATCH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantUtilTest
    extends MultiTenantTestSupport
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Mock
  OperationalDataStore operationalDataStore;

  @Mock
  DataSource dataSource;

  @Before
  @Override
  public void setup() {
    super.setup();

    when(operationalDataStore.getDataSource()).thenReturn(dataSource);
    OperationalDataStoreProvider.setInstance(operationalDataStore);
  }

  @Test
  public void shouldReturnMultiTenantTrue() {
    assertThat(new TenantUtil().isMultiTenant()).isTrue();
  }

  @Test
  public void shouldReturnSingleTenantFalse() {
    assertThat(new TenantUtil().isSingleTenant()).isFalse();
  }

  @Test
  public void shouldReturnSingleTenantTrue_whenMultiTenantFalse() {
    TenantThreadLocal.setTenant(SINGLE_TENANT);

    try {
      assertThat(new TenantUtil().isMultiTenant()).isFalse();
      assertThat(new TenantUtil().isSingleTenant()).isTrue();
    }
    finally {
      TenantThreadLocal.setGlobalTenant();
    }
  }

  @Test
  public void shouldReturnGlobalTrue() {
    assertThat(new TenantUtil().isGlobalTenant()).isTrue();
  }

  @Test
  public void shouldReturnGlobalTrue_whenGlobalSlug() {
    assertThat(new TenantUtil().isGlobalTenant("global")).isTrue();
    assertThat(new TenantUtil().isGlobalTenant("notglobal")).isFalse();
  }

  @Test
  public void shouldExtractTenantNameFromUrl() {
    assertThat(new TenantUtil().getTenantName("tenant1.mtiq.cloudy.sonatype.dev")).isEqualTo("tenant1");
    assertThat(new TenantUtil().getTenantName("tenant2.staging.mtiq.cloudy.sonatype.dev")).isEqualTo("tenant2");
    assertThat(new TenantUtil().getTenantName("tenant3.cloud-dev.sonatype.com")).isEqualTo("tenant3");
    assertThat(new TenantUtil().getTenantName("tenant4.nexus.local")).isEqualTo("tenant4");
  }

  @Test
  public void shouldThrowAnExceptionOnLocalhost() {
    assertThatThrownBy(() -> new TenantUtil().getTenantName("localhost")).hasMessage(
        "You should not be accessing multi-tenant IQ via localhost. Use a fake vanity URL");
  }

  @Test
  public void shouldThrowRuntimeExceptionForInvalidUrl() {
    assertThatThrownBy(() -> new TenantUtil().getTenantName("invalid-url")).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void shouldUseInternedTenantString_forSynchronization() {
    char[] tenantNameCharArray = {'t', '-', '1'};

    // Strings initialized from char arrays do not get interned
    TenantThreadLocal.setTenant(new Tenant(new String(tenantNameCharArray)));

    assertNotSame(new String(tenantNameCharArray), new TenantUtil().getTenantSlugForSynchronization());
    assertSame(new String(tenantNameCharArray).intern(), new TenantUtil().getTenantSlugForSynchronization());
  }

  @Test
  public void shouldReturnTrueWhenAllTenantsJob() {
    assertThat(new TenantUtil().isAllTenantsJob(AllTenantsJob.class)).isTrue();
    assertThat(new TenantUtil().isAllTenantsJob(String.class)).isFalse();
  }

  @Test
  public void shouldReturnTrueWhenMtiqBatchJob() {
    assertThat(new TenantUtil().isMtiqBatchJob(MtiqBatchJob.class)).isTrue();
    assertThat(new TenantUtil().isMtiqBatchJob(String.class)).isFalse();
  }

  @Test
  public void shouldReturnBackgroundModeWhenSet() {
    environmentVariables.set(IS_MTIQ_BATCH, "true");

    assertThat(new TenantUtil().isMtiqBatchMode()).isTrue();

    environmentVariables.set(IS_MTIQ_BATCH, "false");

    assertThat(new TenantUtil().isMtiqBatchMode()).isFalse();
  }

  @Test
  public void shouldGetAllTenantsFromDb() {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      String tenant1 = "tenant1";
      String tenant2 = "tenant2";

      dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(dataSource))
          .thenReturn(asList("t_" + tenant1, "t_" + tenant2));

      List<String> allTenants = new TenantUtil().getAllTenants();

      assertThat(allTenants).containsExactly(tenant1, tenant2);
    }
  }

  @Test
  public void shouldGetAllTenantsNamesFromDb() {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      String tenant1 = "tenant1";
      String tenant2 = "tenant2";

      dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(dataSource))
          .thenReturn(asList("t_" + tenant1, "t_" + tenant2));

      List<String> allTenants = new TenantUtil().getAllTenantsNames();

      assertThat(allTenants).containsExactly(tenant1, tenant2);
    }
  }

  @Test
  public void testValidateThrowsException_whenAllTenantJob_andNotGlobal() {
    Tenant tenant = new Tenant("not-global");

    assertThatThrownBy(() -> new TenantUtil().validateTenantForType(TestAllTenantsJob.class, tenant))
        .isInstanceOf(TenantUtil.InvalidTenantForJobTypeException.class)
        .hasMessage("AllTenantJob(s) cannot be created against a non-global tenant. " +
            "Type=" + TestAllTenantsJob.class.getSimpleName() + ", Tenant=" + tenant);
    ;
  }

  private interface TestAllTenantsJob extends AllTenantsJob
  {
  }
}
