/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.service.AssetPaths;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.license.model.LicensedFeature;
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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

@Named
public class LicenseAwareContainerDynamicFeature
    implements DynamicFeature
{
  private static final Logger log = LoggerFactory.getLogger(LicenseAwareContainerDynamicFeature.class);

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

    private final LicensedFeature[] features;

    private final LicensedFeature entitlement;

    public Filter(LicensedFeature[] features, LicensedFeature entitlement) {
      this.features = features;
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
        if (features != null && features.length > 0) {
          // Enforcement point is satisfied when the license has any one of the declared features (OR).
          productLicense.validateFeatures(features);
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
        if (path.endsWith("index.html") && !path.equals(AssetPaths.BRAIN_ASSET_PATH + "index.html")) {
          throw new WebApplicationException(Response.seeOther(
              baseUrl.redirect().path(AssetPaths.BRAIN_ASSET_PATH).path("index.html").build()).build());
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
    // then DO NOT register a filter.
    // ResourceInfo may expose Spring-generated proxy types that do not carry method-level annotations, so resolve
    // annotations against the user class and corresponding user-declared method where possible.
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Class<?> userClass = ClassUtils.getUserClass(resourceClass);
    Method resourceMethod = resourceInfo.getResourceMethod();
    Method userMethod = resourceMethod;
    if (resourceMethod != null && userClass != resourceClass) {
      try {
        userMethod = userClass.getMethod(resourceMethod.getName(), resourceMethod.getParameterTypes());
      }
      catch (NoSuchMethodException e) {
        log.warn("Could not resolve user method for {}.{}, falling back to proxy method",
            userClass.getSimpleName(), resourceMethod.getName());
      }
    }

    if (userMethod.isAnnotationPresent(UnlicensedPath.class) ||
        (userClass.isAnnotationPresent(UnlicensedPath.class) &&
            !userMethod.isAnnotationPresent(ProductLicenseEnforcementPoint.class)))
    {
      return;
    }

    ProductLicenseEnforcementPoint ep = userMethod.getAnnotation(ProductLicenseEnforcementPoint.class);
    if (ep == null) {
      // method level enforcement annos will override whatever is in the resource, so don't check unless necessary
      ep = userClass.getAnnotation(ProductLicenseEnforcementPoint.class);
    }

    RequiresEntitlement entitlementAnnotation = userMethod.getAnnotation(RequiresEntitlement.class);
    LicensedFeature entitlement = entitlementAnnotation != null ? entitlementAnnotation.value() : null;

    LicensedFeature[] features = ep != null
        ? Stream.concat(Stream.of(ep.value()), Arrays.stream(ep.anyOf())).toArray(LicensedFeature[]::new)
        : null;
    featureContext.register(new Filter(features, entitlement));
  }
}
