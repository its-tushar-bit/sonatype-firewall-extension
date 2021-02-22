/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  nxTextInputStateHelpers
} from '@sonatype/react-shared-components';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { availableScopesPropType } from './advancedLegalPropTypes';

const { initialState, userInput } = nxTextInputStateHelpers;

export default function LicenseObligationAttributionTile(props) {
  const {
    // actions
    setAttributionText,
    setAttributionScope,
    saveAttribution,
    setShowAttributionModal,
    cancelAttributionModal,
    // state
    id,
    name,
    originalAttributionText,
    attributionText,
    availableScopes,
    originalScope,
    scope,
    error,
    saveAttributionSubmitMask,
    showAttributionModal
  } = props;
  const isAttributionPresent = () => id !== null;
  const isDirty = () => attributionText !== originalAttributionText || scope !== originalScope;
  const isValid = () => isDirty() && (isAttributionPresent() || attributionText);
  const validationErrorMessage = isAttributionPresent() ? 'Must change attribution text or scope.' :
    'Must add attribution text.';
  const [attributionTextInput, setAttributionTextInput] = useState(initialState(attributionText));

  const createAttributionModal = () => {
    return <NxModal id="license-obligation-attribution-modal"
                    onClose={ () => setShowAttributionModal({ name, value: false }) }>
      <NxForm onCancel={ () => cancelAttributionModal({ name }) }
              submitBtnText="Save"
              onSubmit={ () => saveAttribution(name) }
              submitError={ error }
              submitMaskState={ saveAttributionSubmitMask }
              validationErrors={ isValid() ? undefined : validationErrorMessage }>
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            Attribution for &quot;{ name }&quot;
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxFormGroup label="Attribution Text"
                       sublabel="Enter information that will be included in the attribution report to fulfill the
                       related obligation."
                       isRequired>
            <NxTextInput type="textarea"
                         { ...attributionTextInput }
                         onChange={ payload => {
                           setAttributionText({ name, value: payload });
                           setAttributionTextInput(userInput(null, attributionText));
                         } }
              />
          </NxFormGroup>
          <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
            <select className="nx-form-select nx-form-select--long"
                    value={ scope }
                    onChange={ payload => setAttributionScope({ name, value: payload.currentTarget.value }) }>
              { availableScopes.values.map(createScopeOption) }
            </select>
          </NxFormGroup>
        </div>
      </NxForm>
    </NxModal>;
  };

  const createScopeOption = value => {
    return <option key={ value.id } value={ value.id }>{ value.label } - { value.name }</option>;
  };

  const classes = classnames('nx-tile-content', { 'license-obligation-no-attribution-text': !isAttributionPresent() });

  return (
    <section id="license-obligation-attribution-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Attribution for &quot;{ name }&quot;</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton variant="tertiary" onClick={ () => setShowAttributionModal({ name, value: true }) }>
            <NxFontAwesomeIcon icon={ isAttributionPresent() ? faPen : faPlus }/>
            <span>{ isAttributionPresent() ? 'Edit' : 'Add' } Attribution</span>
          </NxButton>
        </div>
        { showAttributionModal && createAttributionModal() }
      </header>
      <div className={ classes }>
        { isAttributionPresent() ? originalAttributionText : 'None added' }
      </div>
    </section>
  );
}

LicenseObligationAttributionTile.propTypes = {
  setAttributionText: PropTypes.func.isRequired,
  setAttributionScope: PropTypes.func.isRequired,
  saveAttribution: PropTypes.func.isRequired,
  setShowAttributionModal: PropTypes.func.isRequired,
  cancelAttributionModal: PropTypes.func.isRequired,
  id: PropTypes.string,
  name: PropTypes.string.isRequired,
  originalAttributionText: PropTypes.string,
  attributionText: PropTypes.string.isRequired,
  availableScopes: availableScopesPropType,
  originalScope: PropTypes.string,
  scope: PropTypes.string.isRequired,
  error: PropTypes.string,
  saveAttributionSubmitMask: PropTypes.bool,
  showAttributionModal: PropTypes.bool.isRequired
};
