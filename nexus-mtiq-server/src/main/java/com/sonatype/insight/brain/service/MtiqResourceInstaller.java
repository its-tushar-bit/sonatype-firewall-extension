/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;

/**
 * Utility class for determining resource registration type for MTIQ.
 * Used by Jersey configuration to determine:
 * <ul>
 * <li>MTIQ admin endpoints (marked with @MtiqAdminEndpoint) - these go in the admin bundle</li>
 * <li>IQ-only endpoints (marked with @IqOnlyEndpoint) - these should not be available in MTIQ</li>
 * <li>Regular endpoints - go in the main Jersey bundle</li>
 * </ul>
 */
public final class MtiqResourceInstaller
{
  public enum RegistrationDestination
  {
    MAIN,
    ADMIN,
    EXCLUDED
  }

  private MtiqResourceInstaller() {
    // Utility class
  }

  /**
   * Check if a class is an admin resource for MTIQ.
   *
   * @param type the class to check
   * @return true if the class is marked as an MTIQ admin endpoint
   */
  public static boolean isAdminResource(final Class<?> type) {
    return type.isAnnotationPresent(MtiqAdminEndpoint.class);
  }

  /**
   * Check if a class is an IQ-only resource that should not be available in MTIQ.
   *
   * @param type the class to check
   * @return true if the class is marked as an IQ-only endpoint
   */
  public static boolean isIqOnlyResource(final Class<?> type) {
    return type.isAnnotationPresent(IqOnlyEndpoint.class);
  }

  /**
   * Check if a class should be registered as a regular Jersey resource.
   * A class is a regular resource if it has @Path but is not an admin or IQ-only resource.
   *
   * @param type the class to check
   * @return true if the class should be registered in the main Jersey bundle
   */
  public static boolean isRegularResource(final Class<?> type) {
    return type.isAnnotationPresent(Path.class) && !isAdminResource(type) && !isIqOnlyResource(type);
  }

  public static RegistrationDestination register(
      final Object component,
      final ResourceConfig resourceConfig,
      final AdminResourceBundle adminResourceBundle)
  {
    Class<?> type = ClassUtils.getUserClass(AopUtils.getTargetClass(component));

    if (isIqOnlyResource(type)) {
      return RegistrationDestination.EXCLUDED;
    }
    if (isAdminResource(type)) {
      adminResourceBundle.register(component);
      return RegistrationDestination.ADMIN;
    }

    resourceConfig.register(component);
    return RegistrationDestination.MAIN;
  }
}
