/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useState} from 'react';
import {NxButton, NxFontAwesomeIcon, NxForm, NxFormGroup, NxModal, NxTextInput, nxTextInputStateHelpers, NxToggle}
  from '@sonatype/react-shared-components';
import {availableScopesPropType, componentPropType} from '../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import {faPlus} from '@fortawesome/pro-solid-svg-icons';
import {pathSet} from '../../util/jsUtil';

const { initialState, userInput } = nxTextInputStateHelpers;

export default function CopyrightOverrideForm(props) {

  const {
    component,
    availableScopes,
    saveCopyrightError,
    submitMaskState,

    //actions
    saveCopyrightOverride,
    setDisplayCopyrightOverrideModal
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

  const createScopeOption = value => <option key={value.id} value={value.id}>{value.label} - {value.name}</option>;

  const onCopyrightContentChange = index => content => {
    setCopyrights(pathSet([index, 'content'], userInput(null, content), copyrights));
  };

  const onCopyrightStatusChange = (index, copyright) => setCopyrights(
      pathSet([index, 'status'], flipStatus(copyright.status), copyrights));

  const setComponentCopyrightScope = (event) => setScope(event.target.value);

  const flipStatus = (status) => status === 'enabled' ? 'disabled' : 'enabled';

  // handle click event of the Add button
  const addNewCustomCopyright = () => {
    setCopyrights([
      ...copyrights, {
        id: '',
        content: initialState(''),
        originalContentHash: '',
        status: 'enabled'
      }
    ]);
  };

  const trySave = () => {
    saveCopyrightOverride({
      copyrights: copyrights
          .filter(c => c.id !== 0 && c.content.trimmedValue.length !== 0)
          .map(({ content, ...rest }) => ({
            content: content.trimmedValue,
            ...rest
          })),
      scopeOwnerId: scope
    });
  };

  function isDirty() {
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

  return (
    <NxModal id="edit-copyright-attribution-modal"
             onClose={() => setDisplayCopyrightOverrideModal(false)}
             variant="wide">
      <NxForm onCancel={() => setDisplayCopyrightOverrideModal(false)}
              submitBtnText="Save"
              submitError={saveCopyrightError}
              submitMaskState={submitMaskState}
              onSubmit={trySave}
              validationErrors={isDirty() ? null : 'No modifications'}>
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
          <NxFormGroup id="edit-copyright-scope-selection-group" label="Scope" sublabel="Apply changes to" isRequired>
            <select className="nx-form-select nx-form-select--long"
                    id="edit-copyright-scope-selection"
                    value={scope}
                    onChange={setComponentCopyrightScope}>
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
      component: componentPropType,
      availableScopes: availableScopesPropType,
      saveCopyrightOverride: PropTypes.func,
      saveCopyrightError: PropTypes.string,
      submitMaskState: PropTypes.bool,
      setDisplayCopyrightOverrideModal: PropTypes.func.isRequired
    }
;
