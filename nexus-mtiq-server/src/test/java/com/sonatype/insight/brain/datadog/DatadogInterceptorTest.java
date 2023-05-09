/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.datadog;

import java.util.Collection;

import datadog.trace.api.DDTags;
import datadog.trace.api.interceptor.MutableSpan;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.datadog.DatadogInterceptor.POSTGRESQL_QUERY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DatadogInterceptorTest
{
  @Test
  public void tenantSelect() {
    String inputResource = "SELECT t0.system_configuration_property_id, t0.name, t0.value FROM " +
        "t_tenant_1.system_configuration_property t0 WHERE (t0.name = ?)";
    String expectedResource = "SELECT t0.system_configuration_property_id, t0.name, t0.value FROM " +
        "t_TENANT.system_configuration_property t0 WHERE (t0.name = ?)";

    runTest(inputResource, expectedResource);
  }

  @Test
  public void tenantUpdate() {
    String inputResource = "UPDATE t_tenant_1.persisted_user_session SET session_json = ? WHERE " +
        "persisted_user_session_id IN (SELECT DISTINCT t0.persisted_user_session_id FROM " +
        "t_tenant_1.persisted_user_session t0 WHERE (t0.persisted_user_session_id = ?))";
    String expectedResource = "UPDATE t_TENANT.persisted_user_session SET session_json = ? WHERE " +
        "persisted_user_session_id IN (SELECT DISTINCT t0.persisted_user_session_id FROM " +
        "t_TENANT.persisted_user_session t0 WHERE (t0.persisted_user_session_id = ?))";

    runTest(inputResource, expectedResource);
  }

  @Test
  public void tenantInsert() {
    String inputResource = "INSERT INTO t_tenant_1.license_threat_group_license (license_threat_group_license_id, " +
        "license_id, license_threat_group_id, owner_id) VALUES (?, ?, ?, ?)";
    String expectedResource = "INSERT INTO t_TENANT.license_threat_group_license (license_threat_group_license_id, " +
        "license_id, license_threat_group_id, owner_id) VALUES (?, ?, ?, ?)";

    runTest(inputResource, expectedResource);
  }

  @Test
  public void tenantDelete() {
    String inputResource = "DELETE FROM t_tenant_1.system_configuration_property WHERE " +
        "system_configuration_property_id = ?";
    String expectedResource = "DELETE FROM t_TENANT.system_configuration_property WHERE " +
        "system_configuration_property_id = ?";

    runTest(inputResource, expectedResource);
  }

  @Test
  public void noTenant() {
    // but has a 't_'
    String inputResource = "SELECT t0.reverse_proxy_authentication_configuration_id, t0.csrf_protection_disabled, " +
        "t0.enabled, t0.logout_url, t0.username_header FROM global.reverse_proxy_authentication_configuration t0 " +
        "WHERE (t0.reverse_proxy_authentication_configuration_id = ?)";
    String expectedResource = inputResource;

    runTest(inputResource, expectedResource);
  }

  @Test
  public void noQueryAtAll() {
    String inputResource = "GET /foo";
    String expectedResource = inputResource;

    runTest(inputResource, expectedResource);
  }

  private void runTest(final String inputResource, final String expectedResource) {
    runTest(inputResource, expectedResource, POSTGRESQL_QUERY);
  }

  private void runTest(final String inputResource, final String expectedResource, final String operationName) {
    DatadogInterceptor datadogInterceptor = new DatadogInterceptor();

    MutableSpan span = createMockSpan(operationName, inputResource);
    Collection<MutableSpan> traceCollection = Lists.newArrayList(span);

    // invoke
    datadogInterceptor.onTraceComplete(traceCollection);

    // NOTE: With the Datadog SDK we can only code against an interface. While asserting a specific method was never
    // invoked (i.e. the test being aware of the implementation is an anti-pattern), there really isn't much else we
    // can do here.

    // if it wasn't a query we don't even expect a call on getResourceName
    if (!POSTGRESQL_QUERY.contentEquals(operationName)) {
      verify(span, never()).getResourceName();
      verify(span, never()).setTag(anyString(), anyString());
    }
    else {
      // if expect output is the same as input it should not have attempted to set the tag at all
      if (inputResource.equals(expectedResource)) {
        verify(span, never()).setTag(anyString(), anyString());
      }
      else {
        verify(span, times(1)).setTag(eq(DDTags.RESOURCE_NAME), eq(expectedResource));
      }
    }
  }

  private MutableSpan createMockSpan(final String operationName, final String resourceName) {
    MutableSpan span = Mockito.mock(MutableSpan.class);
    Mockito.when(span.getOperationName()).thenReturn(operationName);
    Mockito.when(span.getResourceName()).thenReturn(resourceName);
    return span;
  }
}
