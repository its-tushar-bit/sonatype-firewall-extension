/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.test;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.HtmlEmail;

/**
 * Mock HtmlEmail that stores sent messages in {@link MailboxTestUtil} instead of actually sending them via SMTP.
 * <p>
 * This replaces the functionality of org.jvnet.mock-javamail which only supports javax.mail.
 */
public class MockHtmlEmail
    extends HtmlEmail
{
  @Override
  public String send() throws EmailException {
    try {
      // Build the message without sending it
      buildMimeMessage();
      MimeMessage message = getMimeMessage();

      // Store in MailboxTestUtil for each recipient
      Address[] recipients = message.getAllRecipients();
      if (recipients != null) {
        for (Address recipient : recipients) {
          MailboxTestUtil.add(recipient.toString(), message);
        }
      }

      return message.getMessageID();
    }
    catch (MessagingException e) {
      throw new EmailException("Failed to build message", e);
    }
  }
}
