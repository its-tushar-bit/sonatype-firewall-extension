/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
