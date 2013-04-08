/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.imp.HtmlMailType;

import com.sonatype.insight.portal.mail.EmailUtil;
import com.sonatype.insight.portal.mail.InsightMailer;

@Named
@Singleton
public class InsightMail
    extends AbstractInjectable<InsightMail>
{
    private final InsightMailer insightMailer;

    private final InsightConfig config;

    @Inject
    public InsightMail( final InsightConfig config, final EMailer mailer )
    {
        this.config = config;
        insightMailer = new InsightMailer( mailer, config.getMailConfig() );
    }

    public String getCdnUrl()
    {
        return config.getCdnUrl();
    }

    public void sendHtml( final String mailId, final List<Address> to, final String subject, final String body )
    {
        final MailRequest message = new MailRequest( mailId, HtmlMailType.HTML_TYPE_ID );

        message.setToAddresses( to );
        message.setExpandedSubject( subject );
        message.setExpandedBody( body );

        EmailUtil.waitForMailStatus( insightMailer.sendMail( message ) );
    }
}
