/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.MailComposer;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailSender;
import org.sonatype.micromailer.MailStorage;
import org.sonatype.micromailer.MailType;
import org.sonatype.micromailer.MailTypeSource;
import org.sonatype.micromailer.imp.DefaultEMailer;
import org.sonatype.micromailer.imp.DefaultMailComposer;
import org.sonatype.micromailer.imp.DefaultMailSender;
import org.sonatype.micromailer.imp.DefaultMailStorage;
import org.sonatype.micromailer.imp.DefaultMailType;
import org.sonatype.micromailer.imp.DefaultMailTypeSource;
import org.sonatype.micromailer.imp.HtmlMailType;
import org.sonatype.sisu.velocity.Velocity;
import org.sonatype.sisu.velocity.internal.VelocityConfigurator;
import org.sonatype.sisu.velocity.internal.VelocityImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.sonatype.insight.portal.mail.EmailUtil;
import com.sonatype.insight.portal.mail.InsightMailType;
import com.sonatype.insight.portal.mail.InsightMailer;

public class InsightMail
    extends AbstractInjectable<InsightMail>
{
    private final InsightMailer insightMailer;

    public InsightMail( final InsightConfig config )
    {
        insightMailer = new InsightMailer( buildEmailer(), config.getMailConfig() );
    }

    public void sendHtml( final String mailId, final List<Address> to, final String subject, final String body )
    {
        final MailRequest message = new MailRequest( mailId, HtmlMailType.HTML_TYPE_ID );

        message.setToAddresses( to );
        message.setExpandedSubject( subject );
        message.setExpandedBody( body );

        EmailUtil.waitForMailStatus( insightMailer.sendMail( message ) );
    }

    private static EMailer buildEmailer()
    {
        return Guice.createInjector( new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bindComponent( EMailer.class, DefaultEMailer.class );
                bindComponent( MailComposer.class, DefaultMailComposer.class );
                bindComponent( MailSender.class, DefaultMailSender.class );
                bindComponent( MailStorage.class, DefaultMailStorage.class );
                bindComponent( MailTypeSource.class, DefaultMailTypeSource.class );
                bindComponent( Velocity.class, VelocityImpl.class );
            }

            private <A, I extends A> void bindComponent( final Class<A> api, Class<I> impl )
            {
                bind( api ).to( impl ).in( Scopes.SINGLETON );
            }

            @Provides
            private Map<String, MailType> mailTypes()
            {
                final Map<String, MailType> mailTypes = new HashMap<String, MailType>();
                mailTypes.put( DefaultMailType.DEFAULT_TYPE_ID, new DefaultMailType() );
                mailTypes.put( HtmlMailType.HTML_TYPE_ID, new HtmlMailType() );
                mailTypes.put( InsightMailType.ID, new InsightMailType() );
                return mailTypes;
            }

            @Provides
            private List<VelocityConfigurator> velocityConfigurators()
            {
                return Collections.emptyList(); // we have no custom configurators
            }
        } ).getInstance( EMailer.class );
    }
}
