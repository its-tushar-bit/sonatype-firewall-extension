/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.mail.Message;

import com.sonatype.insight.brain.service.InsightMail;

/**
 * Utility class to provide a Mailbox-like interface compatible with jakarta.mail.Message. This replaces the legacy
 * org.jvnet.mock_javamail.Mailbox which only supports javax.mail.
 * <p>
 * This class automatically configures {@link InsightMail} to use {@link MockHtmlEmail} via a static initializer,
 * so email sends are intercepted and stored here instead of being sent via SMTP.
 */
public final class MailboxTestUtil
{
  private static final Map<String, List<Message>> mailboxes = new ConcurrentHashMap<>();

  // Static initializer to configure InsightMail to use MockHtmlEmail
  static {
    InsightMail.setHtmlEmailFactory(MockHtmlEmail::new);
  }

  private MailboxTestUtil() {
    // Utility class
  }

  /**
   * Get all messages for a given email address.
   *
   * @param emailAddress the email address
   * @return list of messages (never null)
   */
  public static List<Message> get(String emailAddress) {
    return mailboxes.computeIfAbsent(emailAddress, k -> new ArrayList<>());
  }

  /**
   * Add a message to the mailbox for a given email address.
   *
   * @param emailAddress the email address
   * @param message the message to add
   */
  public static void add(String emailAddress, Message message) {
    mailboxes.computeIfAbsent(emailAddress, k -> new ArrayList<>()).add(message);
  }

  /**
   * Clear all messages from a specific mailbox.
   *
   * @param emailAddress the email address
   */
  public static void clear(String emailAddress) {
    List<Message> mailbox = mailboxes.get(emailAddress);
    if (mailbox != null) {
      mailbox.clear();
    }
  }

  /**
   * Clear all mailboxes.
   */
  public static void clearAll() {
    mailboxes.clear();
  }

  /**
   * Get the number of messages in a mailbox.
   *
   * @param emailAddress the email address
   * @return message count
   */
  public static int size(String emailAddress) {
    return get(emailAddress).size();
  }
}
