/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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

@Named
@Path(VersionResource.SERVICE_PATH)
@UnlicensedPath
public class VersionResource
{
  public static final String SERVICE_PATH = "rest/version";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Properties getVersionInfo() throws Exception {
    return get();
  }

  public static Properties get() throws IOException {
    Properties props = new Properties();
    InputStream is = VersionResource.class.getResourceAsStream("version.properties");
    try {
      props.load(is);
    }
    finally {
      IOUtil.close(is);
    }
    return props;

  }
}
