<div style="margin: 0pt auto; padding: 0; width: 650px;">
<table width="100%" cellpadding="0" cellspacing="0" border="0" style="font-family: Arial, Helvetica, sans-serif; font-size: 14px; line-height: 20px; color:#666666;">
<tbody>
  <tr>
    <td colspan="2" style="border-bottom:1px solid #eaeaea;">
      <img src="${detailedReportUrl}public/sonatype.png" alt="Sonatype"/>
    </td>
  </tr>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr>
    <td colspan="2" style='font-weight:bold'>TODO:</td>
  </tr>
<#list policyAlerts as alert>
  <tr>
    <td colspan="2">${alert.trigger}</td>
  </tr>
</#list>
  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr>
    <td style="padding-right: 25px; vertical-align: top;">
      <!-- badge details? -->
    </td>
    <td style="text-align:right;vertical-align:bottom;padding-bottom:5px">
      <a href="${detailedReportUrl}" style="border: 0;">See Your Report</a>
    </td>
  </tr>
  <tr>
    <td colspan="2" style="border-top:1px solid #eaeaea;">&nbsp;</td>
  </tr>
  <tr>
    <td colspan="2">
      <p style="font-size: 11px;line-height:14px;font-style: italic; color: #999999;">
        This report was sent from a notification-only email address that does not accept incoming email. Please do not reply to this message.
      </p>
    </td>
  </tr>
</tbody>
</table>
</div>
