/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.RootOrganizationNode;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.IconDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateOwnerTest
    extends AbstractFunctionalTest
{
  private static final String APP_PUBLIC_ID = "a..bcd";

  private static final String NAME = "gibberish";

  private ApplicationDAO appDAO = new ApplicationDAO();

  private IconDAO iconDAO = new IconDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private static Organization parentOrg;

  private static final int IMAGE_RESIZE_WIDTH = 52;

  private static final int IMAGE_RESIZE_HEIGHT = 52;

  @BeforeClass
  public static void beforeClass() {
    parentOrg = staticTempEntity.newOrganization("Parent");
    refreshOrOpen(OwnerSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    refreshOrOpen(OwnerSummaryPage.url());
  }

  @After
  public void cleanup() {
    Organization org = organizationDAO.getByName(NAME);
    if (org != null) {
      organizationDAO.delete(org);
    }

    Application app = appDAO.getByPublicId(APP_PUBLIC_ID);
    if (app != null) {
      appDAO.delete(app);
    }
  }

  @Test
  public void testCreateApplication() throws Exception {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode orgNode = OwnerTreeView.organization(0);
    orgNode.treeViewElement().click();
    orgNode.newApplicationButton().shouldBe(visible, enabled).click();

    testNoDirtyState();

    orgNode = OwnerTreeView.organization(0);
    orgNode.newApplicationButton().shouldBe(visible, enabled).click();

    OwnerEditorDialog.name().shouldBe(focused);

    testIconDirtyState();

    // open application
    OwnerEditorDialog.name().shouldBe(visible, empty).shouldHave(CLM.PRISTINE);
    OwnerEditorDialog.publicId().shouldBe(visible, empty).shouldHave(CLM.PRISTINE);
    OwnerEditorDialog.saveButton().shouldBe(disabled);

    // check invalid name
    OwnerEditorDialog.name().val("$$$");
    popoverViolations(OwnerEditorDialog.name()).shouldBe(visible).shouldHave(text("Use valid characters"));

    // should not be able to proceed w/ name error
    OwnerEditorDialog.publicId().val(APP_PUBLIC_ID);
    OwnerEditorDialog.saveButton().shouldBe(disabled);
    // take focus off the input to prevent blinking cursor
    OwnerEditorDialog.titleIcon().click();
    eyesWatcher.eyesCheck("Create owner with validation error");

    // Error should get removed
    OwnerEditorDialog.name().val(NAME);
    popoverViolations(OwnerEditorDialog.name()).shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    // invalid id
    OwnerEditorDialog.publicId().val("a c d $$");
    OwnerEditorDialog.saveButton().shouldBe(disabled);
    popoverViolations(OwnerEditorDialog.publicId()).shouldBe(visible).shouldHave(text("Use valid characters"));

    // check error goes away
    OwnerEditorDialog.publicId().val(APP_PUBLIC_ID);
    popoverViolations(OwnerEditorDialog.publicId()).shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    OwnerEditorDialog.name().shouldNotHave(CLM.PRISTINE);
    OwnerEditorDialog.publicId().shouldNotHave(CLM.PRISTINE);

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));

    // check backend
    Application app = appDAO.getByPublicId(APP_PUBLIC_ID);
    assertThat(app).isNotNull();
    assertThat(app.getPublicId()).isEqualTo(APP_PUBLIC_ID);
    assertThat(app.getOrganizationId()).isEqualTo(parentOrg.getId());
    assertThat(app.getName()).isEqualTo(NAME);

    orgNode.applicationElements().shouldHaveSize(1).get(0).shouldHave(text(NAME));
  }

  @Test
  public void testCreateAndEditApplication_withRobotIcon() throws Exception {
    testCreateApplication_withRobotIcon();
    testEditApplication_withRobotIcon();
  }

  private void testCreateApplication_withRobotIcon() throws Exception {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode orgNode = OwnerTreeView.organization(0);
    orgNode.treeViewElement().click();
    orgNode.newApplicationButton().shouldBe(visible, enabled).click();

    // fill form
    OwnerEditorDialog.name().val(NAME);
    OwnerEditorDialog.publicId().val(APP_PUBLIC_ID);
    OwnerEditorDialog.robotIcon().click();
    OwnerEditorDialog.RobotIconSelector.button().click();

    // ensure image is displayed
    assertImage(OwnerEditorDialog.RobotIconSelector.icon());
    String userSelectedImageSrc = OwnerEditorDialog.RobotIconSelector.icon().attr("src");
    BufferedImage userSelectedImage = fetchImage(userSelectedImageSrc);

    // submit the form
    OwnerEditorDialog.saveButton().shouldBe(enabled);
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // validate system is updated
    Application app = appDAO.getByPublicId(APP_PUBLIC_ID);
    assertThat(app).isNotNull();
    assertThat(app.getPublicId()).isEqualTo(APP_PUBLIC_ID);
    assertThat(app.getOrganizationId()).isEqualTo(parentOrg.getId());
    assertThat(app.getName()).isEqualTo(NAME);

    // validate the selected image is displayed
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    orgNode.applicationElements().shouldHaveSize(1).get(0).shouldHave(text(NAME));
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the same as image that was selected and displayed
    BufferedImage persistedImage = readImage(OwnerType.APPLICATION, app.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedImage, persistedImage);
  }

  private void testEditApplication_withRobotIcon() throws Exception {
    ActionDropDown.actionButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    ActionDropDown.editOwner().shouldHave(text("App")).click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text("Application"));

    // select a robot image
    OwnerEditorDialog.robotIcon().click();
    OwnerEditorDialog.RobotIconSelector.button().click();

    // validate image is displayed
    assertImage(OwnerEditorDialog.RobotIconSelector.icon());
    String userSelectedImageSrc = OwnerEditorDialog.RobotIconSelector.icon().attr("src");
    BufferedImage userSelectedImage = fetchImage(userSelectedImageSrc);

    // save the form with updated image
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // validate system is updated
    Application app = appDAO.getByPublicId(APP_PUBLIC_ID);
    assertThat(app).isNotNull();

    // validate the selected image is displayed
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the same as image that was selected and displayed
    BufferedImage persistedImage = readImage(OwnerType.APPLICATION, app.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedImage, persistedImage);
  }

  @Test
  public void testCreateAndEditOrganization_withRobotIcon() throws Exception {
    testCreateOrganization_withRobotIcon();
    testEditOrganization_withRobotIcon();
  }

  private void testEditOrganization_withRobotIcon() throws Exception {
    ActionDropDown.actionButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    ActionDropDown.editOwner().shouldHave(text("Org")).click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text("Organization"));

    // select a robot image
    OwnerEditorDialog.robotIcon().click();
    OwnerEditorDialog.RobotIconSelector.button().click();

    // validate image is displayed
    assertImage(OwnerEditorDialog.RobotIconSelector.icon());
    String userSelectedImageSrc = OwnerEditorDialog.RobotIconSelector.icon().attr("src");
    BufferedImage userSelectedImage = fetchImage(userSelectedImageSrc);

    // save the updated icon
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // check frontend
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image persisted and displayed is same as image that was selected
    Organization org = organizationDAO.getByName(NAME);
    assertThat(org).isNotNull();

    BufferedImage persistedImage = readImage(OwnerType.ORGANIZATION, org.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedImage, persistedImage);
  }

  private void testCreateOrganization_withRobotIcon() throws Exception {
    RootOrganizationNode.newOrganizationButton().shouldBe(visible, enabled).click();

    // select a robot image
    OwnerEditorDialog.robotIcon().click();
    OwnerEditorDialog.RobotIconSelector.button().click();

    // validate image is displayed
    assertImage(OwnerEditorDialog.RobotIconSelector.icon());
    String userSelectedImageSrc = OwnerEditorDialog.RobotIconSelector.icon().attr("src");
    BufferedImage userSelectedImage = fetchImage(userSelectedImageSrc);

    // fill in organization data
    OwnerEditorDialog.name().val(NAME);
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // check backend
    Organization org = organizationDAO.getByName(NAME);
    assertThat(org).isNotNull();

    // check frontend
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image persisted is same as image that was selected
    BufferedImage persistedImage = readImage(OwnerType.ORGANIZATION, org.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedImage, persistedImage);
  }

  @Test
  public void testCreateOrganization() throws Exception {
    RootOrganizationNode.newOrganizationButton().shouldBe(visible, enabled).click();

    testNoDirtyState();

    RootOrganizationNode.newOrganizationButton().shouldBe(visible, enabled).click();

    OwnerEditorDialog.name().shouldBe(focused);

    testIconDirtyState();

    OwnerEditorDialog.name().shouldBe(visible, empty).shouldHave(CLM.PRISTINE);
    OwnerEditorDialog.publicId().shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(disabled);

    // check invalid name
    OwnerEditorDialog.name().val("$$$");
    popoverViolations(OwnerEditorDialog.name()).shouldBe(visible).shouldHave(text("Use valid characters"));

    // should not be able to proceed w/ name error
    OwnerEditorDialog.saveButton().shouldBe(disabled);

    // Error should get removed
    OwnerEditorDialog.name().val(NAME);
    popoverViolations(OwnerEditorDialog.name()).shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // check backend
    Organization org = organizationDAO.getByName(NAME);
    assertThat(org).isNotNull();

    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));

    OwnerTreeView.organizationElements().shouldHaveSize(2);
    OwnerTreeView.organizationElements().findBy(text(NAME));
  }

  private void testIconDirtyState() {
    OwnerEditorDialog.robotIcon().click();

    UnsavedModal unsavedModal = new UnsavedModal();
    refreshOrOpen(ReportListPage.url());
    unsavedModal.cancelButton().shouldBe(visible).click();

    OwnerEditorDialog.defaultIcon().click();
  }

  private void testNoDirtyState() {
    OwnerEditorDialog.defaultIcon().shouldBe(visible).shouldBe(selected);

    UnsavedModal unsavedModal = new UnsavedModal();
    refreshOrOpen(ReportListPage.url());
    unsavedModal.shouldBe(hidden);
    ReportListPage.listContainer().should(appear);
    Selenide.back();
  }

  private void assertImageEquals(BufferedImage image1, BufferedImage image2) throws IOException {
    BufferedImage resizedImage1 = resizeImage(image1, image1.getType());
    byte[] resizedImage1Bytes = bufferedImageToBytesArray(resizedImage1);
    BufferedImage resizedImage2 = resizeImage(image2, image2.getType());
    byte[] resizedImage2Bytes = bufferedImageToBytesArray(resizedImage2);
    assertThat(resizedImage1Bytes).isEqualTo(resizedImage2Bytes);
  }

  private BufferedImage resizeImage(BufferedImage originalImage, int type) {
    BufferedImage resizedImage = new BufferedImage(IMAGE_RESIZE_WIDTH, IMAGE_RESIZE_HEIGHT, type);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(originalImage, 0, 0, IMAGE_RESIZE_WIDTH, IMAGE_RESIZE_HEIGHT, null);
    g.dispose();
    return resizedImage;
  }

  private byte[] bufferedImageToBytesArray(BufferedImage image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);
    return baos.toByteArray();
  }

  private BufferedImage fetchImage(String urlString) throws IOException {
    HttpClient client = HttpClientBuilder.create().build();
    HttpGet get = new HttpGet(urlString);
    get.setHeader("Authorization",
        "Basic " + Base64.getEncoder().encodeToString("admin:admin123".getBytes(StandardCharsets.UTF_8)));
    HttpResponse response = client.execute(get);
    return ImageIO.read(response.getEntity().getContent());
  }

  private BufferedImage readImage(OwnerType ownerType, String ownerId) throws Exception {
    InsightWork insightWork = testCLMServer.getCLMServer().getInstance(InsightWork.class);
    return ImageIO.read(new ByteArrayInputStream(iconDAO.getIcon(ownerId,
        OwnerType.ORGANIZATION.equals(ownerType) ? insightWork.getOrganizationIconDir() : insightWork
            .getApplicationIconDir())));
  }

  private void assertImage(SelenideElement element) {
    element.shouldBe(new Condition("image")
    {
      @Override
      public boolean apply(WebElement ignored) {
        return element.isImage();
      }
    });
  }
}
