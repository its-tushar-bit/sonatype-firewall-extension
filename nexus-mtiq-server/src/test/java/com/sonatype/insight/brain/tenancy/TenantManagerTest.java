/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantManager.TENANT_PARAMETER_CANNOT_BE_NULL;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantUtil.TENANT_DOES_NOT_EXIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.service.TenantLifecycle;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class TenantManagerTest
    extends AbstractMultiTenantDatabaseTest
{
  static final String TENANT_NAME = "tenant";

  @Mock
  TenantManaged job;

  @Mock
  AllTenantsJob allTenantsJob;

  @Mock
  TenantLifecycle lifecycle;

  @Mock
  DatabaseProvisioner databaseProvisioner;

  @Mock
  TenantValidator tenantValidator;

  @Mock
  TenantService tenantService;

  @Mock
  DeletedTenantDAO deletedTenantDAO;

  Set<TenantManaged> tenantManagedBeans;

  Tenant tenant = new Tenant(TENANT_NAME);

  TenantManager underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    tenantManagedBeans = new HashSet<>();
    tenantManagedBeans.add(job);

    underTest = new TenantManager(() -> tenantManagedBeans, () -> lifecycle, databaseProvisioner,
        tenantValidator, deletedTenantDAO, tenantService);

    when(tenantValidator.validateTenantExists(tenant)).thenReturn(true);

    when(deletedTenantDAO.isScheduledForDeletion(any())).thenReturn(false);
  }

  @Test
  public void shouldSetGlobalTenant_andMultiTenantMode() {
    testAsTenant(new Tenant(TENANT_NAME), tenant -> {
      TenantThreadLocal.setGlobalTenant();

      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
      assertThat(new TenantUtil().isMultiTenant()).isTrue();
    });
  }

  @Test
  public void shouldSetTenant_whenTenantNameProvided() {
    underTest.setTenant(TENANT_NAME);

    assertThat(underTest.getTenant()).isEqualTo(new Tenant(TENANT_NAME));
  }

  @Test
  public void shouldSetTenant_whenTenantProvided() {
    underTest.setTenant(tenant);

    assertThat(underTest.getTenant()).isEqualTo(tenant);
  }

  @Test
  public void shouldSetTenantForAdminRequest_whenNameProvided() {
    underTest.setTenantForAdminRequest(TENANT_NAME);

    assertThat(underTest.getTenant()).isEqualTo(tenant);
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNameForAdminRequestNull() {
    assertThatThrownBy(() -> underTest.setTenantForAdminRequest(null))
        .withFailMessage(TENANT_PARAMETER_CANNOT_BE_NULL)
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNameNull() {
    assertThatThrownBy(() -> underTest.setTenant((String) null))
        .withFailMessage(TENANT_PARAMETER_CANNOT_BE_NULL)
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNameEmpty() {
    assertThatThrownBy(() -> underTest.setTenant(""))
        .withFailMessage(TENANT_PARAMETER_CANNOT_BE_NULL)
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNull() {
    assertThatThrownBy(() -> underTest.setTenant((Tenant) null))
        .withFailMessage(TENANT_PARAMETER_CANNOT_BE_NULL)
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldNotRegister_whenGlobalTenant() throws Exception {
    underTest.setTenant(GLOBAL_TENANT);

    verify(job, never()).register();
    verify(lifecycle, never()).bootTenant();
  }

  @Test
  public void shouldNotRegister_allTenantsJobs() {
    tenantManagedBeans.add(allTenantsJob);

    underTest = new TenantManager(() -> tenantManagedBeans, () -> lifecycle, databaseProvisioner,
        tenantValidator, deletedTenantDAO, tenantService);

    testAsNewTenant(t -> {
      when(tenantValidator.validateTenantExists(t)).thenReturn(true);

      underTest.setTenant(t);

      verify(job).register();
      verify(allTenantsJob, never()).register();
    });
  }

  @Test
  public void shouldGetRegisteredTenants() {
    underTest.setTenant(tenant);

    testAsNewTenant(t -> {
      when(tenantValidator.validateTenantExists(t)).thenReturn(true);

      underTest.setTenant(t);

      assertThat(underTest.getRegisteredTenants()).containsExactlyInAnyOrder(tenant.tenantSlug, t.tenantSlug);
    });
  }

  @Test
  public void shouldGetRegisteredTenants_onlyIfTenantIsRegistered() {
    underTest.setTenant(tenant);

    testAsNewTenant(t -> {
      when(tenantValidator.validateTenantExists(t)).thenReturn(true);
      underTest.setTenant(t);

      // Verify the tenant was registered
      assertThat(underTest.getRegisteredTenants()).containsExactlyInAnyOrder(tenant.tenantSlug, t.tenantSlug);

      underTest.deregisterTenant(t.tenantSlug);

      assertThat(underTest.getRegisteredTenants()).containsExactlyInAnyOrder(tenant.tenantSlug);
    });
  }

  @Test
  public void shouldOnlyRegisterTenantOnce() throws Exception {
    setTenantAndAssertRegistration();

    // Call set tenant a second time
    underTest.setTenant(tenant);

    // Verify that the registration code wasn't called again
    verify(job, times(1)).register();
    verify(lifecycle, times(1)).bootTenant();
  }

  @Test
  public void shouldNotRegisterTenantIfScheduledForDeletion() {
    when(deletedTenantDAO.isScheduledForDeletion(any())).thenReturn(true);

    // Call set tenant a second time
    assertThatThrownBy(() -> underTest.setTenant(tenant)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(TENANT_DOES_NOT_EXIST);

    verify(job, never()).register();
    verify(lifecycle, never()).bootTenant();
  }

  @Test
  public void shouldNotRegister_globalTenantJobs() throws Exception {
    MockTenantManaged mockGlobalTenantJob = mock(MockTenantManaged.class);

    tenantManagedBeans.add(mockGlobalTenantJob);

    setTenantAndAssertRegistration();

    verify(mockGlobalTenantJob, never()).register();
  }

  @Test
  public void shouldCallRegisterInOrderOfPriority() throws Exception {
    int priority = 10;

    when(job.registrationPriority()).thenReturn(priority + 1);

    TenantManaged tenantManaged1 = mock(TenantManaged.class);
    when(tenantManaged1.registrationPriority()).thenReturn(priority);
    tenantManagedBeans.add(tenantManaged1);

    TenantManaged tenantManaged2 = mock(TenantManaged.class);
    when(tenantManaged1.registrationPriority()).thenReturn(priority - 1);
    tenantManagedBeans.add(tenantManaged2);

    setTenantAndAssertRegistration();

    InOrder order = inOrder(job, tenantManaged1, tenantManaged2);
    order.verify(tenantManaged2).register();
    order.verify(tenantManaged1).register();
    order.verify(job).register();
    order.verifyNoMoreInteractions();
  }

  @Test
  public void shouldDeregisterTenant() {
    underTest.setTenant(tenant);

    assertThat(underTest.isTenantRegistered(tenant)).isTrue();

    underTest.deregisterTenant(tenant.tenantSlug);

    assertThat(underTest.isTenantRegistered(tenant)).isFalse();
    verify(tenantManagedBeans.iterator().next(), atMostOnce()).deregister();
  }

  @Test
  public void shouldRegisterAllTenantsOnBootByDefault() {
    TenantManager spyUnderTest = spy(underTest);

    spyUnderTest.afterSingletonsInstantiated();

    verify(spyUnderTest, times(1)).preregisterAllTenants();
  }

  @Test
  public void shouldDeregisterTenantIfTenantSlugIsNotBlank() {
    underTest.deregisterTenant("tenant-slug");

    verify(tenantManagedBeans.iterator().next(), atMostOnce()).deregister();
  }

  @Test
  public void shouldNotDeregisterTenantIfTenantSlugIsBlank() {
    underTest.deregisterTenant(null);
    verify(tenantManagedBeans.iterator().next(), never()).deregister();

    underTest.deregisterTenant("");
    verify(tenantManagedBeans.iterator().next(), never()).deregister();
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantDoesntExist() {
    when(tenantValidator.validateTenantExists(tenant)).thenReturn(false);

    assertThatThrownBy(() -> underTest.setTenant(tenant))
        .withFailMessage(TENANT_DOES_NOT_EXIST)
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test // CLM-25317 // CLM-25499 reverted the ordering introduced with CLM-25317
  public void shouldBootTenantAfterInitializingTenantJobs() throws Exception {
    TenantManaged tenantBean = mock(TenantManaged.class);
    tenantManagedBeans.add(tenantBean);

    setTenantAndAssertRegistration();

    InOrder order = inOrder(job, tenantBean, lifecycle);
    order.verify(tenantBean).register();
    order.verify(lifecycle).bootTenant();
  }

  @Test // CLM-25317
  public void shouldNotFailAllBeanRegistrations_whenOneJobFails() throws Exception {
    TenantManaged tenantManaged1 = mock(TenantManaged.class);
    doThrow(new RuntimeException("expected exception")).when(tenantManaged1).register();
    tenantManagedBeans.add(tenantManaged1);

    TenantManaged tenantManaged2 = mock(TenantManaged.class);
    tenantManagedBeans.add(tenantManaged2);

    setTenantAndAssertRegistration();

    verify(tenantManaged2).register();
  }

  @Test // CLM-25317
  public void shouldNotMarkTenantAsRegistered_whenRegistrationFails() {
    when(tenantValidator.validateTenantExists(any(Tenant.class))).thenThrow(new RuntimeException("Expected"));

    assertThatThrownBy(this::setTenantAndAssertRegistration).isNotNull();

    assertThat(underTest.isRegistered()).isFalse();
  }

  @Test
  public void shouldThrowException_whenTenantScheduledForDeletion() {
    when(deletedTenantDAO.isScheduledForDeletion(tenant.tenantSlug)).thenReturn(true);

    assertThatThrownBy(this::setTenantAndAssertRegistration).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(TENANT_DOES_NOT_EXIST);

    assertThat(underTest.isRegistered()).isFalse();
  }

  @Test
  public void performDatabaseRegistrationAndRun() {
    Supplier<Boolean> supplier = mock(Supplier.class);

    doAnswer(invocationOnMock -> {
      assertTenantSet(tenant);
      return null;
    }).when(databaseProvisioner).initializeDatabaseWithoutMigration();

    underTest.performDatabaseRegistrationAndRunAs(tenant.tenantSlug, supplier);

    verify(supplier, times(1)).get();
    verify(databaseProvisioner).initializeDatabaseWithoutMigration();
  }

  @Test
  public void performDatabaseRegistrationAndRun_tenantNotFound() {
    when(tenantValidator.validateTenantExists(any(Tenant.class)))
        .thenThrow(new IllegalArgumentException(TENANT_DOES_NOT_EXIST));

    assertThatThrownBy(() -> underTest.performDatabaseRegistrationAndRunAs(tenant.tenantSlug, () -> true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(TENANT_DOES_NOT_EXIST);
  }

  @Test
  public void performDatabaseRegistrationAndRun_tenantNull() {
    assertThatThrownBy(() -> underTest.performDatabaseRegistrationAndRunAs(null, () -> true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(TENANT_PARAMETER_CANNOT_BE_NULL);
  }

  @Test
  public void performDatabaseRegistrationAndRun_tenantEmpty() {
    assertThatThrownBy(() -> underTest.performDatabaseRegistrationAndRunAs("", () -> true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(TENANT_PARAMETER_CANNOT_BE_NULL);
  }

  private void setTenantAndAssertRegistration() {
    doAnswer(invocationOnMock -> {
      assertTenantSet(tenant);
      return null;
    }).when(job).register();

    underTest.setTenant(tenant);

    verify(job).register();
    verify(lifecycle).bootTenant();
    verify(databaseProvisioner).initializeDatabaseWithoutMigration();
  }

  private static class MockTenantManaged
      implements TenantManaged, GlobalTenantJob
  {
    // We need a mock that implements GlobalTenantJob and TenantManaged
  }
}
