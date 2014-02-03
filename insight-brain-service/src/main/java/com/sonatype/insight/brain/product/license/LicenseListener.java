/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

/**
 * Hook to be get notified of changes to the server's product license.
 * 
 * @since 1.9
 */
public interface LicenseListener
{
  /**
   * Notifies the listener that the license has changed (e.g. got installed or uninstalled). Querying the
   * {@link CLMLicenseManager} at this point will indicate the state of the current license.
   */
  void licenseChanged();
}
