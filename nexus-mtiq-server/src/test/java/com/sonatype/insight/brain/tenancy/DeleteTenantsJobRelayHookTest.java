/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.relay.RelayRegistrationService;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level coverage of the relay deregistration hook added in {@code DeleteTenantsJob}: it must
 * run before {@code deleteDatabaseSchema} (so the encrypted API key is still readable) and must
 * not block schema drop when the relay throws.
 */
@RunWith(MockitoJUnitRunner.class)
public class DeleteTenantsJobRelayHookTest
{
  @Mock
  private MultiTenantTaskScheduler taskScheduler;

  @Mock
  private TenantManager tenantManager;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private RelayRegistrationService relayRegistrationService;

  @Mock
  private DeletedTenantDAO deletedTenantDAO;

  private DeleteTenantsJob job;

  private final DeletedTenant deletedTenant = new DeletedTenant("acme", new Date());

  @Before
  public void before() {
    job = spy(new DeleteTenantsJob(taskScheduler, null, deletedTenantDAO, null, null, null, null,
        tenantManager, tenantValidator, null, relayRegistrationService));
    // Have performDatabaseRegistrationAndRunAs run the supplier inline.
    doAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(1)).get())
        .when(tenantManager)
        .performDatabaseRegistrationAndRunAs(anyString(), any());
    when(tenantValidator.validateTenantExists(anyString())).thenReturn(true);
    // Stub heavy substeps so the test is hermetic.
    doReturn(true).when(job).deleteAuth0Resources(any());
    doReturn(true).when(job).deleteJobs(any());
    doReturn(true).when(job).deleteDatabaseSchema(any());
    doReturn(true).when(job).deleteFilesOnDisk(any());
  }

  @Test
  public void deleteTenant_callsRelayDeregisterBeforeSchemaDrop() {
    job.deleteTenant(deletedTenant);

    InOrder order = inOrder(relayRegistrationService, job);
    order.verify(relayRegistrationService, times(1)).deregisterTenant();
    order.verify(job).deleteDatabaseSchema(deletedTenant);
  }

  @Test
  public void deleteTenant_relayDeregisterThrows_schemaStillDropped() {
    doThrow(new RuntimeException("relay down")).when(relayRegistrationService).deregisterTenant();

    job.deleteTenant(deletedTenant);

    verify(job).deleteDatabaseSchema(deletedTenant);
  }

  @Test
  public void deleteTenant_jobsDeletionFails_skipsRelayDeregister() {
    // Symmetric to the Auth0 test: when deleteJobs returns false, deletion returns early
    // before reaching deregisterRelay. Documents the contract so a future refactor would
    // fail this test if the guard order changes.
    doReturn(false).when(job).deleteJobs(any());

    job.deleteTenant(deletedTenant);

    verify(relayRegistrationService, never()).deregisterTenant();
  }

  @Test
  public void deleteTenant_auth0DeletionFails_skipsRelayDeregister() {
    // When deleteAuth0Resources returns false, the deletion flow returns early before
    // reaching deregisterRelay (intentional, see DeleteTenantsJob.deleteTenant).
    // Documents the contract so a future refactor moving deregisterRelay above the
    // Auth0/jobs guards would fail this test.
    doReturn(false).when(job).deleteAuth0Resources(any());

    job.deleteTenant(deletedTenant);

    verify(relayRegistrationService, never()).deregisterTenant();
  }

  @Test
  public void deleteTenant_tenantSchemaAbsent_skipsRelayDeregister() {
    // When the schema is already gone (e.g. a prior partially-successful deletion), the
    // relay-side queue and registration are intentionally not cleaned up by this job:
    // out-of-band cleanup is required. Documents the load-bearing skip behaviour.
    when(tenantValidator.validateTenantExists(anyString())).thenReturn(false);

    job.deleteTenant(deletedTenant);

    verify(relayRegistrationService, never()).deregisterTenant();
  }

  @Test
  public void deleteTenant_dbRegistrationThrows_schemaStillDropped() {
    doThrow(new RuntimeException("schema not registered"))
        .when(tenantManager)
        .performDatabaseRegistrationAndRunAs(anyString(), any());

    job.deleteTenant(deletedTenant);

    verify(job).deleteDatabaseSchema(deletedTenant);
  }
}
