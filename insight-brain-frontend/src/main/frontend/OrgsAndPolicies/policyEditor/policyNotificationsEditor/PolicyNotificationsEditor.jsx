/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxButtonBar,
  NxCheckbox,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormRow,
  NxFormSelect,
  NxH2,
  NxInfoAlert,
  NxLoadWrapper,
  NxOverflowTooltip,
  NxTable,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faPlus, faTrashAlt } from '@fortawesome/pro-solid-svg-icons';

import { actions as policyActions, RECIPIENT_TYPES } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectCurrentPolicy,
  selectIsInherited,
  selectApplicableWebhooks,
  selectNotificationsEditorLoadError,
  selectNotificationRecipients,
  selectNotificationsEditorFormState,
  selectAvailableRoles,
  selectNotificationRecipientTypeOptions,
  selectAvailableJiraProjects,
  selectSelectedJiraProject,
  selectNotificationsEditorLoading,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectActionStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import {
  selectIsMonitoringSupported,
  selectIsNotificationsSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { validateEmailPatternMatch, hasValidationErrors } from 'MainRoot/util/validationUtil';

const isValidEmail = (email) => !hasValidationErrors(validateEmailPatternMatch('Invalid email format', email));

export default function PolicyNotificationsEditor() {
  const dispatch = useDispatch();
  const loadNotificationsEditor = () => dispatch(policyActions.loadNotificationsEditor());
  const addNotificationRecipient = () => dispatch(policyActions.addNotificationRecipient());
  const removeNotificationRecipient = (payload) => dispatch(policyActions.removeNotificationRecipient(payload));
  const toggleNotificationRecipientStage = (payload) =>
    dispatch(policyActions.toggleNotificationRecipientStage(payload));
  const setNotificationsEditorFormFieldValue = (field, value) =>
    dispatch(policyActions.setNotificationsEditorFormFieldValue({ field, value }));

  const isFirewallSupported = useSelector(selectIsFirewallSupported);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isNotificationsSupported = useSelector(selectIsNotificationsSupported);
  const actionStages = useSelector(selectActionStageTypes);
  const currentPolicy = useSelector(selectCurrentPolicy);
  const isInherited = useSelector(selectIsInherited);
  const applicableNotificationWebhooks = useSelector(selectApplicableWebhooks);
  const loading = useSelector(selectNotificationsEditorLoading);
  const loadError = useSelector(selectNotificationsEditorLoadError);
  const recipients = useSelector(selectNotificationRecipients);
  const formState = useSelector(selectNotificationsEditorFormState);
  const roleOptions = useSelector(selectAvailableRoles);
  const recipientTypeOptions = useSelector(selectNotificationRecipientTypeOptions);
  const availableJiraProjects = useSelector(selectAvailableJiraProjects);
  const selectedJiraProject = useSelector(selectSelectedJiraProject);

  const recipientType = formState?.recipientType?.value;
  const recipientEmail = formState?.recipientEmail?.value;
  const recipientRoleId = formState?.recipientRoleId?.value;
  const recipientWebhookId = formState?.recipientWebhookId?.value;
  const recipientProjectKey = formState?.recipientProjectKey?.value;
  const recipientIssueTypeId = formState?.recipientIssueTypeId?.value;
  const isNotificationsFormDisabled = isInherited || !(isNotificationsSupported || isFirewallSupported);
  const { userNotifications = [] } = currentPolicy?.notifications ?? {};
  const tableGridTemplateStyles = {
    gridTemplateColumns: `minmax(90px, 1fr) repeat(${actionStages?.length}, min-content) minmax(48px, min-content) 60px`,
  };

  const hasStage = (notification, stageId) => (notification.stageIds ?? []).includes(stageId);

  const isNotificationsSupportedForStage = (stageId) =>
    (isFirewallSupported && stageId === 'proxy') || isNotificationsSupported;

  const isDisabled = (recipient, stageId) => {
    const isStageApplicable = !recipient.projectKey || stageId !== 'proxy';

    return (
      isInherited ||
      !isStageApplicable ||
      !isNotificationsSupportedForStage(stageId) ||
      (recipient?.webhookId && stageId === 'proxy')
    );
  };

  const emailExists = (emailAddress) => {
    return userNotifications?.some((item) => item.emailAddress === emailAddress);
  };

  const isAddButtonDisabled = () => {
    return (
      (recipientType === RECIPIENT_TYPES.ROLE && !recipientRoleId) ||
      (recipientType === RECIPIENT_TYPES.WEBHOOK && !recipientWebhookId) ||
      (recipientType === RECIPIENT_TYPES.JIRA && (!recipientProjectKey || !recipientIssueTypeId)) ||
      (recipientType === RECIPIENT_TYPES.EMAIL &&
        (!recipientEmail || !isValidEmail(recipientEmail) || emailExists(recipientEmail))) ||
      isNotificationsFormDisabled
    );
  };

  useEffect(() => {
    loadNotificationsEditor();
  }, []);

  return (
    <div>
      <NxH2>Notifications</NxH2>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadNotificationsEditor}>
        {!isNotificationsSupported && (
          <NxInfoAlert id="notifications-disabled-message">
            {isFirewallSupported
              ? 'Only Proxy Notifications are supported with your Firewall product license.'
              : 'Notifications are not supported by your product license.'}
          </NxInfoAlert>
        )}

        <NxTable
          id="policy-edit-notifications"
          className="iq-policy-editor-table"
          aria-label="Edit policy notifications table"
          style={tableGridTemplateStyles}
        >
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell />
              {actionStages?.map((stage) => (
                <NxTable.Cell key={stage.stageTypeId} className={stage.stageTypeId}>
                  {stage.shortName}
                </NxTable.Cell>
              ))}
              <NxTable.Cell className="continuous-monitoring">
                <NxOverflowTooltip>
                  <div className="nx-truncate-ellipsis">CONTINUOUS MONITORING</div>
                </NxOverflowTooltip>
              </NxTable.Cell>
              <NxTable.Cell />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body emptyMessage="No notifications configured">
            {recipients &&
              recipients.map((recipient) => (
                <NxTable.Row key={recipient.displayName} data-recipient={recipient.displayName}>
                  <NxTable.Cell>
                    <NxOverflowTooltip>
                      <div className="nx-truncate-ellipsis">{recipient.displayName}</div>
                    </NxOverflowTooltip>
                  </NxTable.Cell>
                  {actionStages?.map((stage) => (
                    <NxTable.Cell key={stage.stageTypeId}>
                      <NxTooltip
                        title={
                          recipient.webhookId && stage.stageTypeId === 'proxy'
                            ? 'Webhooks are not available for policy violations at Proxy stage.'
                            : !isNotificationsSupportedForStage(stage.stageTypeId)
                            ? 'Notifications are not supported by your license.'
                            : ''
                        }
                      >
                        <NxCheckbox
                          aria-label={`notify ${recipient.displayName} for ${stage.stageTypeId}`}
                          isChecked={hasStage(recipient, stage.stageTypeId)}
                          disabled={isDisabled(recipient, stage.stageTypeId)}
                          onChange={() => toggleNotificationRecipientStage({ recipient, stageId: stage.stageTypeId })}
                        />
                      </NxTooltip>
                    </NxTable.Cell>
                  ))}
                  <NxTable.Cell className="tm-continuous-monitoring">
                    <NxTooltip
                      title={isMonitoringSupported ? '' : 'Policy Monitoring is not supported by your license'}
                    >
                      <NxCheckbox
                        aria-label={`notify ${recipient.displayName} for continuous-monitoring`}
                        disabled={isInherited || !isMonitoringSupported}
                        isChecked={hasStage(recipient, 'continuous-monitoring')}
                        onChange={() =>
                          toggleNotificationRecipientStage({ recipient, stageId: 'continuous-monitoring' })
                        }
                      />
                    </NxTooltip>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxButtonBar>
                      <NxButton
                        type="button"
                        variant="icon-only"
                        title={isNotificationsFormDisabled ? '' : 'Remove recipient'}
                        aria-label="Remove recipient"
                        className="iq-notifications-action"
                        disabled={isNotificationsFormDisabled}
                        onClick={() => {
                          if (!isNotificationsFormDisabled) removeNotificationRecipient({ recipient });
                        }}
                      >
                        <NxFontAwesomeIcon icon={faTrashAlt} />
                      </NxButton>
                    </NxButtonBar>
                  </NxTable.Cell>
                </NxTable.Row>
              ))}
          </NxTable.Body>
        </NxTable>

        <NxFormRow id="iq-policy-editor__add-notification">
          <NxFormGroup label="Recipient Type" isRequired>
            <NxFormSelect
              id="recipient-type"
              disabled={isNotificationsFormDisabled}
              value={recipientType}
              onChange={(event) => setNotificationsEditorFormFieldValue('recipientType', event.currentTarget.value)}
            >
              {recipientTypeOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </NxFormSelect>
          </NxFormGroup>
          {recipientType === RECIPIENT_TYPES.EMAIL && (
            <NxFormGroup label="Email" isRequired>
              <NxTextInput
                id="recipient-email"
                validatable
                disabled={isNotificationsFormDisabled}
                {...formState?.recipientEmail}
                onChange={(value) => setNotificationsEditorFormFieldValue('recipientEmail', value)}
                onKeyDown={(evt) => {
                  if (evt.key === 'Enter') {
                    evt.preventDefault();
                    if (!isAddButtonDisabled()) {
                      addNotificationRecipient();
                    }
                  }
                }}
              />
            </NxFormGroup>
          )}
          {recipientType === RECIPIENT_TYPES.ROLE && (
            <NxFormGroup label="Role" isRequired>
              <NxFormSelect
                id="recipient-role"
                disabled={isNotificationsFormDisabled}
                value={recipientRoleId}
                onChange={(event) => setNotificationsEditorFormFieldValue('recipientRoleId', event.currentTarget.value)}
              >
                {isNilOrEmpty(roleOptions) ? (
                  <option value="">All roles are being notified.</option>
                ) : (
                  <option value="">-- Select Role --</option>
                )}
                {roleOptions?.map((role) => (
                  <option key={role.roleId} value={role.roleId}>
                    {role.roleName}
                  </option>
                ))}
              </NxFormSelect>
            </NxFormGroup>
          )}
          {recipientType === RECIPIENT_TYPES.WEBHOOK && (
            <NxFormGroup label="Select Webhook" isRequired>
              <NxFormSelect
                id="recipient-webhook"
                disabled={isNotificationsFormDisabled}
                value={recipientWebhookId}
                onChange={(event) =>
                  setNotificationsEditorFormFieldValue('recipientWebhookId', event.currentTarget.value)
                }
              >
                {isNilOrEmpty(applicableNotificationWebhooks) ? (
                  <option value="">No applicable webhooks.</option>
                ) : (
                  <option value="">-- Select Webhook --</option>
                )}
                {applicableNotificationWebhooks?.map((webhook) => (
                  <option key={webhook.id} value={webhook.id}>
                    {webhook.displayName || webhook.url}
                  </option>
                ))}
              </NxFormSelect>
            </NxFormGroup>
          )}
          {recipientType === RECIPIENT_TYPES.JIRA && (
            <>
              <NxFormGroup label="Project" isRequired>
                <NxFormSelect
                  id="recipient-jira-project"
                  disabled={isNotificationsFormDisabled}
                  value={formState?.recipientProjectKey?.value}
                  onChange={(event) =>
                    setNotificationsEditorFormFieldValue('recipientProjectKey', event.currentTarget.value)
                  }
                >
                  {isNilOrEmpty(availableJiraProjects) ? (
                    <option value="">No applicable projects available.</option>
                  ) : (
                    <option value="">-- Select Project --</option>
                  )}
                  {availableJiraProjects?.map((project) => (
                    <option key={project.key} value={project.key}>
                      {project.name}
                    </option>
                  ))}
                </NxFormSelect>
              </NxFormGroup>
              <NxFormGroup label="Issue Type" isRequired>
                <NxFormSelect
                  id="recipient-jira-issue-type"
                  disabled={isNotificationsFormDisabled || !recipientProjectKey}
                  value={formState?.recipientIssueTypeId?.value}
                  onChange={(event) =>
                    setNotificationsEditorFormFieldValue('recipientIssueTypeId', event.currentTarget.value)
                  }
                >
                  {recipientProjectKey ? (
                    isNilOrEmpty(selectedJiraProject?.issueTypes) ? (
                      <option value="">No applicable issue type.</option>
                    ) : (
                      <option value="">-- Select Issue Type --</option>
                    )
                  ) : (
                    <option value="">-- Select JIRA Project --</option>
                  )}
                  {selectedJiraProject?.issueTypes?.map((issueType) => (
                    <option key={issueType.id} value={issueType.id}>
                      {issueType.name}
                    </option>
                  ))}
                </NxFormSelect>
              </NxFormGroup>
            </>
          )}
          <NxButtonBar>
            <NxButton
              id="editor-notification-add"
              type="button"
              variant="tertiary"
              disabled={isAddButtonDisabled()}
              onClick={addNotificationRecipient}
            >
              <NxFontAwesomeIcon icon={faPlus} />
              <span>Add</span>
            </NxButton>
          </NxButtonBar>
        </NxFormRow>
      </NxLoadWrapper>
    </div>
  );
}
