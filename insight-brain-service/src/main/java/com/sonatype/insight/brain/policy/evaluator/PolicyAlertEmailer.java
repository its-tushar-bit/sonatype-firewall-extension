/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to send (email) notifications for policy alerts.
 *
 * @since 1.8
 */
@Named
public class PolicyAlertEmailer
    extends AbstractPolicyAlertEmailer
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertEmailer.class);

  private final BaseUrl baseUrl;

  private final ApplicationAdapter applicationAdapter;
  
  private final AuditRecorder auditRecorder;

  private final ProductLicense productLicense;

  @Inject
  public PolicyAlertEmailer(final InsightMail mail,
                            final BaseUrl baseUrl,
                            final ApplicationAdapter applicationAdapter,
                            final PolicyAlertEmailResolver policyAlertEmailResolver,
                            final AuditRecorder auditRecorder,
                            final ProductLicense productLicense)
  {
    super(mail, policyAlertEmailResolver);
    this.baseUrl = baseUrl;
    this.applicationAdapter = applicationAdapter;
    this.auditRecorder = auditRecorder;
    this.productLicense = productLicense;
  }

  public void sendNotifications(final Application app,
                                final String scanId,
                                final Stage stage,
                                final List<PolicyNotification> policyNotifications,
                                final int grandfatheredPolicyViolationCount)
  {
    if (!productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)) {
      log.debug("Not sending notifications for application {} and scan {} in stage {}" +
          ", license does not support notifications", app.getPublicId(), scanId, stage.getStageTypeId());
      return;
    }
    new Thread("PolicyAlertEmailNotifierForScan-" + scanId)
    {
      @Override
      public void run() {
        String applicationPublicId = app.getPublicId();
        String mailServer = getMail().getServer();
        Map<String, List<PolicyFact>> policyFactsByEmailAddress = getPolicyFactsByEmailAddress(app,
            policyNotifications);

        if (policyFactsByEmailAddress.isEmpty()) {
          log.debug("Not sending notification emails for application {} and scan {} in stage {}."
              + " There are either no recipients configured, or no new policy violations"
              + " for policies configured to send notifications.", applicationPublicId, scanId, stage);
        }
        for (final Entry<String, List<PolicyFact>> details : policyFactsByEmailAddress.entrySet()) {
          try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.SEND_MAIL)) {
            try {
              log.debug("Sending notification email via {} to {} for application {} and scan {} in stage {}",
                  mailServer, details.getKey(), applicationPublicId, scanId, stage);
              AuditData.get().setApplication(app).setScanId(scanId).setStageId(stage.getStageTypeId())
                  .setData("emailAddress", details.getKey());
              PolicyAlertCounts policyAlertCounts = new PolicyAlertCounts(details.getValue());
              AuditData.get().setData("totalPolicyViolationCount", policyAlertCounts.getTotal());
              StageType stageType = StageTypes.getById(stage.getStageTypeId());
              final String subject = createPolicyMailSubject(policyAlertCounts, app.getName(), stageType);
              final String body = createPolicyMailBody(
                  createPolicyMailModel(app, scanId, stageType, details.getValue(), grandfatheredPolicyViolationCount));
              getMail().sendHtml(details.getKey(), subject, body);
            }
            catch (final Exception e) {
              log.error("Unable to send notification email to {} for application {} and scan {} in stage {}",
                  details.getKey(), applicationPublicId, scanId, stage, e);
              AuditData.get().setException(e);
            }
          }
        }
      }
    }.start();
  }

  protected Map<String, Object> createPolicyMailModel(Application app,
                                                      String scanId,
                                                      StageType stageType,
                                                      List<PolicyFact> policyFacts,
                                                      int grandfatheredPolicyViolationCount)
  {
    Map<String, Object> model = createPolicyMailModel(getMail().getCdnUrl(), app, stageType, policyFacts);
    ContactDTO contact = applicationAdapter.getContact(app.getContactInternalName());
    if (contact != null) {
      model.put("applicationContactEmail", contact.getEmail());
      model.put("applicationContactName", contact.getDisplayName());
    }
    model.put("detailedReportUrl",
        baseUrl.getConfigured() + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), scanId));
    model.put("ownerIdLabel", "APP ID");
    model.put("grandfatheredPolicyViolationCount", grandfatheredPolicyViolationCount);

    return model;
  }
}
