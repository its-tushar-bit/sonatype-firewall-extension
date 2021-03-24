/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {Fragment, useState} from 'react';
import {
  NxButton,
  NxDropdown,
  NxFontAwesomeIcon,
  NxForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  nxTextInputStateHelpers,
  NxToggle,
  useToggle
} from '@sonatype/react-shared-components';
import {availableScopesPropType, componentPropType, licenseObligationPropType} from '../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import {faCheckCircle, faExclamationTriangle, faMinusCircle, faPlus} from '@fortawesome/pro-solid-svg-icons';
import {pathSet} from '../../util/jsUtil';
import {OBLIGATION_STATUS_TO_DISPLAY, OBLIGATION_STATUSES} from '../advancedLegalConstants';

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
    setObligationScope
  } = props;

  const [copyrights, setCopyrights] = useState(component.licenseLegalData.copyrights.map(({ content, ...rest }) => ({
    content: initialState(content),
    ...rest
  })));

  const defaultScope = () => {
    if (component.licenseLegalData.componentCopyrightScopeOwnerId != null) {
      return component.licenseLegalData.componentCopyrightScopeOwnerId;
    }
    return 'ROOT_ORGANIZATION_ID';
  };

  const [scope, setScope] = useState(defaultScope());

  const createFormRowItem = (copyright, index) =>
    <tr key={index}>
      <td>
        <NxTextInput id={'copyright-' + index}
                     { ...copyright.content }
                     onChange={onCopyrightContentChange(index)}
                     className="copyright-override-input-content"
                     disabled={copyright.status === 'disabled'}/>
      </td>
      <td>
        <NxToggle inputId={'copyright-status-toggle-' + index}
                  onChange={() => onCopyrightStatusChange(index, copyright)}
                  className="nx-toggle--no-gap copyright-override-status-toggle"
                  isChecked={copyright.status === 'enabled'}>
          {copyright.status === 'enabled' ? 'Included' : 'Excluded'}
        </NxToggle>
      </td>
    </tr>;

  const createChangeObligationStatus = () => {
    const createObligationStatusIcon = obligationStatus => {
      switch (obligationStatus) {
        case 'FULFILLED':
          return <NxFontAwesomeIcon icon={ faCheckCircle } className="copyright-obligation-fulfilled-icon" />;
        case 'FLAGGED':
          return <NxFontAwesomeIcon icon={ faExclamationTriangle } className="copyright-obligation-flagged-icon"/>;
        case 'IGNORED':
          return <NxFontAwesomeIcon icon={ faMinusCircle } className="copyright-obligation-ignored-icon"/>;
      }
    };

    const createObligationStatusOption = value => (
      <Fragment>
        { createObligationStatusIcon(value) }
        <span>{ OBLIGATION_STATUS_TO_DISPLAY[value] }</span>
      </Fragment>
    );

    const [isOpen, onToggleCollapse] = useToggle(false),
        labelElement = createObligationStatusOption(existingObligation ? existingObligation.status : 'OPEN');

    const obligationStatusDropdownOptions = () =>
      OBLIGATION_STATUSES
          .filter(value => existingObligation.status !== value)
          .map(value => (<button key={ value + '-dropdown-option' }
                                 type="button"
                                 className="nx-dropdown-button"
                                 onClick={ () => {
                                   setObligationStatus({ name: existingObligation.name, value: value });
                                   if (value === existingObligation.originalStatus) {
                                     setObligationScope(
                                         { name: existingObligation.name, value: existingObligation.originalScope });
                                   }
                                   else {
                                     setObligationScope({ name: existingObligation.name, value: scope });
                                   }
                                   onToggleCollapse();
                                 } }>
            { createObligationStatusOption(value) }
          </button>));

    return <div id="edit-copyright-obligation-status-selection-group">
      <label><span className="nx-label__text">Update Obligation Review Status</span></label>
      <div className="nx-sub-label">
        Change the review status of the obligation &quot;Must Inlcude Copyright&quot; to
      </div>
      <NxDropdown label={labelElement}
                  isOpen={isOpen}
                  onToggleCollapse={onToggleCollapse}
                  id="edit-copyright-obligation-status-selection">
        { obligationStatusDropdownOptions() }
      </NxDropdown>
    </div>;
  };

  const createScopeOption = value => <option key={value.id} value={value.id}>{value.label} - {value.name}</option>;

  const onCopyrightContentChange = index => content => {
    setCopyrights(pathSet([index, 'content'], userInput(null, content), copyrights));
  };

  const onCopyrightStatusChange = (index, copyright) => setCopyrights(
      pathSet([index, 'status'], flipStatus(copyright.status), copyrights));

  const setComponentCopyrightScope = (event) => setScope(event.target.value);

  const setObligationScopeIfNeeded = (event) => {
    if (existingObligation && existingObligation.status !== existingObligation.originalStatus) {
      setObligationScope({ name: existingObligation.name, value: event.target.value });
    }
  };

  const flipStatus = (status) => status === 'enabled' ? 'disabled' : 'enabled';

  // handle click event of the Add button
  const addNewCustomCopyright = () => {
    setCopyrights([
      ...copyrights, {
        id: null,
        content: initialState(''),
        originalContentHash: null,
        status: 'enabled'
      }
    ]);
  };

  const trySave = () => {
    saveCopyrightOverride({
      copyrights: copyrights
          .filter(c => c.id !== null || c.originalContentHash !== null || c.content.trimmedValue.length !== 0)
          .map(({ content, ...rest }) => ({
            content: content.trimmedValue,
            ...rest
          })),
      scopeOwnerId: scope,
      existingObligation,
      isCopyrightsDirty: isCopyrightsDirty(),
      isObligationDirty: isObligationDirty()
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
      if (component.licenseLegalData.copyrights[i].content !== copyrights[i].content.trimmedValue ||
          component.licenseLegalData.copyrights[i].status !== copyrights[i].status) {
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
      setObligationStatus({ name: existingObligation.name, value: existingObligation.originalStatus });
      setObligationScope({ name: existingObligation.name, value: existingObligation.originalScope });
    }
  };

  const getSubmitMaskState = () => {
    const nullIfUndef = (b) => b === undefined ? null : b;
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
    <NxModal id="edit-copyright-attribution-modal"
             onClose={() => {
               setDisplayCopyrightOverrideModal(false);
               resetExistingObligation();
             }}
             variant="wide">
      <NxForm onCancel={() => {
        setDisplayCopyrightOverrideModal(false);
        resetExistingObligation();
      }}
              submitBtnText="Save"
              submitError={saveCopyrightError || (existingObligation ? existingObligation.error : false)}
              submitMaskState={getSubmitMaskState()}
              onSubmit={trySave}
              validationErrors={(isCopyrightsDirty() || isObligationDirty()) ? null : 'No modifications'}>
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            Edit Copyrights
          </h2>
        </header>
        <div className="nx-modal-content">
          <table id="edit-copyrights-override-table">
            <thead>
              <tr>
                <th id="edit-copyright-override-copyright-text-title">Copyright Text</th>
                <th id="edit-copyright-override-copyright-status-title">Attribution Report status</th>
              </tr>
            </thead>
            <tbody>
              {copyrights.map(createFormRowItem)}
            </tbody>
          </table>

          <div className="nx-form-row">
            <div className="nx-btn-bar">
              <NxButton type="button" id="add-copyright" variant="tertiary" onClick={addNewCustomCopyright}>
                <NxFontAwesomeIcon icon={faPlus}/>
                <span>Add Copyright</span>
              </NxButton>
            </div>
          </div>
          { existingObligation && createChangeObligationStatus() }
          <NxFormGroup id="edit-copyright-scope-selection-group" label="Scope" sublabel="Apply changes to" isRequired>
            <select className="nx-form-select nx-form-select--long"
                    id="edit-copyright-scope-selection"
                    value={scope}
                    onChange={(event) => {
                      setComponentCopyrightScope(event);
                      setObligationScopeIfNeeded(event);
                    }}>
              {availableScopes.values.map(createScopeOption)}
            </select>
          </NxFormGroup>
        </div>
      </NxForm>
    </NxModal>
  );
}

CopyrightOverrideForm.propTypes =
    {
      setObligationStatus: PropTypes.func.isRequired,
      setObligationScope: PropTypes.func.isRequired,
      component: componentPropType,
      availableScopes: availableScopesPropType,
      saveCopyrightOverride: PropTypes.func,
      saveCopyrightError: PropTypes.string,
      submitMaskState: PropTypes.bool,
      existingObligation: licenseObligationPropType,
      setDisplayCopyrightOverrideModal: PropTypes.func.isRequired
    }
;
