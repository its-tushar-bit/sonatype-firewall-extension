/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  nxTextInputStateHelpers,
  NxToggle,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import { availableScopesPropType, componentPropType, licenseObligationPropType } from '../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';
import { pathSet } from '../../util/jsUtil';
import ObligationStatusComponent from '../shared/ObligationStatusComponent';
import { createScopeOption } from '../legalUtility';

const { initialState, userInput } = nxTextInputStateHelpers;

export default function CopyrightOverrideForm(props) {
  const {
    component,
    availableScopes,
    saveCopyrightError,
    submitMaskState,
    existingObligation,

    //actions
    saveCopyrightOverride,
    setDisplayCopyrightOverrideModal,
    setObligationStatus,
    setObligationScope,
  } = props;

  const [copyrights, setCopyrights] = useState(
    component.licenseLegalData.copyrights.map(({ content, ...rest }) => ({
      content: initialState(content),
      ...rest,
    }))
  );

  const defaultScope = () => {
    if (component.licenseLegalData.componentCopyrightScopeOwnerId != null) {
      return component.licenseLegalData.componentCopyrightScopeOwnerId;
    }
    return 'ROOT_ORGANIZATION_ID';
  };

  const [scope, setScope] = useState(defaultScope());

  const createFormRowItem = (copyright, index) => (
    <tr key={index}>
      <td>
        <NxTextInput
          id={'copyright-' + index}
          {...copyright.content}
          maxLength="1000"
          onChange={onCopyrightContentChange(index)}
          className="legal-modal-override-input-content"
          disabled={copyright.status === 'disabled'}
        />
      </td>
      <td>
        <NxToggle
          inputId={'copyright-status-toggle-' + index}
          onChange={() => onCopyrightStatusChange(index, copyright)}
          className="nx-toggle--no-gap copyright-override-status-toggle"
          isChecked={copyright.status === 'enabled'}
        >
          {copyright.status === 'enabled' ? 'Included' : 'Excluded'}
        </NxToggle>
      </td>
    </tr>
  );

  const isCopyrightPresent = () => copyrights && copyrights.length > 0;

  const onCopyrightContentChange = (index) => (content) => {
    setCopyrights(pathSet([index, 'content'], userInput(null, content), copyrights));
  };

  const onCopyrightStatusChange = (index, copyright) =>
    setCopyrights(pathSet([index, 'status'], flipStatus(copyright.status), copyrights));

  const setComponentCopyrightScope = (event) => setScope(event.target.value);

  const setObligationScopeIfNeeded = (event) => {
    if (existingObligation && existingObligation.status !== existingObligation.originalStatus) {
      setObligationScope({
        name: existingObligation.name,
        value: event.target.value,
      });
    }
  };

  const flipStatus = (status) => (status === 'enabled' ? 'disabled' : 'enabled');

  // handle click event of the Add button
  const addNewCustomCopyright = () => {
    setCopyrights([
      ...copyrights,
      {
        id: null,
        content: initialState(''),
        originalContentHash: null,
        status: 'enabled',
      },
    ]);
  };

  const trySave = () => {
    saveCopyrightOverride({
      copyrights: copyrights
        .filter((c) => c.id !== null || c.originalContentHash !== null || c.content.trimmedValue.length !== 0)
        .map(({ content, ...rest }) => ({
          content: content.trimmedValue,
          ...rest,
        })),
      scopeOwnerId: scope,
      existingObligation,
      isCopyrightsDirty: isCopyrightsDirty(),
      isObligationDirty: isObligationDirty(),
    });
  };

  function isCopyrightsDirty() {
    for (let i = component.licenseLegalData.copyrights.length; i < copyrights.length; i++) {
      if (copyrights[i].content.trimmedValue.length !== 0) {
        return true;
      }
    }
    if (defaultScope() !== scope) {
      return true;
    }
    for (let i = 0; i < component.licenseLegalData.copyrights.length; i++) {
      if (
        component.licenseLegalData.copyrights[i].content !== copyrights[i].content.trimmedValue ||
        component.licenseLegalData.copyrights[i].status !== copyrights[i].status
      ) {
        return true;
      }
    }
    return false;
  }

  function isObligationDirty() {
    return existingObligation && existingObligation.status !== existingObligation.originalStatus;
  }

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

  const getSubmitMaskState = () => {
    const nullIfUndef = (b) => (b === undefined ? null : b);
    const copyrightsSubmitMaskState = nullIfUndef(submitMaskState);
    if (nullIfUndef(existingObligation) === null) {
      return copyrightsSubmitMaskState;
    }
    if (copyrightsSubmitMaskState === null) {
      return existingObligation.saveObligationSubmitMask;
    }
    const obligationSubmitMaskState = nullIfUndef(existingObligation.saveObligationSubmitMask);
    if (obligationSubmitMaskState === null) {
      return copyrightsSubmitMaskState;
    }
    return copyrightsSubmitMaskState && obligationSubmitMaskState;
  };

  return (
    <NxModal
      id="edit-copyright-attribution-modal"
      onClose={() => {
        setDisplayCopyrightOverrideModal(false);
        resetExistingObligation();
      }}
      variant="wide"
    >
      <NxStatefulForm
        onCancel={() => {
          setDisplayCopyrightOverrideModal(false);
          resetExistingObligation();
        }}
        submitBtnText="Save"
        submitError={saveCopyrightError || existingObligation?.error}
        submitMaskState={getSubmitMaskState()}
        onSubmit={trySave}
        validationErrors={isCopyrightsDirty() || isObligationDirty() ? null : 'No modifications'}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">{isCopyrightPresent() ? 'Edit Copyright Notices' : 'Add Copyright Notices'}</h2>
        </header>
        <div className="nx-modal-content">
          <table className="legal-file-override-table">
            <thead>
              <tr>
                <th id="edit-copyright-override-copyright-text-title">Copyright Text</th>
                <th id="edit-copyright-override-copyright-status-title">Attribution Report status</th>
              </tr>
            </thead>
            <tbody>{copyrights.map(createFormRowItem)}</tbody>
          </table>

          <div className="nx-form-row">
            <div className="nx-btn-bar">
              <NxButton type="button" id="add-copyright" variant="tertiary" onClick={addNewCustomCopyright}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add Copyright</span>
              </NxButton>
            </div>
          </div>
          {existingObligation && (
            <ObligationStatusComponent existingObligation={existingObligation} onChange={onObligationChange} />
          )}
          <NxFormGroup
            className="legal-modal-scope-selection-group"
            label="Scope"
            sublabel="Apply changes to"
            isRequired
          >
            <NxFormSelect
              className="nx-form-select--long"
              id="edit-copyright-scope-selection"
              value={scope}
              onChange={(event) => {
                setComponentCopyrightScope(event);
                setObligationScopeIfNeeded(event);
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

CopyrightOverrideForm.propTypes = {
  setObligationStatus: PropTypes.func.isRequired,
  setObligationScope: PropTypes.func.isRequired,
  component: componentPropType,
  availableScopes: availableScopesPropType,
  saveCopyrightOverride: PropTypes.func,
  saveCopyrightError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  existingObligation: licenseObligationPropType,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
};
