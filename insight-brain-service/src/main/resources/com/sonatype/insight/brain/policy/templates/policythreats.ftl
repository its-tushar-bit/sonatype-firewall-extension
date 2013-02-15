<div style="margin: 0pt auto;border:1px solid black;width:600px;padding:1px;font-size:14px;font-family:Arial, Helvetica, sans-serif">
	<table style="width:100%;border-left:2px solid black;border-right:2px solid black;border-top:2px solid black;border-bottom:2px solid black;vertical-align:middle;font-size:14px;">
		<tr>
			<td><img src="../assets/img/sonatype-32.png" style="vertical-align:middle;"/><span style="vertical-align:middle;padding-left:3px;">Sonatype CLM</span></td>
			<td style="text-align:right;">Policy Violation Alerts</td>
		</tr>
	</table>
	<table style="width:100%;border-left:2px solid black;border-right:2px solid black;font-size:14px;" >
		<tr>
			<td>Stage:</td>
			<td>${policyThreatStage}</td>
			<td>My App:</td>
			<td>${policyThreatApp}</td>
		</tr>
		<tr>
			<td>When:</td>
			<td>${policyThreatTime}</td>
		</tr>
	</table>
	<table style="width:100%;border:2px dashed black;font-size:14px;">
		<tr>
			<td style="white-space: nowrap;width:5%;">New Alerts:</td>
			<td>
				<table>
					<tr>
						<td style="width:30px;height:30px;text-align:center;background-color:#EE1B24;">${policyThreatRedCount}</td>
						<td style="width:30px;height:30px;text-align:center;background-color:#F7941D;">${policyThreatOrangeCount}</td>
						<td style="width:30px;height:30px;text-align:center;background-color:#FEDF15;">${policyThreatYellowCount}</td>
						<td style="width:30px;height:30px;text-align:center;background-color:#6E99D0;">${policyThreatBlueCount}</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
	<table style="width: 100%;border-left:2px solid black;border-right:2px solid black;border-collapse: collapse;font-size:14px;">
		<#list policyAlerts as alert>
			<#list alert.trigger.componentFacts as component>
			<tr>
				<td style="height:1px;width:4%;border-bottom:2px solid black;padding:4 0 4 4;">
					<#if (alert.trigger.threatLevel > 7)>
						<div style="height:100%;background-color:#EE1B24;"></div>
					<#elseif (alert.trigger.threatLevel > 3)>
						<div style="height:100%;background-color:#F7941D;"></div>
					<#elseif (alert.trigger.threatLevel > 0)>
						<div style="height:100%;background-color:#FEDF15;"></div>
					<#else>
						<div style="height:100%;background-color:#6E99D0;"></div>
					</#if>
				</td>
				<td style="height:1px;width:31%;vertical-align:top;border-bottom:2px solid black;padding:4 4 4 0;">	
					<div style="height:100%;background-color:#E6E6E6;">
						<div style="padding-left:4px;"><b>${alert.trigger.policyName}</b></div>
						<div style="padding-left:40px;padding-top:5px;color:#6E99D0;">
							<#list alert.actions as action>
								<#list actionTypes as actionType>
									<#if actionType.id == action.actionTypeId>
										<div><i><b>${actionType.summary}</b></i></div>
									</#if>
								</#list>
							</#list>
						</div>
					</div>
				</td>
				<td style="height:1px;width:65%;border-bottom:2px solid black;padding:4 4 4 0;">
					<div style="height:100%;background-color:#E6E6E6;">
						<div style="border-bottom:1px solid black;"><b>GAV:</b> ${component.groupId} : ${component.artifactId} : ${component.version}</div>
						<table style="font-size:14px;">
							<#list component.constraintFacts as constraint>
								<tr>
									<td style="vertical-align:top;">${constraint.constraintName}</td>
									<td style="padding-left:20px;">
										<#list constraint.conditionFacts as condition>
											<div>${condition.summary}</div>
											<div style="color:#6E99D0;">${condition.reason}</div>
										</#list>
									</td>
								</tr>
							</#list>
						</table>
					</div>
				</td>
			</tr>
			</#list>
		</#list>
	</table>
</div>
