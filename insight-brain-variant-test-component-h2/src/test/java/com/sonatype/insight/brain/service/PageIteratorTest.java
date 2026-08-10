/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class PageIteratorTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApplicationDAO applicationDAO;

  @Test
  public void testHasNextAndNext_Empty() {
    PageIterator<Application> pageIterator = new PageIterator<>(1, 2, applicationDAO::getAll);

    assertThat(getAll(pageIterator)).isEmpty();
  }

  @Test
  public void testHasNextAndNext_One() {
    PageIterator<Application> pageIterator = new PageIterator<>(1, 2, applicationDAO::getAll);
    Application application = tempEntity.newApplicationWithParent("app1");

    assertThat(getAll(pageIterator)).extracting(Application::getId).containsExactly(application.getId());
  }

  @Test
  public void testHasNextAndNext_OnePage() {
    PageIterator<Application> pageIterator = new PageIterator<>(1, 2, applicationDAO::getAll);
    Application app1 = tempEntity.newApplicationWithParent("app1");
    Application app2 = tempEntity.newApplicationWithParent("app2");

    assertThat(getAll(pageIterator)).extracting(Application::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }

  @Test
  public void testHasNextAndNext_TwoPages() {
    PageIterator<Application> pageIterator = new PageIterator<>(1, 2, applicationDAO::getAll);
    Application app1 = tempEntity.newApplicationWithParent("app1");
    Application app2 = tempEntity.newApplicationWithParent("app2");
    Application app3 = tempEntity.newApplicationWithParent("app3");

    assertThat(getAll(pageIterator)).extracting(Application::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId(), app3.getId());
  }

  @Test
  public void testHasNextAndNext_StartFromPageTwo() {
    PageIterator<Application> pageIterator = new PageIterator<>(2, 2, applicationDAO::getAll);
    tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationWithParent("app2");
    Application app3 = tempEntity.newApplicationWithParent("app3");

    assertThat(getAll(pageIterator)).extracting(Application::getId).containsExactly(app3.getId());
  }

  @Test
  public void testHasNextAndNext_StartFromPageBeyondAllResults() {
    PageIterator<Application> pageIterator = new PageIterator<>(3, 2, applicationDAO::getAll);
    tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationWithParent("app2");
    tempEntity.newApplicationWithParent("app3");

    assertThat(getAll(pageIterator)).isEmpty();
  }

  @Test
  public void testHasNextAndNext_PageSizeHasAllResults() {
    PageIterator<Application> pageIterator = new PageIterator<>(1, 3, applicationDAO::getAll);
    Application app1 = tempEntity.newApplicationWithParent("app1");
    Application app2 = tempEntity.newApplicationWithParent("app2");
    Application app3 = tempEntity.newApplicationWithParent("app3");

    assertThat(getAll(pageIterator)).extracting(Application::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId(), app3.getId());
  }

  @Test
  public void testHasNextAndNext_PageSizeHasMoreThanAllResults() {
    PageIterator<Application> pageIterator = new PageIterator<>(1, 4, applicationDAO::getAll);
    Application app1 = tempEntity.newApplicationWithParent("app1");
    Application app2 = tempEntity.newApplicationWithParent("app2");
    Application app3 = tempEntity.newApplicationWithParent("app3");

    assertThat(getAll(pageIterator)).extracting(Application::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId(), app3.getId());
  }

  @Test
  public void testPageIterator_InvalidArguments() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new PageIterator<>(0, 2, applicationDAO::getAll))
        .withMessageContaining("Page must be at least 1.");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new PageIterator<>(-1, 2, applicationDAO::getAll))
        .withMessageContaining("Page must be at least 1.");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new PageIterator<>(1, 0, applicationDAO::getAll))
        .withMessageContaining("Page size must be at least 1.");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new PageIterator<>(1, -1, applicationDAO::getAll))
        .withMessageContaining("Page size must be at least 1.");
  }

  private List<Application> getAll(final PageIterator<Application> pageIterator) {
    List<Application> result = new ArrayList<>();
    while (pageIterator.hasNext()) {
      result.add(pageIterator.next());
    }
    return result;
  }
}
