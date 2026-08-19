/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.license.License;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(LicenseResource.RESOURCE_PATH)
public class LicenseResource
{
  public static final String RESOURCE_PATH = "rest/license";

  private final LicenseDAO licenseDAO;

  @Inject
  public LicenseResource(LicenseDAO licenseDAO) {
    this.licenseDAO = licenseDAO;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<License> getLicenses(@DefaultValue("false") @QueryParam("filterSynthetic") boolean filterSynthetic) {
    List<License> licenses = licenseDAO.getAll();
    if (filterSynthetic) {
      List<License> filteredLicenses = new ArrayList<>();
      for (License license : licenses) {
        if (!License.isEffectivelyUnspecified(license.getId())) {
          filteredLicenses.add(license);
        }
      }
      licenses = filteredLicenses;
    }
    return licenses;
  }
}
