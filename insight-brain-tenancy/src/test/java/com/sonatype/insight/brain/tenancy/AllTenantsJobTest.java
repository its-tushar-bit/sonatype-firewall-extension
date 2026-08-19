/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.tenancy.AllTenantsJob.TENANT_LIST;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantUtil.IS_MTIQ_BATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class AllTenantsJobTest
    extends MultiTenantTestSupport
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }

  @Mock
  JobExecutionContext jobExecutionContext;

  @Mock
  JobDetail jobDetail;

  @Mock
  JobDataMap jobDataMap;

  List<String> tenantList = new ArrayList<>();

  StubbedAllTenantsJob underTest;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();

    underTest = new StubbedAllTenantsJob(true);

    AllTenantsJob.tenantUtil.mtiqBatchMode = true;

    lenient().when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
    lenient().when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    lenient().when(jobDataMap.get(TENANT_LIST)).thenReturn(tenantList);
  }

  @Test
  public void testExecutesForTenantWhenSingleTenant() {
    testAsSingleTenant(t -> {
      underTest.execute(jobExecutionContext);

      assertThat(underTest.tenantsUsed).containsExactly(SINGLE_TENANT);
    });
  }

  @Test
  public void testNoopWhenNotMtiqBatchModeButMultiTenant() {
    AllTenantsJob.tenantUtil.mtiqBatchMode = false;

    testAsGlobalTenant(t -> {
      underTest.execute(jobExecutionContext);

      assertThat(underTest.tenantsUsed).isEmpty();
    });
  }

  @Test
  public void testRunsForAllTenantsInMtiqBatchMode() {
    environmentVariables.set(IS_MTIQ_BATCH, "true");

    List<Tenant> expectedTenants = new ArrayList<>();

    testAsNewTenant(t1 -> {
      tenantList.add(t1.tenantSlug);
      expectedTenants.add(t1);
    });

    testAsNewTenant(t2 -> {
      tenantList.add(t2.tenantSlug);
      expectedTenants.add(t2);
    });

    testAsGlobalTenant(g -> {
      underTest.execute(jobExecutionContext);

      assertThat(underTest.tenantsUsed).containsExactlyInAnyOrder(expectedTenants.toArray(new Tenant[]{}));
    });
  }

  @Test
  public void testNoOpWhenTenantNotLicensed() {
    environmentVariables.set(IS_MTIQ_BATCH, "true");

    underTest = new StubbedAllTenantsJob(false);

    List<Tenant> expectedTenants = new ArrayList<>();

    testAsNewTenant(t1 -> {
      tenantList.add(t1.tenantSlug);
      expectedTenants.add(t1);
    });

    testAsNewTenant(t2 -> {
      tenantList.add(t2.tenantSlug);
      expectedTenants.add(t2);
    });

    testAsGlobalTenant(g -> {
      underTest.execute(jobExecutionContext);

      assertThat(underTest.tenantsUsed).isEmpty();
    });
  }

  private class StubbedAllTenantsJob
      implements AllTenantsJob
  {
    private final boolean licensed;

    List<Tenant> tenantsUsed = new ArrayList<>();

    public StubbedAllTenantsJob(boolean licensed) {
      this.licensed = licensed;
    }

    @Override
    public void executeForTenant(JobExecutionContext context, Tenant tenant) {
      tenantsUsed.add(tenant);
    }

    @Override
    public boolean isLicensed() {
      return licensed;
    }
  }
}
