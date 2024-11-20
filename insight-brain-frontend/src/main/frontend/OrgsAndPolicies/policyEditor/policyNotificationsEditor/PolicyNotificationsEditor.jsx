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
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormRow,
  NxFormSelect,
  NxH2,
  NxInfoAlert,
  NxLoadWrapper,
  NxOverflowTooltip,
  NxP,
  NxRadio,
  NxTable,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faPlus, faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { actions as policyActions, RECIPIENT_TYPES } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
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
  selectIsNotificationOverrideEnabled,
  selectOverrideNotificationsFlag,
  selectNotifications,
  selectIsNotificationsTableEnabled,
  selectIsNotificationsInheritOverrideEnabled,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectActionStageTypes, selectSbomStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import {
  selectIsMonitoringSupported,
  selectIsNotificationsSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { validateEmailPatternMatch, hasValidationErrors } from 'MainRoot/util/validationUtil';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsRepositoriesRelated, selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

const isValidEmail = (email) => !hasValidationErrors(validateEmailPatternMatch('Invalid email format', email));

export default function PolicyNotificationsEditor() {
  const dispatch = useDispatch();
  const loadNotificationsEditor = () => dispatch(policyActions.loadNotificationsEditor());
  const addNotificationRecipient = () => dispatch(policyActions.addNotificationRecipient(ownerId));
  const removeNotificationRecipient = (payload) =>
    dispatch(policyActions.removeNotificationRecipient({ ...payload, ownerId }));
  const toggleNotificationRecipientStage = (payload) =>
    dispatch(policyActions.toggleNotificationRecipientStage({ ...payload, ownerId }));
  const setNotificationsEditorFormFieldValue = (field, value) =>
    dispatch(policyActions.setNotificationsEditorFormFieldValue({ field, value, ownerId }));
  const setOverrideParentNotifications = () => dispatch(policyActions.setOverrideParentNotifications());
  const unsetOverrideParentNotifications = (ownerId) =>
    dispatch(policyActions.unSetOverrideParentNotifications(ownerId));
  const setNotificationsOverride = (ownerId, notificationsOverride) =>
    dispatch(policyActions.setNotificationsOverride({ ownerId, notificationsOverride }));

  const ownerId = useSelector(selectSelectedOwnerId);
  const isFirewallSupported = useSelector(selectIsFirewallSupported);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isNotificationsSupported = useSelector(selectIsNotificationsSupported);
  const isNotificationOverrideEnabled = useSelector(selectIsNotificationOverrideEnabled);
  const isOverrideParentNotificationsEnabled = useSelector(selectOverrideNotificationsFlag);
  const notifications = useSelector(selectNotifications);

  const stageTypes = useSelector((state) =>
    selectIsSbomManager(state) ? selectSbomStageTypes(state) : selectActionStageTypes(state)
  );

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
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);

  const recipientType = formState?.recipientType?.value;
  const recipientEmail = formState?.recipientEmail?.value;
  const recipientRoleId = formState?.recipientRoleId?.value;
  const recipientWebhookId = formState?.recipientWebhookId?.value;
  const recipientProjectKey = formState?.recipientProjectKey?.value;
  const recipientIssueTypeId = formState?.recipientIssueTypeId?.value;
  const { userNotifications = [] } = notifications ?? {};
  const tableGridTemplateStyles = {
    gridTemplateColumns: `minmax(90px, 1fr) repeat(${stageTypes?.length}, min-content) minmax(48px, min-content) 60px`,
  };
  const isNotificationsInheritOverrideEnabled = useSelector(selectIsNotificationsInheritOverrideEnabled);
  const isNotificationsTableEnabled = useSelector(selectIsNotificationsTableEnabled);
  const isSbomManager = useSelector(selectIsSbomManager);
  const continuousMonitoringStageId = isSbomManager ? 'sbom-continuous-monitoring' : 'continuous-monitoring';

  const hasStage = (notification, stageId) => (notification.stageIds ?? []).includes(stageId);

  const isNotificationsSupportedForStage = (stageId) =>
    isRepositoriesRelated
      ? stageId === 'proxy' && isNotificationsSupported
      : (isFirewallSupported && stageId === 'proxy') || isNotificationsSupported;

  const isDisabled = (recipient, stageId) => {
    // JIRA/Webhook notifications can't use proxy stage
    const isJiraOrWebhookProxyStage = (recipient?.projectKey || recipient?.webhookId) && stageId === 'proxy';

    return !isNotificationsTableEnabled || isJiraOrWebhookProxyStage || !isNotificationsSupportedForStage(stageId);
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
      !isNotificationsTableEnabled
    );
  };

  const getCheckboxTooltipMessage = (recipient, stage) => {
    if (isRepositoriesRelated && stage.stageTypeId !== 'proxy') {
      return 'Notifications are only supported at Proxy stage';
    }

    if (stage.stageTypeId === 'proxy') {
      if (recipient.webhookId) return 'Webhooks are not available for policy violations at Proxy stage.';
      if (recipient.projectKey) return 'Jira notifications are not available for policy violations at Proxy stage.';
    }

    if (!isNotificationsSupportedForStage(stage.stageTypeId)) return 'Notifications are not supported by your license.';
    else return '';
  };

  const onOverrideParentNotificationsChange = (enableOverrides) => {
    if (enableOverrides) {
      setOverrideParentNotifications();
      setNotificationsOverride(ownerId, notifications ?? {});
    } else {
      unsetOverrideParentNotifications(ownerId);
    }
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

        {isInherited && (
          <div id="edit-policy-notifications-override">
            <NxP>
              {isNotificationOverrideEnabled
                ? 'Notification overrides have been enabled for this policy. Modifying notifications will only affect this level.'
                : 'Notification overrides have been disabled for this policy.'}
            </NxP>

            <NxFieldset id="editor-policy-notification-inherit" label="Override Status" isRequired={true}>
              <NxRadio
                id="edit-policy-notifications-override-inherit"
                name="overrideParentNotificationsStatus"
                value={null}
                disabled={!isNotificationsInheritOverrideEnabled}
                isChecked={!isOverrideParentNotificationsEnabled}
                onChange={() => onOverrideParentNotificationsChange(false)}
              >
                Inherit parent notifications
              </NxRadio>

              <NxRadio
                id="edit-policy-notifications-override-override"
                name="overrideParentNotificationsStatus"
                value="allowOverrideParentNotificationsStatus"
                disabled={!isNotificationsInheritOverrideEnabled}
                isChecked={isOverrideParentNotificationsEnabled}
                onChange={() => onOverrideParentNotificationsChange(true)}
              >
                Override parent notifications
              </NxRadio>
            </NxFieldset>
          </div>
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
              {stageTypes?.map((stage) => (
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
                  {stageTypes?.map((stage) => (
                    <NxTable.Cell key={stage.stageTypeId}>
                      <NxTooltip title={getCheckboxTooltipMessage(recipient, stage)}>
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
                        disabled={
                          isDisabled(recipient, continuousMonitoringStageId) ||
                          !isMonitoringSupported ||
                          isRepositoriesRelated
                        }
                        isChecked={hasStage(recipient, continuousMonitoringStageId)}
                        onChange={() =>
                          toggleNotificationRecipientStage({ recipient, stageId: continuousMonitoringStageId })
                        }
                      />
                    </NxTooltip>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxButtonBar>
                      <NxButton
                        type="button"
                        variant="icon-only"
                        title={!isNotificationsTableEnabled ? '' : 'Remove recipient'}
                        aria-label="Remove recipient"
                        className="iq-notifications-action"
                        disabled={!isNotificationsTableEnabled}
                        onClick={() => {
                          if (isNotificationsTableEnabled) removeNotificationRecipient({ recipient });
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
              disabled={!isNotificationsTableEnabled}
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
                disabled={!isNotificationsTableEnabled}
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
                disabled={!isNotificationsTableEnabled}
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
                disabled={!isNotificationsTableEnabled}
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
                  disabled={!isNotificationsTableEnabled}
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
                  disabled={!isNotificationsTableEnabled || !recipientProjectKey}
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
