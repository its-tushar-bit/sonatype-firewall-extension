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
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.IconDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.InsightWork;
import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateOwnerTest
    extends AbstractFunctionalTest
{
  private static final String APP_PUBLIC_ID = "a..bcd";

  private static final String NAME = "gibberish";

  private static final String NEW_NAME = "gibberish 2";

  private ApplicationDAO appDAO;

  private IconDAO iconDAO;

  private OrganizationDAO organizationDAO;

  private Organization parentOrg;

  private Organization org;

  private static final int IMAGE_RESIZE_WIDTH = 52;

  private static final int IMAGE_RESIZE_HEIGHT = 52;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    appDAO = lookup(ApplicationDAO.class);
    iconDAO = lookup(IconDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    parentOrg = tempEntity.newOrganization("Parent");
    org = tempEntity.newOrganization(NAME, parentOrg);
    refreshOrOpen(OwnerSummaryPage.url());
  }

  @Test
  @Ignore
  public void testCreateApplication() {
    OwnerTreeView.organizationElements().shouldHave(size(1));
    OrganizationNode orgNode = OwnerTreeView.organization(0);
    orgNode.treeViewElement().click();
    orgNode.newApplicationButton().shouldBe(visible, enabled).click();

    testNoDirtyState();

    orgNode = OwnerTreeView.organization(0);
    orgNode.newApplicationButton().shouldBe(visible, enabled).click();

    OwnerEditorDialog.name().shouldBe(focused);

    testIconDirtyState();

    // open application
    OwnerEditorDialog.nameDiv().shouldBe(visible).shouldHave(cssClass("pristine"));
    OwnerEditorDialog.name().shouldBe(visible, empty);
    OwnerEditorDialog.publicIdDiv().shouldBe(visible).shouldHave(cssClass("pristine"));
    OwnerEditorDialog.publicId().shouldBe(visible, empty);
    OwnerEditorDialog.saveButton().shouldBe(visible);

    // check invalid name
    OwnerEditorDialog.name().val("$$$");
    OwnerEditorDialog.nameInvalidMessage()
            .shouldBe(visible).shouldHave(text(OwnerEditorDialog.INVALID_NAME_MESSAGE));

    // should not be able to proceed w/ name error
    OwnerEditorDialog.publicId().val(APP_PUBLIC_ID);
    OwnerEditorDialog.saveButton().click();
    FormUtils.getAlertElement().shouldBe(visible)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));

    // take focus off the input to prevent blinking cursor
    OwnerEditorDialog.title().click();
    eyesWatcher.eyesCheck("Create owner with validation error");

    // Error should get removed
    OwnerEditorDialog.name().val(NAME);
    OwnerEditorDialog.nameInvalidMessage().shouldNotBe(visible);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    // invalid id
    OwnerEditorDialog.publicId().val("a c d $$");
    OwnerEditorDialog.publicIdInvalidMessage()
            .shouldBe(visible).shouldHave(text(OwnerEditorDialog.INVALID_PUBLICID_MESSAGE));

    // check error goes away
    OwnerEditorDialog.publicId().val(APP_PUBLIC_ID);
    OwnerEditorDialog.publicIdInvalidMessage().shouldNotBe(visible);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    OwnerEditorDialog.nameDiv().shouldNotHave(cssClass("pristine"));
    OwnerEditorDialog.publicIdDiv().shouldNotHave(cssClass("pristine"));

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));

    // check backend
    Application app = appDAO.getByPublicId(APP_PUBLIC_ID);
    assertThat(app).isNotNull();
    assertThat(app.getPublicId()).isEqualTo(APP_PUBLIC_ID);
    assertThat(app.getOrganizationId()).isEqualTo(parentOrg.getId());
    assertThat(app.getName()).isEqualTo(NAME);

    orgNode.applicationElements().shouldHave(size(1));
    orgNode.applicationElements().get(0).shouldHave(text(NAME));
  }

  @Test
  public void testCreateAndEditApplication_withRobotIcon() throws Exception {
    testCreateApplication_withRobotIcon();
    testEditApplication_withRobotIcon();
  }

  private void testCreateApplication_withRobotIcon() throws Exception {
    refreshOrOpen(OwnerSummaryPage.url(parentOrg));
    waitUntilUrl(OwnerSummaryPage.url(parentOrg));
    selectAddApplicationOption();

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

    // after application is created and model is closed we are redirected to that
    waitUntilUrl(OwnerSummaryPage.url(app));

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
  public void testEditApplication_resetToDefaultIcon() throws Exception {
    Application app = tempEntity.newApplication(NAME, APP_PUBLIC_ID, parentOrg.getId());
    refreshOrOpen(OwnerSummaryPage.url(app));
    waitUntilUrl(OwnerSummaryPage.url(app));

    // validate saved application info and grab reference to default image
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage originalDefaultImage = fetchImage(summaryTileHeaderIconSrc);

    // Edit icon to be a robot
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldHave(text("App")).click();
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

    // validate the selected image is displayed
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedRobotImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the same as image that was selected and displayed
    BufferedImage persistedImage = readImage(OwnerType.APPLICATION, app.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedRobotImage, persistedImage);

    // resetting icon back to default
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldHave(text("App")).click();
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // validate the selected image is displayed
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage imageAfterResettingToDefault = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the default image
    assertImageEquals(originalDefaultImage, imageAfterResettingToDefault);
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
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NEW_NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image persisted and displayed is same as image that was selected
    Organization org = organizationDAO.getByName(NEW_NAME);
    assertThat(org).isNotNull();

    BufferedImage persistedImage = readImage(OwnerType.ORGANIZATION, org.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedImage, persistedImage);
  }

  private void testCreateOrganization_withRobotIcon() throws Exception {
    refreshOrOpen(OwnerSummaryPage.url(parentOrg));
    waitUntilUrl(OwnerSummaryPage.url(parentOrg));

    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();

    orgsAndPoliciesSidebar.getChildOrganizations().shouldHave(size(1));
    selectAddOrganizationIcon(orgsAndPoliciesSidebar);

    // select a robot image
    OwnerEditorDialog.robotIcon().click();
    OwnerEditorDialog.RobotIconSelector.button().click();

    // validate image is displayed
    assertImage(OwnerEditorDialog.RobotIconSelector.icon());
    String userSelectedImageSrc = OwnerEditorDialog.RobotIconSelector.icon().attr("src");
    BufferedImage userSelectedImage = fetchImage(userSelectedImageSrc);

    // fill in organization data
    OwnerEditorDialog.name().val(NEW_NAME);
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // check backend
    Organization org = organizationDAO.getByName(NEW_NAME);
    assertThat(org).isNotNull();

    // check frontend - no redirect to newly created organization
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(parentOrg.getName()));
    assertThat(orgsAndPoliciesSidebar.getChildOrganizations()).hasSize(2);

    orgsAndPoliciesSidebar.getChildOrganizations().get(1).click();

    waitUntilUrl(OwnerSummaryPage.url(org));

    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NEW_NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image persisted is same as image that was selected
    BufferedImage persistedImage = readImage(OwnerType.ORGANIZATION, org.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedImage, persistedImage);
  }

  @Test
  public void testEditOrganization_resetToDefaultIcon() throws Exception {
    refreshOrOpen(OwnerSummaryPage.url(org));
    waitUntilUrl(OwnerSummaryPage.url(org));
    // grab reference to default image
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage originalDefaultImage = fetchImage(summaryTileHeaderIconSrc);

    // Edit icon to be a robot
    ActionDropDown.actionButton().click();
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

    // save the form with updated image
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // validate the selected image is displayed
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedRobotImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the same as image that was selected and displayed
    BufferedImage persistedImage = readImage(OwnerType.ORGANIZATION, org.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedRobotImage, persistedImage);

    // resetting icon back to default
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldHave(text("Org")).click();
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // validate the selected image is displayed
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NAME));
    assertImage(OwnerSummaryPage.summaryTile().headerIcon());
    summaryTileHeaderIconSrc = OwnerSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage imageAfterResettingToDefault = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the default image
    assertImageEquals(originalDefaultImage, imageAfterResettingToDefault);
  }

  @Test
  public void testCreateOrganization() {
    refreshOrOpen(OwnerSummaryPage.url(parentOrg));
    waitUntilUrl(OwnerSummaryPage.url(parentOrg));

    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    selectAddOrganizationIcon(orgsAndPoliciesSidebar);

    testNoDirtyState();

    refreshOrOpen(OwnerSummaryPage.url(parentOrg));
    waitUntilUrl(OwnerSummaryPage.url(parentOrg));

    selectAddOrganizationIcon(orgsAndPoliciesSidebar);
    OwnerEditorDialog.name().shouldBe(focused);

    testIconDirtyState();

    OwnerEditorDialog.nameDiv().shouldBe(visible).shouldHave(cssClass("pristine"));
    OwnerEditorDialog.name().shouldBe(visible, empty);
    OwnerEditorDialog.publicIdDiv().shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(visible);

    // check invalid name
    OwnerEditorDialog.name().val("$$$");
    OwnerEditorDialog.nameInvalidMessage().shouldBe(visible).shouldHave(text(OwnerEditorDialog.INVALID_NAME_MESSAGE));

    // Error should get removed
    OwnerEditorDialog.name().val(NEW_NAME);
    OwnerEditorDialog.nameInvalidMessage().shouldNotBe(visible);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // check backend
    Organization org = organizationDAO.getByName(NEW_NAME);
    assertThat(org).isNotNull();

    // check frontend - no redirect to newly created organization
    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(parentOrg.getName()));
    orgsAndPoliciesSidebar.getChildOrganizations().shouldHave(size(2));

    orgsAndPoliciesSidebar.getChildOrganizations().get(1).click();
    waitUntilUrl(OwnerSummaryPage.url(org));

    OwnerSummaryPage.summaryTile().name().should(appear).shouldHave(text(NEW_NAME));
  }

  private void selectAddOrganizationIcon(OrgsAndPoliciesSidebar orgsAndPoliciesSidebar) {
    SelenideElement organizationActionButton = orgsAndPoliciesSidebar.getOrganizationPlusIcon();
    organizationActionButton.shouldBe(visible);
    organizationActionButton.click();
  }

  private void testIconDirtyState() {
    OwnerEditorDialog.robotIcon().click();

    UnsavedModal unsavedModal = new UnsavedModal();
    OwnerEditorDialog.cancelButton().click();
    unsavedModal.cancelButton().shouldBe(visible).click();

    OwnerEditorDialog.defaultIcon().click();
  }

  private void testNoDirtyState() {
    OwnerEditorDialog.defaultIcon().shouldBe(visible).shouldBe(CLM.NX_RADIO_SELECTED);

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
    HttpGet get = new HttpGet(BaseUrl.convertContainerUrlToHostUrl(urlString));
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
    element.shouldBe(new WebElementCondition("image")
    {
      @Override
      public CheckResult check(Driver driver, WebElement ignored) {
        boolean isImage = element.isImage();
        return new CheckResult(isImage, element);
      }
    });
  }

  private void selectAddApplicationOption() {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    SelenideElement applicationsActionButton = orgsAndPoliciesSidebar.getApplicationPlusIcon();
    applicationsActionButton.shouldBe(visible);
    applicationsActionButton.shouldBe(enabled);
    applicationsActionButton.click();
    SelenideElement newApplicationButton = orgsAndPoliciesSidebar.getNewApplicationButton();
    newApplicationButton.shouldBe(visible);
    newApplicationButton.shouldBe(enabled);
    newApplicationButton.click();
  }
}
