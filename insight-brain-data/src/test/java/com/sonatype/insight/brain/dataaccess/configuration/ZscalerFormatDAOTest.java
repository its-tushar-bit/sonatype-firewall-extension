/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ZscalerFormatDAOTest
    extends AbstractDbDAOTest
{
  private ZscalerFormatDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createZscalerFormatDAO();
    tempEntity.newZScalerConfiguration("user", "password", "host", "validapikey1", true, true, false, true);
  }

  @Test
  public void testGetAll() {
    List<ZscalerFormat> formats = dao.getAll();
    assertThat(formats).isNotEmpty();
    for (ZscalerFormat format : formats) {
      switch (format.getFormat()) {
        case "maven", "npm", "nuget" -> assertThat(format.isEnabled()).isTrue();
        case "pypi" -> assertThat(format.isEnabled()).isFalse();
        default -> throw new AssertionError("Unexpected format: " + format.getFormat());
      }
    }
  }
}
