/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove hash:null from modified flags created by the license override
 * 
 * @since 1.11
 */
@Named
public class NullHashModifiedMigrator
{
  private static final Logger log = LoggerFactory.getLogger(NullHashModifiedMigrator.class);

  static final String MARKER_FILE_NAME = "null-hash-license-modified";

  private InsightWork work;

  private ApplicationDAO appDAO = new ApplicationDAO();

  private OrganizationDAO orgDAO = new OrganizationDAO();

  @Inject
  public NullHashModifiedMigrator(InsightWork work) {
    this.work = work;
  }

  void migrate() throws IOException {
    File markerFile = new File(work.getAuditDir(), MARKER_FILE_NAME);

    if (markerFile.exists()) {
      log.debug("BOM modified flags already migrated.");
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Updating modified flags...");
    for (Application app : appDAO.getAll()) {
      log.debug("Checking application {}", app.getPublicId());

      try {
        migrate(app.getId());
      }
      catch (IOException e) {
        log.error("Failed to update modified flags for application {}", app.getPublicId(), e);
      }
    }
    for (Organization org : orgDAO.getAll()) {
      log.debug("Checking application {}", org.getId());

      try {
        migrate(org.getId());
      }
      catch (IOException e) {
        log.error("Failed to update modified flags for organization {}", org.getName(), e);
      }
    }
    log.info("Finished updating modified flags in {} ms.", System.currentTimeMillis() - start);

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();
  }

  private void migrate(String id) throws IOException {
    File bom = new File(work.getAuditDir(id), "bom.json");
    if (bom.exists()) {
      final ArrayNode content = JsonUtils.read(bom);

      for (int i = 0; i < content.size(); i++) {
        ObjectNode node = (ObjectNode) content.get(i);

        log.debug("Checking node {}", node);
        if (node.path("data").isObject()) {
          node = (ObjectNode) node.get("data");
          if (node.path("hash").isNull()) {
            log.debug("Removing hash");
            node.remove("hash");
          }
        }
      }
      JsonUtils.write(bom, content);
    }
  }
}
