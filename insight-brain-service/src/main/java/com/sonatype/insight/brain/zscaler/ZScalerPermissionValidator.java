/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ZScalerPermissionValidator
{
  private final Logger log = LoggerFactory.getLogger(ZScalerPermissionValidator.class);

  private final ZScalerClient zScalerClient;

  private enum ZScalerPermission
  {
    CUSTOM_URL_CAT("category", "create"),
    OVERRIDE_EXISTING_CAT("URL", "update");

    private final String testElement;

    private final String operation;

    ZScalerPermission(String testElement, String operation) {
      this.testElement = testElement;
      this.operation = operation;
    }

    public String getTestElement() {
      return testElement;
    }

    public String getOperation() {
      return operation;
    }
  }

  @Inject
  public ZScalerPermissionValidator(final ZScalerClient zScalerClient) {
    this.zScalerClient = zScalerClient;
  }

  public void validatePermissions(String baseUrl) {
    long timestamp = System.currentTimeMillis();
    String testCategoryName = "sonatype-permission-test-" + timestamp;
    String categoryId = null;

    try {
      categoryId = createTestCategory(baseUrl, testCategoryName, timestamp);
      updateTestCategory(baseUrl, testCategoryName, categoryId, timestamp);

      log.info("Permission validation successful: " +
          "user has required CUSTOM_URL_CAT and OVERRIDE_EXISTING_CAT permissions");
    }
    finally {
      cleanupTestCategory(baseUrl, categoryId, testCategoryName);
    }
  }

  private String createTestCategory(String baseUrl, String testCategoryName, long timestamp) {
    log.info("Testing CUSTOM_URL_CAT permission by creating test category: {}", testCategoryName);

    // ZScaler expects URLs without the protocol prefix (no https://)
    String testUrl = "links.sonatype.com/zscaler-validation/url-create-" + timestamp;

    ZScalerOperationResult<ZScalerCategory> result =
        zScalerClient.createCustomUrlCategory(baseUrl, testCategoryName, List.of(testUrl));

    if (!result.isSuccess()) {
      handleError(result, ZScalerPermission.CUSTOM_URL_CAT);
    }

    ZScalerCategory createdCategory = result.getData().orElse(null);
    if (createdCategory == null || createdCategory.getId() == null) {
      throw new BadRequestException("Failed to get ID from created test category. " +
          "This may indicate insufficient permissions or a ZScaler API issue.");
    }

    log.info("Successfully created test category with ID: {}", createdCategory.getId());
    return createdCategory.getId();
  }

  private void updateTestCategory(String baseUrl, String testCategoryName, String categoryId, long timestamp) {
    log.info("Testing OVERRIDE_EXISTING_CAT permission by updating test category");

    // ZScaler expects URLs without the protocol prefix (no https://)
    String testUrl = "links.sonatype.com/zscaler-validation/url-update-" + timestamp;

    ZScalerOperationResult<Void> result =
        zScalerClient.updateCustomUrlCategories(baseUrl, testCategoryName, categoryId, List.of(testUrl));

    if (!result.isSuccess()) {
      handleError(result, ZScalerPermission.OVERRIDE_EXISTING_CAT);
    }

    log.info("Successfully updated test category");
  }

  private void handleError(ZScalerOperationResult<?> result, ZScalerPermission permissionTested) {
    log.warn("Permission test failed for {}: {}", permissionTested, result.getMessage());

    if (result.isForbidden()) {
      if (permissionTested == ZScalerPermission.OVERRIDE_EXISTING_CAT) {
        throw new BadRequestException(
            "Insufficient ZScaler permissions. The user has CUSTOM_URL_CAT permission (create succeeded) " +
                "but lacks OVERRIDE_EXISTING_CAT permission (update failed). " +
                "The user account must have both permissions with READ_WRITE access."
        );
      }
      else {
        throw new BadRequestException(
            "Insufficient ZScaler permissions for " + permissionTested + ". The user account must have both " +
                "CUSTOM_URL_CAT and OVERRIDE_EXISTING_CAT permissions with READ_WRITE access."
        );
      }
    }

    if (result.isBadRequest()) {
      if (result.isQuotaError()) {
        throw new BadRequestException(
            "ZScaler quota is full. Unable to " + permissionTested.getOperation() + " test " +
                permissionTested.getTestElement() + ". " +
                "Please free up quota in ZScaler and try the test again."
        );
      }

      throw new BadRequestException(
          "Failed to validate ZScaler permission " + permissionTested + ". " +
              "ZScaler rejected the request: " + result.getMessage()
      );
    }

    throw new BadRequestException(
        "Failed to validate ZScaler permission " + permissionTested + " with status " + result.getStatusCode());
  }

  private void cleanupTestCategory(String baseUrl, String categoryId, String testCategoryName) {
    if (categoryId == null) {
      return;
    }

    log.info("Cleaning up test category: {}", testCategoryName);
    ZScalerOperationResult<Void> result = zScalerClient.deleteCustomUrlCategory(baseUrl, categoryId);

    if (!result.isSuccess()) {
      log.error("Failed to delete test category '{}': {}", testCategoryName, result.getMessage());
      throw new BadRequestException(
          "ZScaler permission validation failed for test category " + testCategoryName +
              " so it will have to be manually deleted. " +
              "Provided credentials can create and update categories but cannot delete them. " +
              "This indicates a misconfigured role in ZScaler. " +
              "The role should have CUSTOM_URL_CAT with full READ_WRITE access."
      );
    }

    log.info("Successfully deleted test category");
  }
}
