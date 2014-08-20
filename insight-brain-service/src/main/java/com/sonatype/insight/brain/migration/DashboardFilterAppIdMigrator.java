/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.DashboardFilterDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.12
 */
@Named
public class DashboardFilterAppIdMigrator
{
  private static final Logger log = LoggerFactory.getLogger(DashboardFilterAppIdMigrator.class);

  private InsightWork work;

  private ApplicationDAO appDAO = new ApplicationDAO();

  private DashboardFilterDAO filterDAO = new DashboardFilterDAO();

  @Inject
  public DashboardFilterAppIdMigrator(InsightWork work) {
    this.work = work;
  }

  void migrate() throws IOException {
    File markerFile = new File(work.getAuditDir(), "filter-app-ids");

    if (markerFile.exists()) {
      log.debug("filter-app-ids exists, skipping migration");
      return;
    }

    long start = System.currentTimeMillis();
    for (DashboardFilter filter : filterDAO.getAll()) {
      log.debug("Updating filter for {}", filter.getUsername());
      try {
        DashboardFilterDTO dto = JsonUtils.parse(filter.getFilter(), DashboardFilterDTO.class);
        if (dto.applicationFilters != null) {
          List<String> byId = new LinkedList<String>();
          for (String appPublicId : dto.applicationFilters) {
            log.trace("Looking for application {}", appPublicId);
            Application app = appDAO.getByPublicId(appPublicId);
            if (app != null) {
              byId.add(app.getId());
            }
            else {
              log.trace("Missing application {}", appPublicId);
            }
          }
          dto.applicationFilters = byId;

          filter.setFilter(JsonUtils.format(dto));
          filterDAO.update(filter);
        }
      }
      catch (IOException e) {
        log.error("Failed to migrate dashboard filter for user {}", filter.getUsername(), e);
      }
    }

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();

    log.info("Finished updating modified flags in {} ms.", System.currentTimeMillis() - start);
  }

}
