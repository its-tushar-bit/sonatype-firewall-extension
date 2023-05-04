/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  NxToggle,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { availableScopesPropType, legalFilesPropType, licenseObligationPropType } from '../../advancedLegalPropTypes';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';
import ObligationStatusComponent from '../../shared/ObligationStatusComponent';
import { createScopeOption } from '../../legalUtility';

export default function NoticesModal(props) {
  const {
    // actions
    cancelNoticesModal,
    setNoticeContent,
    setNoticeStatus,
    addNotice,
    setNoticesScope,
    saveNotices,
    setObligationStatus,
    setObligationScope,
    // state
    scope,
    originalScope,
    availableScopes,
    notices,
    error,
    submitMaskState,
    existingObligation,
  } = props;

  const createFormRowItem = (notice, index) => (
    <tr id={'notice-row-' + index} key={index}>
      <td>
        <NxTextInput
          id={'notice-text-input-' + index}
          className="nx-text-input nx-text-input--full"
          type="textarea"
          value={notice.content}
          isPristine={notice.isPristine}
          onChange={(payload) => setNoticeContent({ index: index, value: payload })}
          disabled={notice.status === 'disabled'}
        />
      </td>
      <td>
        <NxToggle
          inputId={'notice-status-toggle-' + index}
          onChange={() =>
            setNoticeStatus({
              index: index,
              value: notice.status === 'enabled' ? 'disabled' : 'enabled',
            })
          }
          className="nx-toggle nx-toggle--no-gap"
          isChecked={notice.status === 'enabled'}
        >
          {notice.status === 'enabled' ? 'Included' : 'Excluded'}
        </NxToggle>
      </td>
    </tr>
  );

  const notValidErrorMessage = 'A custom notice must have text.';

  const isValid = () => {
    return !notices.some(
      (notice) => notice.id === null && notice.originalContentHash === null && notice.content === ''
    );
  };

  const notDirtyErrorMessage = 'Must add a new notice or change the content or status of a notice.';

  function isObligationDirty() {
    return existingObligation && existingObligation.status !== existingObligation.originalStatus;
  }

  const noticeFilesExist = () => notices && notices.length > 0;

  const resetExistingObligation = () => {
    if (existingObligation) {
      setObligationStatus({
        name: existingObligation.name,
        value: existingObligation.originalStatus,
      });
      setObligationScope({
        name: existingObligation.name,
        value: existingObligation.originalScope,
      });
    }
  };

  function isNoticesDirty() {
    return (
      scope !== originalScope ||
      notices.some(
        (notice) =>
          (notice.id === null && notice.originalContentHash === null) ||
          notice.content !== notice.originalContent ||
          notice.status !== notice.originalStatus
      )
    );
  }

  const isDirty = () => {
    return isNoticesDirty() || isObligationDirty();
  };

  const getValidationErrors = () => {
    if (!isValid()) {
      return notValidErrorMessage;
    }
    if (!isDirty()) {
      return notDirtyErrorMessage;
    }
    return undefined;
  };

  const getSubmitMaskState = () => {
    const nullIfUndef = (b) => (b === undefined ? null : b);
    const mainSubmitMaskState = nullIfUndef(submitMaskState);
    const obligationSubmitMaskState = existingObligation
      ? nullIfUndef(existingObligation.saveObligationSubmitMask)
      : null;
    if (mainSubmitMaskState === null) {
      return obligationSubmitMaskState;
    }
    if (obligationSubmitMaskState === null) {
      return mainSubmitMaskState;
    }
    return mainSubmitMaskState && obligationSubmitMaskState;
  };

  const setObligationScopeIfNeeded = (event) => {
    if (existingObligation && existingObligation.status !== existingObligation.originalStatus) {
      setObligationScope({
        name: existingObligation.name,
        value: event.target.value,
      });
    }
  };

  const onObligationChange = (value) => {
    setObligationStatus({ name: existingObligation.name, value });
    if (value === existingObligation.originalStatus) {
      setObligationScope({
        name: existingObligation.name,
        value: existingObligation.originalScope,
      });
    } else {
      setObligationScope({ name: existingObligation.name, value: scope });
    }
  };

  const trySave = () => {
    saveNotices({
      existingObligation,
      isNoticesDirty: isNoticesDirty(),
      isObligationDirty: isObligationDirty(),
    });
  };

  return (
    <NxModal
      id="edit-notices-attribution-modal"
      onClose={() => {
        resetExistingObligation();
        cancelNoticesModal();
      }}
      variant="wide"
    >
      <NxStatefulForm
        onCancel={() => {
          resetExistingObligation();
          cancelNoticesModal();
        }}
        submitBtnText="Save"
        onSubmit={trySave}
        submitError={error || existingObligation?.error}
        submitMaskState={getSubmitMaskState()}
        validationErrors={getValidationErrors()}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">{`${noticeFilesExist() ? 'Edit' : 'Add'} Notice Files`}</h2>
        </header>
        <div className="nx-modal-content">
          <table className="legal-file-override-table">
            <thead>
              <tr>
                <th>Notice Text</th>
                <th>Attribution Report Status</th>
              </tr>
            </thead>
            <tbody>
              {notices.length > 0 ? (
                notices.map(createFormRowItem)
              ) : (
                <tr>
                  <td className="no-legal-texts-found">No notice files found</td>
                  <td />
                </tr>
              )}
            </tbody>
          </table>
          <div className="nx-btn-bar nx-btn-bar--left">
            <NxButton id="add-notice" type="button" variant="tertiary" onClick={addNotice}>
              <NxFontAwesomeIcon icon={faPlus} />
              <span>Add Notice Text</span>
            </NxButton>
          </div>
          {existingObligation && (
            <ObligationStatusComponent existingObligation={existingObligation} onChange={onObligationChange} />
          )}
          <NxFormGroup
            clasName="legal-modal-scope-selection-group"
            label="Scope"
            sublabel="Apply changes to"
            isRequired
          >
            <NxFormSelect
              id="edit-notice-scope-selection"
              className="nx-form-select--long"
              value={scope}
              onChange={(payload) => {
                setNoticesScope(payload.currentTarget.value);
                setObligationScopeIfNeeded(payload);
              }}
            >
              {availableScopes.values.map(createScopeOption)}
            </NxFormSelect>
          </NxFormGroup>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

NoticesModal.propTypes = {
  cancelNoticesModal: PropTypes.func.isRequired,
  setNoticeContent: PropTypes.func.isRequired,
  setNoticeStatus: PropTypes.func.isRequired,
  addNotice: PropTypes.func.isRequired,
  setNoticesScope: PropTypes.func.isRequired,
  saveNotices: PropTypes.func.isRequired,
  scope: PropTypes.string.isRequired,
  originalScope: PropTypes.string.isRequired,
  availableScopes: availableScopesPropType,
  notices: legalFilesPropType,
  error: PropTypes.string,
  submitMaskState: PropTypes.bool,
  existingObligation: licenseObligationPropType,
  setObligationStatus: PropTypes.func.isRequired,
  setObligationScope: PropTypes.func.isRequired,
};
