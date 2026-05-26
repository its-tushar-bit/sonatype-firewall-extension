/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQ configuration for database access.
 */
@Configuration
public class JooqConfiguration
{

  private static final Logger log = LoggerFactory.getLogger(JooqConfiguration.class);

  @Bean
  public DSLContext dslContext(DataSource dataSource) {
    SQLDialect dialect = SQLDialect.DEFAULT;
    try (Connection conn = dataSource.getConnection()) {
      String url = conn.getMetaData().getURL();
      if (url != null) {
        if (url.startsWith("jdbc:h2:")) {
          dialect = SQLDialect.H2;
        }
        else if (url.startsWith("jdbc:postgresql:")) {
          dialect = SQLDialect.POSTGRES;
        }
      }
    }
    catch (Exception e) {
      log.warn("Could not determine SQL dialect from DataSource, using DEFAULT", e);
    }
    return DSL.using(dataSource, dialect);
  }
}
