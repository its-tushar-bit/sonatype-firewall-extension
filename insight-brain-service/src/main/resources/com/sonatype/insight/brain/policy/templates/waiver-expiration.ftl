<#escape x as x?html>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Waiver Expiration Notice</title>
  <style>
    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
    .container { max-width: 640px; margin: 32px auto; background: #ffffff; border: 1px solid #dddddd; border-radius: 4px; }
    .header { background-color: #1b2a4a; padding: 24px 32px; border-radius: 4px 4px 0 0; }
    .header h1 { color: #ffffff; font-size: 20px; margin: 0; }
    .body { padding: 32px; }
    .summary-box { background-color: #fff8e1; border-left: 4px solid #f5a623; padding: 16px 20px; margin-bottom: 24px; border-radius: 2px; }
    .summary-box.critical { background-color: #fdecea; border-left-color: #d32f2f; }
    .summary-box p { margin: 0; font-size: 15px; color: #333333; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
    th { text-align: left; background-color: #f0f0f0; padding: 10px 12px; font-size: 13px; color: #555555; border-bottom: 1px solid #dddddd; }
    td { padding: 10px 12px; font-size: 14px; color: #333333; border-bottom: 1px solid #eeeeee; vertical-align: top; }
    td.label { font-weight: bold; width: 160px; color: #555555; }
    .btn { display: inline-block; margin-top: 24px; padding: 12px 24px; background-color: #1b6ca8; color: #ffffff; text-decoration: none; border-radius: 4px; font-size: 14px; }
    .footer { padding: 16px 32px; border-top: 1px solid #eeeeee; font-size: 12px; color: #999999; }
  </style>
</head>
<body>
<div class="container">

  <div class="header">
    <h1>Nexus IQ Server — Waiver Expiration Notice</h1>
  </div>

  <div class="body">

    <#if status == "EXPIRING_IN_24_HOURS">
    <div class="summary-box critical">
      <p><strong>Action Required:</strong> A policy waiver is expiring in <strong>24 hours</strong>. After expiry, the component may be flagged as a violation again.</p>
    </div>
    <#else>
    <div class="summary-box">
      <p><strong>Heads Up:</strong> A policy waiver is expiring in <strong>${daysUntilExpiry} days</strong>. You may want to review and renew it before it expires.</p>
    </div>
    </#if>

    <table>
      <tr>
        <td class="label">Component</td>
        <td>${componentDisplayName}</td>
      </tr>
      <#if componentPackageUrl?has_content>
      <tr>
        <td class="label">Package URL</td>
        <td>${componentPackageUrl}</td>
      </tr>
      </#if>
      <tr>
        <td class="label">Policy</td>
        <td>
          ${policyName!"Unknown Policy"}
          <#if threatLevel?has_content>
            &nbsp;<span style="color:#d32f2f; font-weight:bold;">(Threat Level: ${threatLevel})</span>
          </#if>
        </td>
      </tr>
      <tr>
        <td class="label">Application</td>
        <td>${applicationName}</td>
      </tr>
      <tr>
        <td class="label">Expiration Date</td>
        <td>${expirationDate}</td>
      </tr>
      <tr>
        <td class="label">Waiver Created By</td>
        <td>${creatorUsername!"Unknown"}</td>
      </tr>
    </table>

    <#if reportUrl?has_content>
    <a href="${reportUrl}" class="btn">View Policy Report</a>
    </#if>

  </div>

  <div class="footer">
    This is an automated notification from Nexus IQ Server. Do not reply to this email.
  </div>

</div>
</body>
</html>
</#escape>
