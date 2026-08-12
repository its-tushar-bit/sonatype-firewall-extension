/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReportService#isHostedRepositoryComponent(String)}, the sole hosted-scan
 * lookup on {@link ReportService}. Exercises the two branches: {@code proxy_repository_component}
 * row present (true) and absent (false).
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportServiceHostedComponentTest
{
  @Mock
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @InjectMocks
  private ReportService reportService;

  @Test
  public void isHostedRepositoryComponent_returnsTrueWhenComponentExists() {
    when(proxyRepositoryComponentDAO.getByScanId("scan1")).thenReturn(newComponent("repo1", "lib.jar", "abc123"));

    assertThat(reportService.isHostedRepositoryComponent("scan1")).isTrue();
  }

  @Test
  public void isHostedRepositoryComponent_returnsFalseWhenNotFound() {
    when(proxyRepositoryComponentDAO.getByScanId("none")).thenReturn(null);

    assertThat(reportService.isHostedRepositoryComponent("none")).isFalse();
  }

  private static ProxyRepositoryComponent newComponent(String repositoryId, String pathname, String hash) {
    ProxyRepositoryComponent c = new ProxyRepositoryComponent();
    c.setRepositoryId(repositoryId);
    c.setPathname(pathname);
    c.setHash(hash);
    return c;
  }
}
