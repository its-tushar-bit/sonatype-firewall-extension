/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.support.SupportService.SupportFileType;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @since 1.27
 */
public class SupportResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SupportResource.RESOURCE_PATH);
  }

  @Test
  public void testCreateSupportZip() throws Exception {
    final HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    try (final InputStream inputStream = response.getBodyStream()) {
      assertThat(inputStream, notNullValue());

      try (final ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
        final ZipEntry zipEntry = zipInputStream.getNextEntry();
        assertThat(zipEntry.getName(), startsWith("support-"));
        assertThat(zipEntry.getName(), endsWith("/" + SupportFileType.CONFIG.dirName + "/filtered-config-test.yml"));

        final String served = IOUtil.toString(zipInputStream, "UTF-8").replace("\r\n", "\n");
        assertThat(served, is("showRootOrganization: true\n" +
            "logging:\n" +
            "  level: DEBUG\n" +
            "  loggers: {eu.medsea.mimeutil.MimeUtil2: INFO, org.apache.http: INFO, org.eclipse.jetty: INFO,\n" +
            "    com.ning.http.client: INFO, org.springframework.jdbc.datasource.init.ResourceDatabasePopulator: INFO,\n" +
            "    org.apache.directory: ERROR, com.sonatype.insight.error.ErrorResponseGenerator: TRACE,\n" +
            "    org.apache.shiro.realm.AuthenticatingRealm: INFO, org.springframework.jdbc.datasource.SimpleDriverDataSource: INFO,\n" +
            "    org.apache.commons.beanutils.converters: INFO}\n" +
            "  console: {logFormat: '%date %level [%thread%X{DC}] %logger - %msg%n'}\n"));
      }
    }
  }
}
