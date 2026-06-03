/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxPageTitle,
  NxH1,
  NxH2,
  NxTile,
  NxStatefulForm,
  NxRadio,
  NxFieldset,
  NxFormGroup,
  NxTextInput,
  NxFormSelect,
  NxButton,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxModal,
  NxWarningAlert,
  NxErrorAlert,
  NxP,
  NxTable,
  NxList,
} from '@sonatype/react-shared-components';
import { faPlus, faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerParentId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectLoading,
  selectLoadError,
  selectIsDirty,
  selectInheritConfig,
  selectNotificationDays,
  selectDirectEmails,
  selectRoleIds,
  selectSubmitMaskState,
  selectSubmitError,
  selectAvailableRoles,
  selectServerData,
} from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationSelectors';
import {
  actions,
  RECIPIENT_TYPE_DIRECT,
  RECIPIENT_TYPE_ROLE,
} from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationSlice';
import './_waiverExpirationNotificationEditor.scss';

const MAX_NOTIFICATION_DAYS = 3;

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateEmail(val) {
  if (!val || val.trim() === '') {
    return 'Required';
  }
  if (!EMAIL_REGEX.test(val.trim())) {
    return 'Invalid email address';
  }
  return null;
}

function validateDay(val) {
  const num = Number(val);
  if (val === '' || val === null || val === undefined) {
    return 'Required';
  }
  if (isNaN(num) || !Number.isInteger(num)) {
    return 'Must be a whole number';
  }
  if (num < 1) {
    return 'Reminder must be at least 1 day before expiration.';
  }
  return null;
}

export default function WaiverExpirationNotificationEditor() {
  const dispatch = useDispatch();
  const isOrg = useSelector(selectIsOrganization);
  const parentId = useSelector(selectSelectedOwnerParentId);
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const isDirty = useSelector(selectIsDirty);
  const inheritConfig = useSelector(selectInheritConfig);
  const notificationDays = useSelector(selectNotificationDays);
  const directEmails = useSelector(selectDirectEmails);
  const roleIds = useSelector(selectRoleIds);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const submitError = useSelector(selectSubmitError);
  const availableRoles = useSelector(selectAvailableRoles);

  const serverData = useSelector(selectServerData);

  const isRootOrg = isOrg && !parentId;
  const showCustomForm = !inheritConfig || isRootOrg;
  const hasExistingConfig =
    serverData != null &&
    !serverData.inheritConfig &&
    ((serverData.notificationDays && serverData.notificationDays.length > 0) ||
      (serverData.directEmails && serverData.directEmails.length > 0) ||
      (serverData.roleIds && serverData.roleIds.length > 0));

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  // draftType controls which input to show in the add row — does NOT affect the table
  const [draftType, setDraftType] = useState(RECIPIENT_TYPE_DIRECT);
  const [draftEmail, setDraftEmail] = useState('');
  const [draftRoleId, setDraftRoleId] = useState('');

  const isAddingEmail = draftType === RECIPIENT_TYPE_DIRECT;
  const hasRecipients = directEmails.length > 0 || roleIds.length > 0;

  const doLoad = () => {
    dispatch(actions.loadConfig());
    dispatch(actions.loadRoles());
  };

  useEffect(() => {
    doLoad();
  }, []);

  function handleDayChange(index, val) {
    const updated = [...notificationDays];
    updated[index] = val;
    dispatch(actions.setNotificationDays(updated));
  }

  function handleDayRemove(index) {
    dispatch(actions.setNotificationDays(notificationDays.filter((_, i) => i !== index)));
  }

  function handleEmailRemove(index) {
    dispatch(actions.setDirectEmails(directEmails.filter((_, i) => i !== index)));
  }

  function handleRoleRemove(index) {
    dispatch(actions.setRoleIds(roleIds.filter((_, i) => i !== index)));
  }

  function handleAddEmail() {
    const trimmed = draftEmail.trim();
    if (!trimmed || validateEmail(trimmed)) return;
    dispatch(actions.setDirectEmails([...directEmails, trimmed]));
    setDraftEmail('');
  }

  function handleAddRole() {
    if (!draftRoleId) return;
    dispatch(actions.setRoleIds([...roleIds, draftRoleId]));
    setDraftRoleId('');
  }

  return (
    <div className="iq-waiver-expiration-notification-editor">
      <NxPageTitle>
        <NxH1>Waiver Expiration Notifications</NxH1>
        <NxPageTitle.Description>
          Configure when notifications are sent before waivers expire and who receives them
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile>
          <NxStatefulForm
            submitBtnText="Update"
            submitMaskMessage="Saving…"
            onSubmit={() => isDirty && dispatch(actions.saveConfig())}
            submitMaskState={submitMaskState}
            additionalFooterBtns={
              <>
                {hasExistingConfig && (
                  <NxButton
                    variant="tertiary"
                    type="button"
                    onClick={() => setIsDeleteModalOpen(true)}
                    className="iq-waiver-expiration-notification-editor__delete-btn"
                  >
                    <NxFontAwesomeIcon icon={faTrashAlt} />
                    <span>Delete Config</span>
                  </NxButton>
                )}
                <NxButton variant="secondary" type="button" onClick={() => window.history.back()}>
                  Back
                </NxButton>
              </>
            }
          >
            {isDeleteModalOpen && (
              <NxModal
                id="waiver-expiration-delete-modal"
                variant="narrow"
                aria-labelledby="waiver-expiration-delete-modal-heading"
                onCancel={() => setIsDeleteModalOpen(false)}
              >
                <header className="nx-modal-header">
                  <h2 className="nx-h2" id="waiver-expiration-delete-modal-heading">
                    <NxFontAwesomeIcon icon={faTrashAlt} />
                    <span>Delete Configuration</span>
                  </h2>
                </header>
                <div className="nx-modal-content">
                  <NxWarningAlert>
                    Clicking delete will permanently remove this custom configuration. This owner will then inherit the
                    notification settings from its parent.
                  </NxWarningAlert>
                </div>
                <footer className="nx-footer">
                  <div className="nx-btn-bar">
                    <NxButton type="button" onClick={() => setIsDeleteModalOpen(false)}>
                      Cancel
                    </NxButton>
                    <NxButton variant="primary" type="button" onClick={() => dispatch(actions.deleteConfig())}>
                      Delete
                    </NxButton>
                  </div>
                </footer>
              </NxModal>
            )}

            <NxTile.Content>
              {submitError && <NxErrorAlert>{submitError}</NxErrorAlert>}

              {!isRootOrg && (
                <NxFieldset
                  label="Notification configuration"
                  isRequired
                  className="iq-waiver-expiration-notification-editor__fieldset"
                >
                  <NxRadio
                    name="inheritConfig"
                    value="inherit"
                    isChecked={inheritConfig}
                    onChange={() => dispatch(actions.setInheritConfig(true))}
                  >
                    Use inherited notifications
                  </NxRadio>
                  <NxRadio
                    name="inheritConfig"
                    value="custom"
                    isChecked={!inheritConfig}
                    onChange={() => dispatch(actions.setInheritConfig(false))}
                  >
                    Customize notifications for this level
                  </NxRadio>
                </NxFieldset>
              )}

              {showCustomForm && (
                <>
                  <NxTile.Subsection>
                    <NxH2 className="iq-waiver-expiration-notification-editor__subsection-heading">
                      Expiration reminders
                    </NxH2>
                    <NxP className="iq-waiver-expiration-notification-editor__subsection-description">
                      Choose when reminder notifications should be sent before the waiver expires.
                    </NxP>

                    {notificationDays.length > 0 && (
                      <NxList>
                        {notificationDays.map((day, index) => {
                          const dayError = validateDay(day);
                          return (
                            <NxList.Item
                              key={index}
                              className={[
                                'iq-waiver-expiration-notification-editor__reminder-list-item',
                                index === 0
                                  ? 'iq-waiver-expiration-notification-editor__reminder-list-item--first'
                                  : '',
                                index === notificationDays.length - 1
                                  ? 'iq-waiver-expiration-notification-editor__reminder-list-item--last'
                                  : '',
                              ]
                                .filter(Boolean)
                                .join(' ')}
                            >
                              <div className="iq-waiver-expiration-notification-editor__reminder-row">
                                <span className="iq-waiver-expiration-notification-editor__reminder-label">
                                  Send reminder
                                </span>
                                <div className="iq-waiver-expiration-notification-editor__day-input-wrapper">
                                  <NxTextInput
                                    value={String(day)}
                                    onChange={(val) => handleDayChange(index, val)}
                                    type="number"
                                    id={`notification-day-${index}`}
                                  />
                                </div>
                                <span className="iq-waiver-expiration-notification-editor__reminder-label">
                                  days before expiration
                                </span>
                                <NxButton
                                  variant="icon-only"
                                  title="Remove"
                                  type="button"
                                  onClick={() => handleDayRemove(index)}
                                >
                                  <NxFontAwesomeIcon icon={faTrashAlt} />
                                </NxButton>
                              </div>
                              {dayError && (
                                <div className="iq-waiver-expiration-notification-editor__day-error">{dayError}</div>
                              )}
                            </NxList.Item>
                          );
                        })}
                      </NxList>
                    )}

                    <div className="iq-waiver-expiration-notification-editor__button-section">
                      {notificationDays.length > 0 && (
                        <hr className="iq-waiver-expiration-notification-editor__divider" />
                      )}
                      {notificationDays.length < MAX_NOTIFICATION_DAYS && (
                        <NxButton
                          variant="tertiary"
                          type="button"
                          onClick={() => dispatch(actions.setNotificationDays([...notificationDays, '']))}
                        >
                          <NxFontAwesomeIcon icon={faPlus} />
                          <span>Add Reminder</span>
                        </NxButton>
                      )}
                      <hr className="iq-waiver-expiration-notification-editor__divider" />
                    </div>
                  </NxTile.Subsection>

                  <NxTile.Subsection>
                    <NxH2 className="iq-waiver-expiration-notification-editor__subsection-heading">
                      Notification recipients
                    </NxH2>
                    <NxP className="iq-waiver-expiration-notification-editor__subsection-description">
                      Who should receive waiver expiration reminders?
                    </NxP>

                    <NxTable>
                      <NxTable.Head>
                        <NxTable.Row>
                          <NxTable.Cell>RECIPIENTS</NxTable.Cell>
                          <NxTable.Cell />
                        </NxTable.Row>
                      </NxTable.Head>
                      <NxTable.Body
                        emptyMessage={
                          hasRecipients
                            ? undefined
                            : 'Add people or groups who should receive waiver expiration reminders.'
                        }
                      >
                        {directEmails.map((email, index) => (
                          <NxTable.Row key={`email-${index}`}>
                            <NxTable.Cell>{email}</NxTable.Cell>
                            <NxTable.Cell>
                              <NxButton
                                variant="icon-only"
                                title="Remove"
                                type="button"
                                onClick={() => handleEmailRemove(index)}
                              >
                                <NxFontAwesomeIcon icon={faTrashAlt} />
                              </NxButton>
                            </NxTable.Cell>
                          </NxTable.Row>
                        ))}
                        {roleIds.map((roleId, index) => {
                          const role = availableRoles.find((r) => r.roleId === roleId);
                          return (
                            <NxTable.Row key={`role-${index}`}>
                              <NxTable.Cell>{role ? role.roleName : roleId}</NxTable.Cell>
                              <NxTable.Cell>
                                <NxButton
                                  variant="icon-only"
                                  title="Remove"
                                  type="button"
                                  onClick={() => handleRoleRemove(index)}
                                >
                                  <NxFontAwesomeIcon icon={faTrashAlt} />
                                </NxButton>
                              </NxTable.Cell>
                            </NxTable.Row>
                          );
                        })}
                      </NxTable.Body>
                    </NxTable>

                    <div className="nx-form-row">
                      <NxFormGroup label="Recipient Type" isRequired>
                        <NxFormSelect
                          value={draftType}
                          onChange={(val) => {
                            setDraftType(val);
                            setDraftEmail('');
                            setDraftRoleId('');
                          }}
                          id="recipient-type-select"
                        >
                          <option value={RECIPIENT_TYPE_DIRECT}>Email</option>
                          <option value={RECIPIENT_TYPE_ROLE}>Role</option>
                        </NxFormSelect>
                      </NxFormGroup>

                      {isAddingEmail && (
                        <NxFormGroup label="Email" isRequired>
                          <NxTextInput
                            value={draftEmail}
                            onChange={(val) => setDraftEmail(val)}
                            validatable
                            validationErrors={draftEmail ? validateEmail(draftEmail) : null}
                            id="new-email-input"
                            placeholder="Enter email address"
                          />
                        </NxFormGroup>
                      )}

                      {!isAddingEmail && (
                        <NxFormGroup label="Role" isRequired>
                          <NxFormSelect
                            value={draftRoleId}
                            onChange={(val) => setDraftRoleId(val)}
                            id="new-role-select"
                          >
                            <option value="">-- Select Role --</option>
                            {availableRoles.map((role) => (
                              <option key={role.roleId} value={role.roleId}>
                                {role.roleName}
                              </option>
                            ))}
                          </NxFormSelect>
                        </NxFormGroup>
                      )}

                      <NxButton
                        variant="tertiary"
                        type="button"
                        onClick={() => (isAddingEmail ? handleAddEmail() : handleAddRole())}
                      >
                        <NxFontAwesomeIcon icon={faPlus} />
                        <span>Add</span>
                      </NxButton>
                    </div>
                  </NxTile.Subsection>
                </>
              )}
            </NxTile.Content>
          </NxStatefulForm>
        </NxTile>
      </NxLoadWrapper>
    </div>
  );
}
