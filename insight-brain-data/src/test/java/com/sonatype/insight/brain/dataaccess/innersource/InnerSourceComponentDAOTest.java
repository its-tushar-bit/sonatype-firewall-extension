/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceComponentDAOTest
    extends AbstractDbDAOTest
{
  private final InnerSourceComponentDAO dao = new InnerSourceComponentDAO();

  private static final Comparator<InnerSourceComponent> INNER_SOURCE_COMPONENT_COMPARATOR =
      Comparator.comparing(InnerSourceComponent::getApplicationId)
          .thenComparing(InnerSourceComponent::getPackageUrl)
          .thenComparing(InnerSourceComponent::getId);

  @Test
  public void testCRUD() throws Exception {
    String purl = "pkg:maven/inner/source@1.0.0";

    // Create
    InnerSourceComponent innerSourceComponent = new InnerSourceComponent(application.getId(), purl);
    dao.insert(innerSourceComponent);
    assertThat(innerSourceComponent.getId()).isNotNull();

    // Get
    innerSourceComponent = dao.getById(innerSourceComponent.getId());
    assertThat(innerSourceComponent).isNotNull();
    assertInnerSourceComponent(application.getId(), purl, innerSourceComponent);

    // Update
    Application newApplication = tempEntity.newApplication(application.getOrganizationId());
    innerSourceComponent.setApplicationId(newApplication.getId());
    dao.update(innerSourceComponent);
    assertInnerSourceComponent(newApplication.getId(), purl, innerSourceComponent);

    // Delete
    dao.delete(innerSourceComponent);

    // Get
    innerSourceComponent = dao.getById(innerSourceComponent.getId());
    assertThat(innerSourceComponent).isNull();
  }

  @Test
  public void testGetByApplicationId() {
    final Application application2 = tempEntity.newApplication(application.getOrganizationId());

    InnerSourceComponent innerSourceComponent1 =
        tempEntity.newInnerSourceComponent("pkg:maven/inner/source@1.0.0", application);
    InnerSourceComponent
        innerSourceComponent2 = tempEntity.newInnerSourceComponent("pkg:maven/inner/source@2.0.0", application);
    tempEntity.newInnerSourceComponent("pkg:maven/inner/source@3.0.0", application2);

    List<InnerSourceComponent> innerSourceComponents = dao.getByApplicationId(application.getId());
    assertThat(innerSourceComponents).hasSize(2);
    assertInnerSourceComponent(innerSourceComponent1, innerSourceComponents.get(0));
    assertInnerSourceComponent(innerSourceComponent2, innerSourceComponents.get(1));
  }

  @Test
  public void testGetByPurl() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/source@1.0.0");

    InnerSourceComponent innerSourceComponent1 = tempEntity.newInnerSourceComponent(purl.getPackageUrl(), application);
    tempEntity.newInnerSourceComponent("pkg:maven/inner/source@2.0.0", application);

    InnerSourceComponent innerSourceComponent = dao.getByPackageUrl(purl);
    assertThat(innerSourceComponent).isNotNull();
    assertInnerSourceComponent(innerSourceComponent1, innerSourceComponent);
  }

  @Test
  public void testDeleteByApplicationId() {
    Application applicationTest1 = tempEntity.newApplication(organization.getId());
    Application applicationTest2 = tempEntity.newApplication(organization.getId());

    tempEntity.newInnerSourceComponent("pkg:maven/inner/source@1.0.0", applicationTest1);
    tempEntity.newInnerSourceComponent("pkg:maven/inner/source@2.0.0", applicationTest1);
    tempEntity.newInnerSourceComponent("pkg:maven/inner/source@3.0.0", applicationTest2);

    int deletedRows = 0;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      deletedRows = dao.deleteByApplicationId(tx, applicationTest1.getId());
      tx.commit();
    }

    assertThat(deletedRows).isEqualTo(2);
    assertThat(dao.getByApplicationId(applicationTest1.getId())).isEmpty();
    assertThat(dao.getByApplicationId(applicationTest2.getId())).hasSize(1);
  }

  private void assertInnerSourceComponent(
      String application,
      String purl,
      InnerSourceComponent component)
  {
    assertThat(component.getApplicationId()).isEqualTo(application);
    assertThat(component.getPackageUrl()).isEqualTo(purl);
  }

  public void assertInnerSourceComponent(InnerSourceComponent expected, InnerSourceComponent actual) {
    assertThat(actual).isNotNull();
    assertThat(actual).usingComparator(INNER_SOURCE_COMPONENT_COMPARATOR).isEqualTo(expected);
  }
}
