/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class LicenseAwareContainerResourceFilterFactory
    implements ResourceFilterFactory
{
  @Inject
  private CLMLicenseManager licenseManager;

  private class Filter
      implements ResourceFilter, ContainerRequestFilter
  {
    private final Set<CLMEnforcementPoint> enforcementPoints;

    private final Logger log = LoggerFactory.getLogger(Filter.class);

    @Context
    private BaseUrl baseUrl;

    public Filter(Set<CLMEnforcementPoint> enforcementPoints) {
      this.enforcementPoints = enforcementPoints;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
      String path = request.getPath();

      try {
        licenseManager.validate();
        licenseManager.validateAnyEnforcementPoint(enforcementPoints);
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

      return request;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
      return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
      return null;
    }
  }

  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    // If the method is unlicensed, simply return null, no filter
    // If the resource is unlicensed, make sure the method isn't looking for enforcement points, if not, return null, no
    // filter
    if (am.isAnnotationPresent(UnlicensedPath.class)
        || (am.getResource().isAnnotationPresent(UnlicensedPath.class) && !am
            .isAnnotationPresent(ProductLicenseEnforcementPoint.class))) {
      return null;
    }

    Set<CLMEnforcementPoint> enforcementPoints = new HashSet<CLMEnforcementPoint>();

    ProductLicenseEnforcementPoint ep = am.getAnnotation(ProductLicenseEnforcementPoint.class);

    if (ep != null) {
      enforcementPoints.addAll(Arrays.asList(ep.value()));
    }

    // method level enforcement annos will override whatever is in the resource, so dont check unless necessary
    if (enforcementPoints.isEmpty()) {
      ep = am.getResource().getAnnotation(ProductLicenseEnforcementPoint.class);

      if (ep != null) {
        enforcementPoints.addAll(Arrays.asList(ep.value()));
      }
    }

    return Collections.<ResourceFilter> singletonList(new Filter(enforcementPoints));
  }
}
