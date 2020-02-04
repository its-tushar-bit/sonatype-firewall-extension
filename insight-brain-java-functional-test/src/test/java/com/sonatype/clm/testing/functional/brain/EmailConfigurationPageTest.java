/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.EmailConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.service.InsightMail;

import com.codeborne.selenide.SelenideElement;
import org.codehaus.plexus.util.IOUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

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
  private final MailConfigurationDAO mailConfigurationDAO = new MailConfigurationDAO();

  private final EmailConfigurationPage emailConfigurationPage = new EmailConfigurationPage();

  private final InsightMail insightMail = testCLMServer.getCLMServer().getInstance(InsightMail.class);

  private static final String FAKE_PASSWORD = "\u0000\u0000\u0000\u0000\u0000";

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());
    loginAsAdmin();
  }

  @Test
  public void testCrud() {
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    // ## -- CREATE -- ##
    assertNoMailServerIsConfigured();
    eyesWatcher.eyesCheck("Email Configuration Page - Empty State");

    // Save an Email Configuration with minimal data
    emailConfigurationPage.hostName().setValue("smtp.myserver.com");
    emailConfigurationPage.port().setValue("465");
    emailConfigurationPage.systemEmail().setValue("no-reply@iqserver.com");
    assertConfigurationCanBeSaved();
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
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());
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
    assertConfigurationCanBeSaved();
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
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());
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
    eyesWatcher.eyesCheck("Email Configuration Page - Delete Modal");
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

    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    // Test Hostname requires Password
    emailConfigurationPage.hostName().setValue("123");
    assertSaveDisabledWithPasswordRequired();

    emailConfigurationPage.password().setValue("a-new-password");
    assertConfigurationCanBeSaved();

    emailConfigurationPage.password().clear();
    assertConfigurationCanBeSaved();

    // Reset
    emailConfigurationPage.cancel().shouldBe(enabled).click();

    // Password required when port is modified
    emailConfigurationPage.port().setValue("123");
    assertSaveDisabledWithPasswordRequired();

    // Save is enabled when password provided
    emailConfigurationPage.password().setValue("a-new-password");
    assertConfigurationCanBeSaved();

    // Save is enabled with empty password
    emailConfigurationPage.password().clear();
    assertConfigurationCanBeSaved();
  }

  // I must be able to update fields I am allowed to without providing my password
  // And if I do not modify the password, it must not be modified.
  @Test
  public void testUpdatePasswordIsNotModified() {
    MailConfiguration existing
        = tempEntity.newMailConfiguration("admin", insightMail.encryptPassword("password".toCharArray()));

    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    // These are all the values I am allowed to modify without providing my password
    emailConfigurationPage.username().setValue("new-username");
    emailConfigurationPage.systemEmail().setValue("new-system-email");
    emailConfigurationPage.sslEnabled().click();
    emailConfigurationPage.startTlsEnabled().click();

    assertConfigurationCanBeSaved();
    saveConfiguration();

    MailConfiguration updated = mailConfigurationDAO.get();
    // I did not provide a password in the form.
    // My password must NOT be overridden in backend.
    assertThat(insightMail.decryptPassword(updated.getPassword())).isEqualTo("password".toCharArray());

    // I did not modify hostname or port
    assertThat(existing.getHostname()).isEqualTo(updated.getHostname());
    assertThat(existing.getPort()).isEqualTo(updated.getPort());

    // I did update all these
    assertThat(updated.getUsername()).isEqualTo("new-username");
    assertThat(updated.getSystemEmail()).isEqualTo("new-system-email");
    assertThat(updated.isSslEnabled()).isFalse();
    assertThat(updated.isStartTlsEnabled()).isFalse();
  }

  @Test
  public void testUpdatePassword() {
    MailConfiguration existing =
        tempEntity.newMailConfiguration("admin", insightMail.encryptPassword("password".toCharArray()));

    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.password().setValue("new-password");
    assertConfigurationCanBeSaved();
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
    MailConfiguration existing
        = tempEntity.newMailConfiguration("a", insightMail.encryptPassword("password".toCharArray()));

    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.username().click();
    emailConfigurationPage.username().sendKeys(BACK_SPACE);

    // tests that password is selected and cleared with a single back space
    emailConfigurationPage.password().click();
    emailConfigurationPage.password().sendKeys(BACK_SPACE);

    assertConfigurationCanBeSaved();
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

  private void assertSaveDisabledWithRequiredFields() {
    emailConfigurationPage.save().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Hostname, Port and System Email are required details."));
  }

  private void assertSaveDisabledWithPasswordRequired() {
    emailConfigurationPage.save().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Password must be provided when updating Hostname or Port."));
  }

  private void assertConfigurationCanBeSaved() {
    // When save is enabled, tooltip must not be visible
    emailConfigurationPage.save().shouldNotBe(DISABLED).hover();
    Tooltip.get().shouldNotBe(visible);
  }

  private void saveConfiguration() {
    emailConfigurationPage.save().click();
    FormMask.seeAndWaitForDismissal();
  }

  @Test
  public void testDirtyBehaviour() {
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    // Form is said to be dirty if I make any change in any field.
    // Cancel button must be active for a dirty form, and clicking it must bring the form back to its original state
    dirtyBehaviourTextInput(emailConfigurationPage.hostName());
    dirtyBehaviourTextInput(emailConfigurationPage.port());
    dirtyBehaviourTextInput(emailConfigurationPage.username());
    dirtyBehaviourTextInput(emailConfigurationPage.password());
    dirtyBehaviourTextInput(emailConfigurationPage.systemEmail());
    dirtyBehaviourCheckBox(emailConfigurationPage.sslEnabled());
    dirtyBehaviourCheckBox(emailConfigurationPage.startTlsEnabled());
  }

  @Test
  public void testSendTestEmailConfigurationNotSaved_MinimalData() throws MessagingException, IOException {
    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());
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
  }

  @Test
  public void testSendEmailConfigurationNotSaved_FullData() throws MessagingException, IOException {
    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

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
  }

  @Test
  public void testSendEmailConfigExistsNoUpdateOnUI() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfigurationWithNoAuthentication();

    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    sendTestEmail();

    assertTestConfigurationEmail(mailConfiguration.getHostname(), String.valueOf(mailConfiguration.getPort()),
        null, null, mailConfiguration.getSystemEmail(), mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
  }

  @Test
  public void testSendEmailConfigExistsAddUsernameAndPasswordSwitchTLS() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfigurationWithNoAuthentication();

    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.username().setValue("u");
    emailConfigurationPage.password().setValue("p");
    emailConfigurationPage.startTlsEnabled().click();

    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    sendTestEmail();

    assertTestConfigurationEmail(mailConfiguration.getHostname(), String.valueOf(mailConfiguration.getPort()),
        "u", "p", mailConfiguration.getSystemEmail(), !mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
  }

  @Test
  public void testSendEmailConfigExistsHostnameUpdateRequiresPassword() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfiguration("u", "p".toCharArray());

    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.hostName().setValue("another-host");
    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Password must be provided when updating Hostname or Port."));

    emailConfigurationPage.password().setValue("not-same-password");
    sendTestEmail();

    assertTestConfigurationEmail("another-host", String.valueOf(mailConfiguration.getPort()), "u",
        "not-same-password", mailConfiguration.getSystemEmail(), mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
  }

  @Test
  public void testSendEmailConfigExistsPortUpdateRequiresPassword() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfiguration("u", "p".toCharArray());

    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.port().setValue("25");
    emailConfigurationPage.testEmailRecipient().setValue("john@doe");
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Password must be provided when updating Hostname or Port."));

    emailConfigurationPage.password().setValue("not-same-password");
    sendTestEmail();

    assertTestConfigurationEmail("smtp.hostname.com", "25", "u",
        "not-same-password", mailConfiguration.getSystemEmail(), mailConfiguration.isStartTlsEnabled(),
        mailConfiguration.isSslEnabled(), "john@doe");
  }

  @Test
  public void testSendEmailModifyExistingConfigurationPasswordNotRequired() throws IOException, MessagingException {
    MailConfiguration mailConfiguration = tempEntity.newMailConfigurationWithNoAuthentication();

    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.sslEnabled().click();
    emailConfigurationPage.startTlsEnabled().click();
    emailConfigurationPage.systemEmail().setValue("modified@system.com");
    emailConfigurationPage.testEmailRecipient().setValue("koray@tugay.biz");
    sendTestEmail();

    assertTestConfigurationEmail(mailConfiguration.getHostname(), String.valueOf(mailConfiguration.getPort()), null,
        null, "modified@system.com", !mailConfiguration.isStartTlsEnabled(), !mailConfiguration.isSslEnabled(),
        "koray@tugay.biz");
  }

  @Test
  public void testMustNotBeAbleToSendEmailToEmptySpace() {
    tempEntity.newMailConfigurationWithNoAuthentication();

    Mailbox.clearAll();
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());

    emailConfigurationPage.testEmailRecipient().setValue("  ");
    emailConfigurationPage.testEmailSend().shouldBe(DISABLED);
  }

  private void dirtyBehaviourTextInput(SelenideElement element) {
    element.setValue("koray-was-here");
    assertButtonsAndTooltipMessageAndClickCancel();
    element.shouldBe(value(""));
  }

  private void dirtyBehaviourCheckBox(NxCheckbox checkbox) {
    checkbox.click();
    assertButtonsAndTooltipMessageAndClickCancel();
    checkbox.shouldNotBe(checked);
  }

  private void assertButtonsAndTooltipMessageAndClickCancel() {
    assertSaveDisabledWithRequiredFields();
    emailConfigurationPage.delete().shouldBe(disabled);
    emailConfigurationPage.cancel().shouldBe(enabled);

    emailConfigurationPage.cancel().click();

    emailConfigurationPage.save().shouldBe(DISABLED);
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

    emailConfigurationPage.save().shouldBe(DISABLED);
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
    Mailbox emails = Mailbox.get(recipientAddress);

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
    String emailBody = IOUtil.toString(email.getInputStream(), StandardCharsets.UTF_8.name());
    assertThat(emailBody).contains("Success! This is a test mail from");
  }
}
