<#escape x as x?html>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns:v="urn:schemas-microsoft-com:vml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>CLM Policy Alert</title>
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
			<td colspan="3"><a href="${detailedReportUrl}" title="Sonatype CLM Policy Alert"><img
					src="http://cdn.sonatype.com/clm/policy/1.3/header_bg.gif" width="647" height="68" alt="View Full Report" border="0" /></a></td>
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
												<td width="60" height="30"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-image: url(http://cdn.sonatype.com/clm/policy/1.3/alert_red.gif); background-size: cover; background-color: #ED1C24; background-repeat: no-repeat;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold; -webkit-text-size-adjust: none;">${policyThreatRedCount}</span></td>
												<td width="60" height="30"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-image: url(http://cdn.sonatype.com/clm/policy/1.3/alert_orange.gif); background-size: cover; background-color: #F7931D; background-repeat: no-repeat;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold; -webkit-text-size-adjust: none;">${policyThreatOrangeCount}</span></td>
												<td width="60" height="30"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-image: url(http://cdn.sonatype.com/clm/policy/1.3/alert_yellow.gif); background-size: cover; background-color: #FFDD17; background-repeat: no-repeat;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold; -webkit-text-size-adjust: none;">${policyThreatYellowCount}</span></td>
												<td width="60" height="30"
													style="color: #FFFFFF; width: 60px; height: 30px; text-align: center; background-image: url(http://cdn.sonatype.com/clm/policy/1.3/alert_blue.gif); background-size: cover; background-color: #6D98CF; background-repeat: no-repeat;"><span
													style="font-family: Helvetica, Arial, sans-serif; color: #FFFFFF; font-size: 12px; font-weight: bold; -webkit-text-size-adjust: none;">${policyThreatBlueCount}</span></td>
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
			<td width="12"></td>
			<td width="623" bgcolor="#f7f7f7"
				style="border-left: 1px solid #bcbcbc; border-right: 1px solid #bcbcbc; border-top: 1px solid #dddddd; border-bottom: 1px solid #dddddd; padding: 15px">
				<span style="font-family: Helvetica, Arial, sans-serif; color: #5d5d5d; font-size: 13px">You&#39;re receiving this email because a policy
					has been configured to notify you. See details below.</span>
			</td>
			<td width="12"></td>
		</tr>
		<tr>
			<td width="12">&nbsp;</td>
			<td width="623" bgcolor="#FFFFFF" style="border-left: 1px solid #BCBCBC; border-right: 1px solid #BCBCBC; font-size: 13px;">
				<table border="0" cellpadding="0" cellspacing="0" style="width: 100%; font-size: 13px;">
					<#list policyAlerts as alert> <#list alert.trigger.componentFacts as component>
					<tr>
						<#if (alert.trigger.threatLevel > 7)>
						<td bgcolor="#ED1C24" valign="top" height="20" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 15px;">&nbsp;</td> <#elseif (alert.trigger.threatLevel > 3)>
						<td bgcolor="#F7941D" valign="top" height="20" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 15px;">&nbsp;</td> <#elseif (alert.trigger.threatLevel > 0)>
						<td bgcolor="#FEDF15" valign="top" height="20" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 15px;">&nbsp;</td> <#else>
						<td bgcolor="#6D98CF" valign="top" height="20" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 15px;">&nbsp;</td> </#if>
						<td colspan="2">&nbsp;</td>
					</tr>
					<tr>
						<#if (alert.trigger.threatLevel > 7)>
						<td bgcolor="#ED1C24" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding-left: 15px;">
							<div style="color: #A91113;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> <#elseif (alert.trigger.threatLevel > 3)>
						<td bgcolor="#F7941D" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding-left: 15px;">
							<div style="color: #CB7A16;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> <#elseif (alert.trigger.threatLevel > 0)>
						<td bgcolor="#FEDF15" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding-left: 15px;">
							<div style="color: #D4B718;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> <#else>
						<td bgcolor="#6D98CF" valign="top" height="1" nowrap="nowrap" align="center" width="20"
							style="vertical-align: top; width: 20px; padding: 0; height: 1;">&nbsp;</td>
						<td align="left" valign="top" height="100%" nowrap="nowrap" style="padding-left: 15px;">
							<div style="color: #005399;">
								<b>${alert.trigger.policyName}</b>
							</div>
						</td> </#if>
						<td valign="top" align="left" style="padding-left: 10px;">
							<div style="background-color: #FFFFFF;">
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
			<td colspan="3" width="647" height="54"><a href="${detailedReportUrl}" title="View Full Report"><img
					src="http://cdn.sonatype.com/clm/policy/1.3/footer_bg.gif" width="647" height="75" alt="View Full Report" border="0" /></a></td>
		</tr>
	</table>
</body>
</html>
</#escape>
