/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(VersionResource.SERVICE_PATH)
@UnlicensedPath
public class VersionResource
{
  public static final String SERVICE_PATH = "rest/version";

  private static final Logger log = LoggerFactory.getLogger(VersionResource.class);

  private static final String FILE_NAME = "version.properties";

  private static Properties properties;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Properties getVersionInfo() throws Exception {
    return get();
  }

  public synchronized static Properties get() throws IOException {
    if (properties == null) {
      properties = new Properties();
      InputStream is = VersionResource.class.getResourceAsStream(FILE_NAME);
      if (is != null) {
        try {
          properties.load(is);
        }
        catch (IOException e) {
          log.error(e.getMessage(), e);
        }
        finally {
          IOUtil.close(is);
        }
      } else {
        log.error("Missing properties file {}", FILE_NAME);
      }
    }
    return properties;

  }
}
