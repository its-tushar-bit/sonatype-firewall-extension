/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.EmailConfigurationPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.test.MailboxTestUtil;

import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.Keys.BACK_SPACE;

public class EmailConfigurationPageTest
    extends AbstractFunctionalTest
{
  private MailConfigurationDAO mailConfigurationDAO;

  private final EmailConfigurationPage emailConfigurationPage = new EmailConfigurationPage();

  private final InsightMail insightMail = testCLMServer.getCLMServer().getInstance(InsightMail.class);

  private static final String FAKE_PASSWORD = "\u0000\u0000\u0000\u0000\u0000";

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(EmailConfigurationPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    mailConfigurationDAO = lookup(MailConfigurationDAO.class);
  }

  @Test
  public void testCrud() {
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.loadError().shouldNotBe(visible);

    // ## -- CREATE -- ##
    assertNoMailServerIsConfigured();
    eyesWatcher.eyesCheck("Email Configuration Page - Empty State");

    // Save an Email Configuration with minimal data
    emailConfigurationPage.hostName().setValue("smtp.myserver.com");
    emailConfigurationPage.port().setValue("465");
    emailConfigurationPage.systemEmail().setValue("no-reply@iqserver.com");
    saveConfiguration();

    // ## -- READ -- ##
    // Immediate state of page
    emailConfigurationPage.hostName().shouldBe(value("smtp.myserver.com"));
    emailConfigurationPage.port().shouldBe(value("465"));
    emailConfigurationPage.username().shouldBe(value(""));
    emailConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    emailConfigurationPage.systemEmail().shouldBe(value("no-reply@iqserver.com"));
    emailConfigurationPage.sslEnabled().shouldNotBe(selected);
    emailConfigurationPage.startTlsEnabled().shouldNotBe(selected);

    // State after refresh
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.hostName().shouldBe(value("smtp.myserver.com"));
    emailConfigurationPage.port().shouldBe(value("465"));
    emailConfigurationPage.username().shouldBe(value(""));
    emailConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    emailConfigurationPage.systemEmail().shouldBe(value("no-reply@iqserver.com"));
    emailConfigurationPage.sslEnabled().shouldNotBe(selected);
    emailConfigurationPage.startTlsEnabled().shouldNotBe(selected);

    assertThat(mailConfigurationDAO.get().getPassword()).isNull();

    // ## -- UPDATE -- ##
    emailConfigurationPage.username().setValue("admin");
    emailConfigurationPage.password().setValue("p");
    emailConfigurationPage.sslEnabled().click();
    emailConfigurationPage.startTlsEnabled().click();
    saveConfiguration();

    // Immediate state of page
    emailConfigurationPage.hostName().shouldBe(value("smtp.myserver.com"));
    emailConfigurationPage.port().shouldBe(value("465"));
    emailConfigurationPage.username().shouldBe(value("admin"));
    emailConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    emailConfigurationPage.systemEmail().shouldBe(value("no-reply@iqserver.com"));
    emailConfigurationPage.sslEnabled().shouldBe(selected);
    emailConfigurationPage.startTlsEnabled().shouldBe(selected);

    // State after refresh
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.hostName().shouldBe(value("smtp.myserver.com"));
    emailConfigurationPage.port().shouldBe(value("465"));
    emailConfigurationPage.username().shouldBe(value("admin"));
    emailConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    emailConfigurationPage.systemEmail().shouldBe(value("no-reply@iqserver.com"));
    emailConfigurationPage.sslEnabled().shouldBe(selected);
    emailConfigurationPage.startTlsEnabled().shouldBe(selected);

    assertThat(String.valueOf(insightMail.decryptPassword(mailConfigurationDAO.get().getPassword()))).isEqualTo("p");

    // ## -- DELETE -- ##
    emailConfigurationPage.delete().shouldBe(enabled).click();
    emailConfigurationPage.deleteModal().shouldBe(visible);
    emailConfigurationPage.deleteModal().cancel().shouldBe(enabled).click();
    emailConfigurationPage.deleteModal().shouldBe(hidden);

    // Assert not deleted
    // UI State should not be altered when I click cancel on delete modal
    emailConfigurationPage.hostName().shouldBe(value("smtp.myserver.com"));
    emailConfigurationPage.port().shouldBe(value("465"));
    emailConfigurationPage.username().shouldBe(value("admin"));
    emailConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    assertThat(String.valueOf(insightMail.decryptPassword(mailConfigurationDAO.get().getPassword()))).isEqualTo("p");
    emailConfigurationPage.systemEmail().shouldBe(value("no-reply@iqserver.com"));
    emailConfigurationPage.sslEnabled().shouldBe(selected);
    emailConfigurationPage.startTlsEnabled().shouldBe(selected);

    // Mail config must not be deleted
    assertThat(mailConfigurationDAO.get()).isNotNull();

    // Delete for real
    emailConfigurationPage.delete().shouldBe(enabled).click();
    emailConfigurationPage.deleteModal().shouldBe(visible);
    emailConfigurationPage.deleteModal().ok().shouldBe(enabled).click();
    emailConfigurationPage.deleteModal().shouldBe(hidden);
    assertNoMailServerIsConfigured();
  }

  @Test
  public void testHostnameOrPortUpdateRequiresPasswordToBeProvided() {
    tempEntity.newMailConfigurationWithNoAuthentication();

    refreshOrOpen(EmailConfigurationPage.url());

    // Test Hostname requires Password
    emailConfigurationPage.hostName().setValue("123");
    assertSavingProducesValidationErrorWithPasswordRequired();

    emailConfigurationPage.password().setValue("a-new-password");

    emailConfigurationPage.password().clear();

    // Reset
    emailConfigurationPage.cancel().shouldBe(enabled).click();

    // Password required when port is modified
    emailConfigurationPage.port().setValue("123");
    assertSavingProducesValidationErrorWithPasswordRequired();

    // Save is enabled when password provided
    emailConfigurationPage.password().setValue("a-new-password");

    // Save is enabled with empty password
    emailConfigurationPage.password().clear();
    resetForm();
  }

  @Test
  public void testRequiredValidationErrors() {
    refreshOrOpen(EmailConfigurationPage.url());
    saveConfiguration();

    FormUtils.getAlertElement(emailConfigurationPage)
        .shouldBe(visible)
        .shouldBe(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX +
            " Hostname, Port and System Email are required details."));
    emailConfigurationPage.hostName().setValue("smtp.myserver.com");
    emailConfigurationPage.port().setValue("465");
    emailConfigurationPage.systemEmail().setValue("no-reply@iqserver.com");
    saveConfiguration();
    emailConfigurationPage.save().shouldBe(visible);
  }

  // I must be able to update fields I am allowed to without providing my password
  // And if I do not modify the password, it must not be modified.
  @Test
  public void testUpdatePasswordIsNotModified() {
    MailConfiguration existing =
        tempEntity.newMailConfiguration("admin", insightMail.encryptPassword("password".toCharArray()));

    refreshOrOpen(EmailConfigurationPage.url());

    // These are all the values I am allowed to modify without providing my password
    emailConfigurationPage.username().setValue("new-username");
    emailConfigurationPage.systemEmail().setValue("new-system-email@something.com");
    emailConfigurationPage.sslEnabled().click();
    emailConfigurationPage.startTlsEnabled().click();

    saveConfiguration();
    emailConfigurationPage.save().shouldBe(visible);

    MailConfiguration updated = mailConfigurationDAO.get();
    // I did not provide a password in the form.
    // My password must NOT be overridden in backend.
    assertThat(insightMail.decryptPassword(updated.getPassword())).isEqualTo("password".toCharArray());

    // I did not modify hostname or port
    assertThat(existing.getHostname()).isEqualTo(updated.getHostname());
    assertThat(existing.getPort()).isEqualTo(updated.getPort());

    // I did update all these
    assertThat(updated.getUsername()).isEqualTo("new-username");
    assertThat(updated.getSystemEmail()).isEqualTo("new-system-email@something.com");
    assertThat(updated.isSslEnabled()).isFalse();
    assertThat(updated.isStartTlsEnabled()).isFalse();
  }

  @Test
  public void testUpdatePassword() {
    MailConfiguration existing =
        tempEntity.newMailConfiguration("admin", insightMail.encryptPassword("password".toCharArray()));

    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.password().setValue("new-password");
    saveConfiguration();

    // Verify UI state
    emailConfigurationPage.hostName().shouldBe(value(existing.getHostname()));
    emailConfigurationPage.port().shouldBe(value(String.valueOf(existing.getPort())));
    emailConfigurationPage.username().shouldBe(value("admin"));
    emailConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    emailConfigurationPage.systemEmail().shouldBe(value(existing.getSystemEmail()));
    emailConfigurationPage.sslEnabled().shouldBe(selected);
    emailConfigurationPage.startTlsEnabled().shouldBe(selected);

    // and database state
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    assertThat(insightMail.decryptPassword(mailConfiguration.getPassword())).isEqualTo("new-password".toCharArray());
    assertThat(mailConfiguration.getHostname()).isEqualTo(existing.getHostname());
    assertThat(mailConfiguration.getPort()).isEqualTo(existing.getPort());
    assertThat(mailConfiguration.getUsername()).isEqualTo(existing.getUsername());
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(existing.getSystemEmail());
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(existing.isSslEnabled());
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(existing.isStartTlsEnabled());
  }

  @Test
  public void testRemoveCredentials() {
    MailConfiguration existing =
        tempEntity.newMailConfiguration("a", insightMail.encryptPassword("password".toCharArray()));

    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.username().click();
    emailConfigurationPage.username().sendKeys(BACK_SPACE);

    // tests that password is selected and cleared with a single back space
    emailConfigurationPage.password().click();
    emailConfigurationPage.password().sendKeys(BACK_SPACE);

    saveConfiguration();

    // Verify UI State
    emailConfigurationPage.hostName().shouldBe(value(existing.getHostname()));
    emailConfigurationPage.port().shouldBe(value(String.valueOf(existing.getPort())));
    emailConfigurationPage.username().shouldBe(value(""));
    emailConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    emailConfigurationPage.systemEmail().shouldBe(value(existing.getSystemEmail()));
    emailConfigurationPage.sslEnabled().shouldBe(selected);
    emailConfigurationPage.startTlsEnabled().shouldBe(selected);

    // and database state
    MailConfiguration mailConfiguration = mailConfigurationDAO.get();
    assertThat(mailConfiguration.getUsername()).isNull();
    assertThat(mailConfiguration.getPassword()).isNull();
    assertThat(mailConfiguration.getHostname()).isEqualTo(existing.getHostname());
    assertThat(mailConfiguration.getPort()).isEqualTo(existing.getPort());
    assertThat(mailConfiguration.getSystemEmail()).isEqualTo(existing.getSystemEmail());
    assertThat(mailConfiguration.isSslEnabled()).isEqualTo(existing.isSslEnabled());
    assertThat(mailConfiguration.isStartTlsEnabled()).isEqualTo(existing.isStartTlsEnabled());
  }

  private void assertSavingProducesValidationErrorWithPasswordRequired() {
    saveConfiguration();
    FormUtils.getAlertElement(emailConfigurationPage)
        .shouldBe(visible)
        .shouldBe(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX +
            " Password must be provided when updating Hostname or Port."));
  }

  private void saveConfiguration() {
    emailConfigurationPage.save().click();
    FormMask.seeAndWaitForDismissal();
  }

  @Test
  public void testDirtyBehaviour() {
    refreshOrOpen(EmailConfigurationPage.url());

    // Form is said to be dirty if I make any change in any field.
    // Cancel button must be active for a dirty form, and clicking it must bring the form back to its original state
    dirtyBehaviourTextInput(emailConfigurationPage.hostName());

    emailConfigurationPage.port().setValue("1234");
    assertButtonsAndTooltipMessageAndClickCancel();
    emailConfigurationPage.port().shouldBe(value(""));

    dirtyBehaviourTextInput(emailConfigurationPage.username());
    dirtyBehaviourTextInput(emailConfigurationPage.password());
    dirtyBehaviourTextInput(emailConfigurationPage.systemEmail());
    dirtyBehaviourCheckBox(emailConfigurationPage.sslEnabled());
    dirtyBehaviourCheckBox(emailConfigurationPage.startTlsEnabled());
  }

  @Test
  public void testSendTestEmailDisabledWhenNoConfigurationExists() {
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.testEmailRecipient().setValue("koraytugay@icloud.com");
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED);
  }

  @Test
  public void testEmailRecipientIsEmptyWhenPageIsNavigated() {
    tempEntity.newMailConfiguration("a", insightMail.encryptPassword("password".toCharArray()));
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.testEmailRecipient().setValue("koraytugay@icloud.com");
    refreshOrOpen(DashboardPage.url());
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.testEmailRecipient().shouldBe(empty);
  }

  @Test
  public void testSendTestEmailConfigurationNotSaved_MinimalData() throws MessagingException, IOException {
    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Hostname, Port, System Email and Recipient address are required."));

    emailConfigurationPage.hostName().setValue("localhost");
    emailConfigurationPage.port().setValue("465");
    emailConfigurationPage.systemEmail().setValue("nexus@iq.com");
    emailConfigurationPage.testEmailRecipient().setValue("admin@company.com");
    sendTestEmail();

    // Sending a test email must not persist the mail configuration
    assertThat(mailConfigurationDAO.get()).isNull();

    // Sending a test mail must not modify UI state upon sending the test email
    emailConfigurationPage.hostName().shouldBe(value("localhost"));
    emailConfigurationPage.port().shouldBe(value("465"));
    emailConfigurationPage.systemEmail().shouldBe(value("nexus@iq.com"));

    assertTestConfigurationEmail("localhost", "465", null, null, "nexus@iq.com", false, false, "admin@company.com");
    resetForm();
  }

  @Test
  public void testUnsavedChangesModal() {
    refreshOrOpen(EmailConfigurationPage.url());
    emailConfigurationPage.hostName().setValue("localhost");
    emailConfigurationPage.port().setValue("465");

    testUnsavedChangesModal_Cancel();
    testUnsavedChangesModal_Continue();
  }

  private void testUnsavedChangesModal_Cancel() {
    refreshOrOpen(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();

    DashboardPage.dashboardContainer().shouldNotBe(visible);

    emailConfigurationPage.title().shouldBe(visible).shouldHave(text("Email"));
  }

  private void testUnsavedChangesModal_Continue() {
    refreshOrOpen(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();

    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testSendEmailConfigurationNotSaved_FullData() throws MessagingException, IOException {
    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.hostName().setValue("localhost");
    emailConfigurationPage.port().setValue("465");
    emailConfigurationPage.systemEmail().setValue("nexus@iq.com");
    emailConfigurationPage.username().setValue("u");
    emailConfigurationPage.password().setValue("p");
    emailConfigurationPage.sslEnabled().click();
    emailConfigurationPage.startTlsEnabled().click();
    emailConfigurationPage.testEmailRecipient().setValue("admin@company.com");
    sendTestEmail();

    // Sending a test email must not persist the mail configuration
    assertThat(mailConfigurationDAO.get()).isNull();

    // Sending a test mail must not modify UI state upon sending the test email
    emailConfigurationPage.hostName().shouldBe(value("localhost"));
    emailConfigurationPage.port().shouldBe(value("465"));
    emailConfigurationPage.systemEmail().shouldBe(value("nexus@iq.com"));
    emailConfigurationPage.username().shouldBe(value("u"));
    emailConfigurationPage.password().shouldBe(value("p"));
    emailConfigurationPage.startTlsEnabled().shouldBe(selected);

    assertTestConfigurationEmail("localhost", "465", "u", "p", "nexus@iq.com", true, true, "admin@company.com");
    resetForm();
  }

  @Test
  public void testSendEmailConfigExistsNoUpdateOnUI() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfigurationWithNoAuthentication();

    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    sendTestEmail();

    assertTestConfigurationEmail(mailConfiguration.getHostname(), String.valueOf(mailConfiguration.getPort()),
        null, null, mailConfiguration.getSystemEmail(), mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
  }

  @Test
  public void testSendEmailConfigExistsAddUsernameAndPasswordSwitchTLS() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfigurationWithNoAuthentication();

    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.username().setValue("u");
    emailConfigurationPage.password().setValue("p");
    emailConfigurationPage.startTlsEnabled().click();

    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    sendTestEmail();

    assertTestConfigurationEmail(mailConfiguration.getHostname(), String.valueOf(mailConfiguration.getPort()),
        "u", "p", mailConfiguration.getSystemEmail(), !mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
    resetForm();
  }

  @Test
  public void testSendEmailConfigExistsHostnameUpdateRequiresPassword() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfiguration("u", "p".toCharArray());

    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.hostName().setValue("another-host");
    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Password must be provided when updating Hostname or Port."));

    emailConfigurationPage.password().setValue("not-same-password");
    sendTestEmail();

    assertTestConfigurationEmail("another-host", String.valueOf(mailConfiguration.getPort()), "u",
        "not-same-password", mailConfiguration.getSystemEmail(), mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
    resetForm();
  }

  @Test
  public void testSendEmailConfigExistsPortUpdateRequiresPassword() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfiguration("u", "p".toCharArray());

    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.port().setValue("25");
    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Password must be provided when updating Hostname or Port."));

    emailConfigurationPage.password().setValue("not-same-password");
    sendTestEmail();

    assertTestConfigurationEmail("smtp.hostname.com", "25", "u",
        "not-same-password", mailConfiguration.getSystemEmail(), mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
    resetForm();
  }

  @Test
  public void testSendEmailModifyExistingConfigurationPasswordNotRequired() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfigurationWithNoAuthentication();

    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.sslEnabled().click();
    emailConfigurationPage.startTlsEnabled().click();
    emailConfigurationPage.systemEmail().setValue("modified@system.com");
    emailConfigurationPage.testEmailRecipient().setValue("koray@tugay.biz");
    sendTestEmail();

    assertTestConfigurationEmail(mailConfiguration.getHostname(), String.valueOf(mailConfiguration.getPort()), null,
        null, "modified@system.com", !mailConfiguration.isStartTlsEnabled(), !mailConfiguration.isSslEnabled(),
        "koray@tugay.biz");
    resetForm();
  }

  @Test
  public void testMustNotBeAbleToSendEmailToEmptySpace() {
    tempEntity.newMailConfigurationWithNoAuthentication();

    MailboxTestUtil.clearAll();
    refreshOrOpen(EmailConfigurationPage.url());

    emailConfigurationPage.testEmailRecipient().setValue("  ");
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED);
  }

  @Test
  public void testPageNotAccessible() {
    try {
      User user = tempEntity.newUser("username", "john", "doe", "john@doe");
      refreshOrOpen(DashboardPage.url());
      logout();
      login(user.getUsername(), user.getPassword());
      refreshOrOpen(EmailConfigurationPage.url());
      emailConfigurationPage.hostName().shouldNotBe(visible);
      emailConfigurationPage.loadError()
          .shouldBe(visible)
          .shouldHave(text("An error occurred loading data. It appears you do not have permission to access this " +
              "page. If you believe this to be incorrect please contact your administrator."));
    }
    finally {
      logout();
      refreshOrOpen(EmailConfigurationPage.url());
      loginAsAdmin();
    }
  }

  private void dirtyBehaviourTextInput(SelenideElement reactTextInput) {
    reactTextInput.setValue("koray-was-here");
    assertButtonsAndTooltipMessageAndClickCancel();
    reactTextInput.shouldBe(value(""));
  }

  private void dirtyBehaviourCheckBox(NxCheckbox checkbox) {
    checkbox.click();
    assertButtonsAndTooltipMessageAndClickCancel();
    checkbox.shouldNotBe(checked);
  }

  private void assertButtonsAndTooltipMessageAndClickCancel() {
    emailConfigurationPage.delete().shouldBe(disabled);
    emailConfigurationPage.cancel().shouldBe(enabled);

    emailConfigurationPage.cancel().click();

    emailConfigurationPage.delete().shouldBe(disabled);
    emailConfigurationPage.cancel().shouldBe(disabled);
  }

  private void assertNoMailServerIsConfigured() {
    // All fields must be empty
    emailConfigurationPage.hostName().shouldBe(empty);
    emailConfigurationPage.port().shouldBe(empty);
    emailConfigurationPage.username().shouldBe(empty);
    emailConfigurationPage.password().shouldBe(empty);
    emailConfigurationPage.systemEmail().shouldBe(empty);

    // Booleans by default are false
    emailConfigurationPage.sslEnabled().shouldNotBe(checked);
    emailConfigurationPage.startTlsEnabled().shouldNotBe(checked);

    emailConfigurationPage.delete().shouldBe(disabled);
    emailConfigurationPage.cancel().shouldBe(disabled);

    assertThat(mailConfigurationDAO.get()).isNull();
  }

  private void sendTestEmail() {
    emailConfigurationPage.testEmailSend().shouldNotBe(DISABLED).hover();
    Tooltip.get().shouldNotBe(visible);

    emailConfigurationPage.testEmailSend().click();
    FormMask.seeAndWaitForDismissal();
  }

  private void assertTestConfigurationEmail(
      String hostname,
      String port,
      String username,
      String password,
      String systemEmail,
      boolean startTlsEnabled,
      boolean sslEnabled,
      String recipientAddress) throws MessagingException, IOException
  {
    List<Message> emails = MailboxTestUtil.get(recipientAddress);

    assertThat(emails).hasSize(1);
    Message email = emails.get(0);

    // Assert mail server
    Session session = email.getSession();
    assertThat(session.getProperties()) //
        .containsEntry("mail.smtp.host", hostname)
        .containsEntry("mail.smtp.port", port)
        .containsEntry("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));

    if (sslEnabled) {
      assertThat(session.getProperties())
          .containsEntry("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    }
    else {
      assertThat(session.getProperties())
          .doesNotContainEntry("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    }

    // Assert authentication
    PasswordAuthentication passwordAuthentication = session.requestPasswordAuthentication(null, 0, null, null, null);
    if (username == null) {
      assertThat(passwordAuthentication).isNull();
    }
    else {
      assertThat(passwordAuthentication.getUserName()).isEqualTo(username);
      if (password == null) {
        assertThat(passwordAuthentication.getPassword()).isNull();
      }
      else {
        assertThat(passwordAuthentication.getPassword()).isEqualTo(password);
      }
    }

    // Assert "to" and "from" addresses
    Address[] recipients = email.getRecipients(RecipientType.TO);
    assertThat(recipients).hasSize(1);
    assertThat(recipients[0].toString()).isEqualTo(recipientAddress);
    assertThat(email.getFrom()[0].toString()).isEqualTo("Nexus IQ Server <" + systemEmail + ">");

    // Assert email subject and body
    assertThat(email.getSubject()).isEqualTo("Test Email Configuration");
    String emailBody = IOUtils.toString(email.getInputStream(), StandardCharsets.UTF_8);
    assertThat(emailBody).contains("Success! This is a test mail from");
  }

  private void resetForm() {
    emailConfigurationPage.cancel().shouldBe(enabled).click();
  }
}
