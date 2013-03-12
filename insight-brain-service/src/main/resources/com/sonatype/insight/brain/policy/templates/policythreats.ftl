<#escape x as x?html>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns:v="urn:schemas-microsoft-com:vml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Insight Alert Email</title>
<style type="text/css">
v:* {
	behavior: url(#default#VML);
	display: inline-block;
	overflow: hidden;
}

img {
	display: block;
}

table {
	font-family: Helvetica, Arial, sans-serif;
}

div,p,a,li,td {
	-webkit-text-size-adjust: none;
}
</style>
</head>
<body style="margin: 0; padding: 0;">
	<table width="647" border="0" cellspacing="0" cellpadding="0" style="margin: 0 auto;">
		<tr>
			<td colspan="3"><a href="${serverUrl}rest/report/${applicationPublicId}/${scanId}/embedReport/index.html" title="Sonatype CLM Policy Alert"><img
					src="${serverUrl}policy-assets/img/header_bg.gif" width="647" height="68" alt="View Full Report" border="0" /></a></td>
		</tr>
		<tr>
			<td width="12">&nbsp;</td>
			<td width="623" bgcolor="#FFFFFF" style="border-left: 1px solid #BCBCBC; border-right: 1px solid #BCBCBC; border-bottom: 1px solid #E6E6E6;">
				<table width="100%" border="0" cellspacing="0" cellpadding="0">
					<tr>
						<td style="padding: 15px;">
							<table width="100%" border="0" cellspacing="0" cellpadding="2">
								<tr>
									<td>
										<table cellpadding="0" cellspacing="0" border="0" bordercolor="#FFFFFF" style="border-collapse: collapse;">
											<tr>
												<td width="60" height="30" background="${serverUrl}policy-assets/img/alert_red.gif"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-color: #ED1C24;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold;">${policyThreatRedCount}</span></td>
												<td width="60" height="30" background="${serverUrl}policy-assets/img/alert_orange.gif"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-color: #F7931D;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold;">${policyThreatOrangeCount}</span></td>
												<td width="60" height="30" background="${serverUrl}policy-assets/img/alert_yellow.gif"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-color: #FFDD17;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold;">${policyThreatYellowCount}</span></td>
												<td width="60" height="30" background="${serverUrl}policy-assets/img/alert_blue.gif"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-color: #6D98CF;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold;">${policyThreatBlueCount}</span></td>
											</tr>
											<tr>
												<td style="color: #A91113; font-size: 8px; text-align: center;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #A91113; font-size: 8px; font-weight: bold;">CRITICAL</span></td>
												<td style="color: #CB7A16; font-size: 8px; text-align: center;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #CB7A16; font-size: 8px; font-weight: bold;">SEVERE</span></td>
												<td style="color: #D4B718; font-size: 8px; text-align: center;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #D4B718; font-size: 8px; font-weight: bold;">MODERATE</span></td>
												<td style="color: #5B80AE; font-size: 8px; text-align: center;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #005399; font-size: 8px; font-weight: bold;">NEUTRAL</span></td>
											</tr>
										</table>
									</td>
								</tr>
							</table>
						</td>
						<td style="padding: 15px;">
							<table width="100%" border="0" cellspacing="2" cellpadding="0">
								<tr>
									<td valign="baseline"><span style="font-family: Helvetica, Arial, sans-serif; font-size: 10px; color: #9E9E9E;">APP ID</span></td>
									<td valign="baseline"><span style="font-family: Helvetica, Arial, sans-serif; color: #5D5D5D; font-size: 13px;">${policyThreatApp}</span></td>
								</tr>
								<tr>
									<td valign="baseline"><span style="font-family: Helvetica, Arial, sans-serif; font-size: 10px; color: #9E9E9E;">STAGE</span></td>
									<td valign="baseline"><span style="font-family: Helvetica, Arial, sans-serif; color: #5D5D5D; font-size: 13px;">${policyThreatStage}</span></td>
								</tr>
								<tr>
									<td valign="baseline"><span style="font-family: Helvetica, Arial, sans-serif; font-size: 10px; color: #9E9E9E;">WHEN</span></td>
									<td valign="baseline"><span style="font-family: Helvetica, Arial, sans-serif; color: #5D5D5D; font-size: 13px; line-height: 14px;">${policyThreatTime}</span></td>
								</tr>
							</table>
						</td>
					</tr>
				</table>
			</td>
			<td width="12">&nbsp;</td>
		</tr>
		<tr>
			<td width="12">&nbsp;</td>
			<td width="623" bgcolor="#FFFFFF" style="border-left: 1px solid #BCBCBC; border-right: 1px solid #BCBCBC; font-size: 13px;">
				<table border="0" cellpadding="0" cellspacing="0" style="width: 100%; font-size: 13px; padding-top: 15px;">
					<#list policyAlerts as alert> <#list alert.trigger.componentFacts as component>
					<tr>
						<#if (alert.trigger.threatLevel > 7)>
						<td bgcolor="#ED1C24" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding: 15px;">
							<div style="color: #A91113;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> <#elseif (alert.trigger.threatLevel > 3)>
						<td bgcolor="#F7941D" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding: 15px;">
							<div style="color: #CB7A16;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> <#elseif (alert.trigger.threatLevel > 0)>
						<td bgcolor="#FEDF15" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding: 15px;">
							<div style="color: #D4B718;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> <#else>
						<td bgcolor="#6D98CF" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding: 15px;">
							<div style="color: #005399;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> </#if>
						<td valign="top" align="left" nowrap="nowrap" style="padding: 15px 15px 15px 0;">
							<div style="background-color: #FFFFFF; padding-bottom: 15px;">
								<#if component.groupId??>
								<div style="border-bottom: 1px dotted #E6E6E6;">
									<b>GAV:</b> ${component.groupId} : ${component.artifactId} : ${component.version}
								</div>
								<#else>
								<div style="border-bottom: 1px dotted #E6E6E6;">
									<b>Hash:</b> ${component.hash}
								</div>
								</#if>
								<table border="0" cellpadding="0" style="font-size: 13px;">
									<#list component.constraintFacts as constraint>
									<tr>
										<td align="left" height="100%">
											<div style="padding-top: 7px; color: #555555;">
												<b>${constraint.constraintName}</b>
											</div> <#list constraint.conditionFacts as condition>
											<div style="padding-top: 5px; color: #555555;">${condition.reason}</div> </#list>
										</td>
									</tr>
									</#list>
								</table>
							</div>
						</td>
					</tr>
					</#list> </#list>
				</table>
			</td>
			<td width="12">&nbsp;</td>
		</tr>
		<tr>
			<td colspan="3" width="647" height="54"><a href="${serverUrl}rest/report/${applicationPublicId}/${scanId}/embedReport/index.html"
				title="View Full Report"><img src="${serverUrl}policy-assets/img/footer_bg.gif" width="647" height="75" alt="View Full Report" border="0" /></a></td>
		</tr>
	</table>
</body>
</html>
</#escape>
