/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.lang.reflect.Method;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.product.license.entitlement.EntitlementRequiredException;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class LicenseAwareContainerDynamicFeature
    implements DynamicFeature
{
  private final ProductLicense productLicense;

  private final BaseUrl baseUrl;

  @Inject
  public LicenseAwareContainerDynamicFeature(ProductLicense productLicense, BaseUrl baseUrl) {
    this.productLicense = productLicense;
    this.baseUrl = baseUrl;
  }

  private class Filter
      implements ContainerRequestFilter
  {
    private final Logger log = LoggerFactory.getLogger(Filter.class);

    private final LicensedFeature feature;

    private final LicensedFeature entitlement;

    public Filter(LicensedFeature feature, LicensedFeature entitlement) {
      this.feature = feature;
      this.entitlement = entitlement;
    }

    private boolean isLifecycleProduct() {
      return CLMLicenseManager.hasLifecycleProduct(productLicense);
    }

    @Override
    public void filter(final ContainerRequestContext request) throws IOException {
      String path = request.getUriInfo().getPath();

      try {
        productLicense.validate();
        if (feature != null) {
          productLicense.validateFeature(feature);
        }
        // Entitlement check only applies to Lifecycle products (Pro/Enterprise/Legacy).
        // Non-Lifecycle products (Firewall, SBOM Manager, etc.) skip this check entirely
        // because tier-gated features are Lifecycle-specific.
        if (entitlement != null && isLifecycleProduct() && !productLicense.hasFeature(entitlement)) {
          throw new EntitlementRequiredException(entitlement);
        }
      }
      catch (InvalidLicenseException e) {
        log.error(e.getMessage(), e);

        // we want to redirect if going to an html page when unlicensed, unless of course they are going to the main
        // html page
        if (path.endsWith("index.html") && !path.equals(InsightBrainService.BRAIN_ASSET_PATH + "index.html")) {
          throw new WebApplicationException(Response.seeOther(
              baseUrl.redirect().path(InsightBrainService.BRAIN_ASSET_PATH).path("index.html").build()).build());
        }
        else {
          throw e;
        }
      }
    }
  }

  @Override
  public void configure(final ResourceInfo resourceInfo, final FeatureContext featureContext) {
    // If the method is unlicensed,
    // or if the resource (class) is unlicensed AND the method is NOT looking for enforcement points,
    // then DO NOT register a filter
    // Note that ResourceInfo.getResourceClass() and ResourceInfo.getResourceMethod() may return proxied classes/methods
    // without any annotations unless they're inherited, so make sure any annotations we're checking for are @Inherited
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();
    if (resourceMethod.isAnnotationPresent(UnlicensedPath.class) ||
        (resourceClass.isAnnotationPresent(UnlicensedPath.class) &&
            !resourceMethod.isAnnotationPresent(ProductLicenseEnforcementPoint.class)))
    {
      return;
    }

    ProductLicenseEnforcementPoint ep = resourceMethod.getAnnotation(ProductLicenseEnforcementPoint.class);
    if (ep == null) {
      // method level enforcement annos will override whatever is in the resource, so don't check unless necessary
      ep = resourceClass.getAnnotation(ProductLicenseEnforcementPoint.class);
    }

    RequiresEntitlement entitlementAnnotation = resourceMethod.getAnnotation(RequiresEntitlement.class);
    LicensedFeature entitlement = entitlementAnnotation != null ? entitlementAnnotation.value() : null;

    featureContext.register(new Filter(ep != null ? ep.value() : null, entitlement));
  }
}
