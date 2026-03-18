/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceApplicationDAOTest
    extends AbstractDbDAOTest
{
  private InnerSourceApplicationDAO dao;

  private InnerSourceVersionDAO versionDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createInnerSourceApplicationDAO();
    versionDAO = daoFactory.createInnerSourceVersionDAO();
  }

  @Test
  public void testCRUD() {
    String purl = "pkg:maven/inner/source1";

    // Create
    InnerSourceApplication innerSourceApplication = new InnerSourceApplication(application.getId(), purl);
    dao.insert(innerSourceApplication);
    assertThat(innerSourceApplication.getId()).isNotNull();

    // Get
    InnerSourceApplication storedInnerSourceApplication = dao.getById(innerSourceApplication.getId());
    assertThat(storedInnerSourceApplication).isNotNull();
    JPA.assertEntityEquals(storedInnerSourceApplication, innerSourceApplication);

    // Update
    Application newApplication = tempEntity.newApplication(application.getOrganizationId());
    innerSourceApplication.setApplicationId(newApplication.getId());
    dao.update(innerSourceApplication);
    storedInnerSourceApplication = dao.getById(innerSourceApplication.getId());
    JPA.assertEntityEquals(storedInnerSourceApplication, innerSourceApplication);

    // Delete
    dao.delete(innerSourceApplication);

    // Get
    innerSourceApplication = dao.getById(innerSourceApplication.getId());
    assertThat(innerSourceApplication).isNull();
  }

  @Test
  public void testGetByApplicationId() {
    final Application application2 = tempEntity.newApplication(application.getOrganizationId());

    InnerSourceApplication innerSourceApplication1 =
        tempEntity.newInnerSourceApplication("pkg:maven/inner/source1", application);
    InnerSourceApplication innerSourceApplication2 =
        tempEntity.newInnerSourceApplication("pkg:maven/inner/source2", application);
    tempEntity.newInnerSourceApplication("pkg:maven/inner/source3", application2);

    List<InnerSourceApplication> innerSourceApplications = dao.getByApplicationId(application.getId());
    assertThat(innerSourceApplications).hasSize(2);
    JPA.assertEntityEquals(innerSourceApplications.get(0), innerSourceApplication1);
    JPA.assertEntityEquals(innerSourceApplications.get(1), innerSourceApplication2);
  }

  @Test
  public void testGetByPurl() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/source1");

    InnerSourceApplication innerSourceApplication1 =
        tempEntity.newInnerSourceApplication(purl.getPackageUrl(), application);
    tempEntity.newInnerSourceApplication("pkg:maven/inner/source", application);

    InnerSourceApplication innerSourceApplication = dao.getByPackageUrl(purl);
    assertThat(innerSourceApplication).isNotNull();
    JPA.assertEntityEquals(innerSourceApplication, innerSourceApplication1);
  }

  @Test
  public void testGetByPurl_appExcluded() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/source1");

    tempEntity.newInnerSourceApplication(purl.getPackageUrl(), application);
    tempEntity.newInnerSourceApplication("pkg:maven/inner/source2", application);

    InnerSourceApplication innerSourceApplication = dao.getByPackageUrlExcludingApplication(purl, application.getId());
    assertThat(innerSourceApplication).isNull();
  }

  @Test
  public void testGetByPurl_appNotExcluded() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/sourceTest1");

    Application newApp = tempEntity.newApplicationWithParent();

    InnerSourceApplication expected = tempEntity.newInnerSourceApplication(purl.getPackageUrl(), application);
    tempEntity.newInnerSourceApplication("pkg:maven/inner/sourceTest2", application);

    InnerSourceApplication innerSourceApplication = dao.getByPackageUrlExcludingApplication(purl, newApp.getId());
    assertThat(innerSourceApplication).isNotNull();
    JPA.assertEntityEquals(innerSourceApplication, expected);
  }

  @Test
  public void testGetByPurls() {
    PackageUrlIdentifier purl1 = new PackageUrlIdentifier("pkg:maven/inner/source1");
    PackageUrlIdentifier purl2 = new PackageUrlIdentifier("pkg:maven/inner/source2");

    InnerSourceApplication innerSourceApplication1 =
        tempEntity.newInnerSourceApplication(purl1.getPackageUrl(), application);
    InnerSourceApplication innerSourceApplication2 =
        tempEntity.newInnerSourceApplication(purl2.getPackageUrl(), application);

    tempEntity.newInnerSourceApplication("pkg:maven/inner/source3 ", application);

    List<InnerSourceApplication> innerSourceApplications = dao.getByPackageUrls(Sets.newHashSet(purl1, purl2));

    assertThat(innerSourceApplications).hasSize(2);
    JPA.assertEntityEquals(innerSourceApplications.get(0), innerSourceApplication1);
    JPA.assertEntityEquals(innerSourceApplications.get(1), innerSourceApplication2);
  }

  @Test
  public void testDelete_CascadesToInnerSource() {
    InnerSourceApplication innerSourceApplication = tempEntity.newInnerSourceApplication("pkg:test/name", application);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", StageTypes.RELEASE.getId());

    assertThat(versionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).isNotEmpty();

    dao.delete(innerSourceApplication);

    assertThat(versionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())).isEmpty();
  }
}
