/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

/**
 * Hook to be get notified of changes to the server's product license.
 *
 * @since 1.9
 */
public interface ProductLicenseListener
{
  /**
   * Notifies the listener that the license has changed (e.g. got installed or uninstalled). Querying the
   * {@link ProductLicense} at this point will indicate the state of the current license.
   */
  void productLicenseChanged();
}
