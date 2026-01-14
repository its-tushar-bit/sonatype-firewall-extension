/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.List;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZScalerPermissionValidatorTest
{
  @Mock
  private ZScalerClient mockZScalerClient;

  private ZScalerPermissionValidator underTest;

  @Before
  public void setup() {
    underTest = new ZScalerPermissionValidator(mockZScalerClient);
  }

  @Test
  public void shouldValidatePermissionsSuccessfully() {
    ZScalerCategory testCategory = new ZScalerCategory();
    testCategory.setId("test-123");
    testCategory.setConfiguredName("sonatype-permission-test-123");

    when(mockZScalerClient.createCustomUrlCategory(eq("http://example.com"), anyString(),
        any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200, testCategory));
    when(mockZScalerClient.updateCustomUrlCategories(eq("http://example.com"), anyString(),
        eq("test-123"), any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200));
    when(mockZScalerClient.deleteCustomUrlCategory(eq("http://example.com"), eq("test-123")))
        .thenReturn(ZScalerOperationResult.success(204));

    underTest.validatePermissions("http://example.com");

    // Should make 3 calls: create, update, delete
    verify(mockZScalerClient, times(1)).createCustomUrlCategory(eq("http://example.com"), anyString(),
        any(List.class));
    verify(mockZScalerClient, times(1)).updateCustomUrlCategories(eq("http://example.com"), anyString(),
        eq("test-123"), any(List.class));
    verify(mockZScalerClient, times(1)).deleteCustomUrlCategory(eq("http://example.com"), eq("test-123"));
  }

  @Test
  public void shouldThrowBadRequestExceptionWhenCreateCategoryFails() {
    when(mockZScalerClient.createCustomUrlCategory(eq("http://example.com"), anyString(), any(List.class)))
        .thenReturn(
            ZScalerOperationResult.failure(403, "Failed to create URL category: Forbidden - insufficient permissions"));

    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        underTest.validatePermissions("http://example.com")
    );

    assertTrue(exception.getMessage().contains("Insufficient ZScaler permissions"));
    assertTrue(exception.getMessage().contains("CUSTOM_URL_CAT"));

    verify(mockZScalerClient, times(0)).deleteCustomUrlCategory(anyString(), anyString());
  }

  @Test
  public void shouldThrowBadRequestExceptionWhenUpdateCategoryFails() {
    ZScalerCategory testCategory = new ZScalerCategory();
    testCategory.setId("test-123");
    testCategory.setConfiguredName("sonatype-permission-test-123");

    when(mockZScalerClient.createCustomUrlCategory(eq("http://example.com"), anyString(), any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200, testCategory));
    when(mockZScalerClient.updateCustomUrlCategories(eq("http://example.com"), anyString(), eq("test-123"),
        any(List.class)))
        .thenReturn(ZScalerOperationResult.failure(403,
            "Failed to update URL category: Forbidden - cannot override existing categories"));
    when(mockZScalerClient.deleteCustomUrlCategory(eq("http://example.com"), eq("test-123")))
        .thenReturn(ZScalerOperationResult.success(204));

    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        underTest.validatePermissions("http://example.com")
    );

    // Verify it's a permission error related to ZScaler permissions
    String message = exception.getMessage();
    assertTrue("Expected message to contain 'permission' but was: " + message,
        message.toLowerCase().contains("permission"));
    assertTrue("Expected message to contain 'ZScaler' or 'OVERRIDE_EXISTING_CAT' but was: " + message,
        message.contains("ZScaler") || message.contains("OVERRIDE_EXISTING_CAT"));

    verify(mockZScalerClient, times(1)).deleteCustomUrlCategory(eq("http://example.com"), eq("test-123"));
  }

  @Test
  public void shouldThrowBadRequestExceptionWhenDeleteCategoryFails() {
    ZScalerCategory testCategory = new ZScalerCategory();
    testCategory.setId("test-123");
    testCategory.setConfiguredName("sonatype-permission-test-123");

    when(mockZScalerClient.createCustomUrlCategory(eq("http://example.com"), anyString(), any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200, testCategory));
    when(mockZScalerClient.updateCustomUrlCategories(eq("http://example.com"), anyString(), eq("test-123"),
        any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200));
    when(mockZScalerClient.deleteCustomUrlCategory(eq("http://example.com"), eq("test-123")))
        .thenReturn(ZScalerOperationResult.failure(500, "Failed to delete URL category test-123: Connection timeout"));

    // Delete failures should now fail validation (no more quiet cleanup)
    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        underTest.validatePermissions("http://example.com")
    );

    assertTrue(exception.getMessage().contains("permission validation failed"));
    assertTrue(exception.getMessage().contains("manually deleted"));

    // Should make 3 calls: create, update, delete (fails)
    verify(mockZScalerClient, times(1)).createCustomUrlCategory(eq("http://example.com"), anyString(),
        any(List.class));
    verify(mockZScalerClient, times(1)).updateCustomUrlCategories(eq("http://example.com"), anyString(),
        eq("test-123"), any(List.class));
    verify(mockZScalerClient, times(1)).deleteCustomUrlCategory(eq("http://example.com"), eq("test-123"));
  }

  @Test
  public void shouldFailWhenCreateFailsDueToQuota() {
    when(mockZScalerClient.createCustomUrlCategory(eq("http://example.com"), anyString(), any(List.class)))
        .thenReturn(ZScalerOperationResult.failure(400, "Failed to create URL category: Quota limit exceeded"));

    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        underTest.validatePermissions("http://example.com")
    );

    assertTrue(exception.getMessage().contains("quota is full"));
    assertTrue(exception.getMessage().contains("Please free up quota"));

    verify(mockZScalerClient, times(0)).deleteCustomUrlCategory(anyString(), anyString());
  }

  @Test
  public void shouldFailWhenUpdateFailsDueToQuota() {
    ZScalerCategory testCategory = new ZScalerCategory();
    testCategory.setId("test-123");
    testCategory.setConfiguredName("sonatype-permission-test-123");

    when(mockZScalerClient.createCustomUrlCategory(eq("http://example.com"), anyString(), any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200, testCategory));
    when(mockZScalerClient.updateCustomUrlCategories(eq("http://example.com"), anyString(), eq("test-123"),
        any(List.class)))
        .thenReturn(
            ZScalerOperationResult.failure(400, "Failed to update URL category test-123: Quota limit exceeded"));
    when(mockZScalerClient.deleteCustomUrlCategory(eq("http://example.com"), eq("test-123")))
        .thenReturn(ZScalerOperationResult.success(204));

    // Quota errors now always fail validation to be honest about what we can validate
    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        underTest.validatePermissions("http://example.com")
    );

    assertTrue(exception.getMessage().contains("quota is full"));
    assertTrue(exception.getMessage().contains("Please free up quota"));

    // Should make 3 calls: create, update (fails), delete (cleanup in finally)
    verify(mockZScalerClient, times(1)).createCustomUrlCategory(eq("http://example.com"), anyString(),
        any(List.class));
    verify(mockZScalerClient, times(1)).updateCustomUrlCategories(eq("http://example.com"), anyString(),
        eq("test-123"), any(List.class));
    verify(mockZScalerClient, times(1)).deleteCustomUrlCategory(eq("http://example.com"), eq("test-123"));
  }

  @Test
  public void shouldUseRealUrlFormatInTests() {
    ZScalerCategory testCategory = new ZScalerCategory();
    testCategory.setId("test-123");
    testCategory.setConfiguredName("sonatype-permission-test-123");

    when(mockZScalerClient.createCustomUrlCategory(eq("http://example.com"), anyString(),
        any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200, testCategory));
    when(mockZScalerClient.updateCustomUrlCategories(eq("http://example.com"), anyString(),
        eq("test-123"), any(List.class)))
        .thenReturn(ZScalerOperationResult.success(200));
    when(mockZScalerClient.deleteCustomUrlCategory(eq("http://example.com"), eq("test-123")))
        .thenReturn(ZScalerOperationResult.success(204));

    underTest.validatePermissions("http://example.com");

    // Verify that test URLs use real URL format (not .invalid domain)
    verify(mockZScalerClient).createCustomUrlCategory(
        eq("http://example.com"),
        anyString(),
        any(List.class)
    );
    verify(mockZScalerClient).updateCustomUrlCategories(
        eq("http://example.com"),
        anyString(),
        eq("test-123"),
        any(List.class)
    );
  }
}
