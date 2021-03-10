/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  NxToggle
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { availableScopesPropType, legalFilesPropType } from '../../advancedLegalPropTypes';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';

export default function NoticesModal(props) {
  const {
    // actions
    cancelNoticesModal,
    setNoticeContent,
    setNoticeStatus,
    addNotice,
    setNoticesScope,
    saveNotices,
    // state
    scope,
    originalScope,
    availableScopes,
    notices,
    error,
    submitMaskState
  } = props;

  const createFormRowItem = (notice, index) =>
    <tr id={ 'notice-row-' + index } key={ index }>
      <td>
        <NxTextInput id={ 'notice-text-input-' + index }
                     className="nx-text-input nx-text-input--full"
                     type="textarea"
                     value={ notice.content }
                     isPristine={ notice.isPristine }
                     onChange={ payload => setNoticeContent({ index: index, value: payload }) }
                     disabled={ notice.status === 'disabled' }/>
      </td>
      <td>
        <NxToggle inputId={ 'notice-status-toggle-' + index }
                  onChange={ () => setNoticeStatus(
                      { index: index, value: notice.status === 'enabled' ? 'disabled' : 'enabled' }) }
                  className="nx-toggle nx-toggle--no-gap"
                  isChecked={ notice.status === 'enabled' }>
          { notice.status === 'enabled' ? 'Included' : 'Excluded' }
        </NxToggle>
      </td>
    </tr>;

  const createScopeOption = value => {
    return <option key={ value.id } value={ value.id }>{ value.label } - { value.name }</option>;
  };

  const notValidErrorMessage = 'A custom notice must have text.';

  const isValid = () => {
    return !notices.some(notice => notice.id === null && notice.originalContentHash === null && notice.content === '');
  };

  const notDirtyErrorMessage = 'Must add a new notice or change the content or status of a notice.';

  const isDirty = () => {
    return scope !== originalScope ||
        notices.some(notice =>
          (notice.id === null && notice.originalContentHash === null) ||
          (notice.content !== notice.originalContent) ||
          (notice.status !== notice.originalStatus));
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

  return <NxModal id="edit-notices-attribution-modal" onClose={ cancelNoticesModal } variant="wide">
    <NxForm onCancel={ cancelNoticesModal }
            submitBtnText="Save"
            onSubmit={ saveNotices }
            submitError={ error }
            submitMaskState={ submitMaskState }
            validationErrors={ getValidationErrors() }>
      <header className="nx-modal-header">
        <h2 className="nx-h2">
          Edit Notice Texts
        </h2>
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
            { notices.length > 0 ? notices.map(createFormRowItem) :
            <tr><td className="no-legal-texts-found">No notice texts found</td><td/></tr> }
          </tbody>
        </table>
        <div className="nx-btn-bar nx-btn-bar--left">
          <NxButton id="add-notice" type="button" variant="tertiary" onClick={ addNotice }>
            <NxFontAwesomeIcon icon={ faPlus }/>
            <span>Add Notice Text</span>
          </NxButton>
        </div>
        <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
          <select id="edit-notice-scope-selection"
                  className="nx-form-select nx-form-select--long"
                  value={ scope }
                  onChange={ payload => setNoticesScope(payload.currentTarget.value) }>
            { availableScopes.values.map(createScopeOption) }
          </select>
        </NxFormGroup>
      </div>
    </NxForm>
  </NxModal>;
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
  submitMaskState: PropTypes.bool
};
