/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  NxButton,
  NxCheckbox,
  NxFontAwesomeIcon,
  NxModal,
  NxTextInput,
  NxFormGroup,
  NxForm,
  NxFieldset
} from '@sonatype/react-shared-components';
import { faPlus, faPen } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { availableScopesPropType } from './advancedLegalPropTypes';

export default function LicenseObligationAttributionTile(props) {
  const {
    // actions
    setAttributionText,
    setObligationFulfilled,
    setScope,
    // state
    name,
    attributionText,
    obligationFulfilled,
    availableScopes,
    scope
  } = props;
  const [showAttributionModal, setShowAttributionModal] = useState(false);
  const attributionModalCloseHandler = () => setShowAttributionModal(false);
  const isAttributionPresent = () => attributionText !== '';

  function load() {
    setAttributionText({ name: name, value: attributionText });
    setObligationFulfilled({ name: name, value: obligationFulfilled });
    setScope({ name: name, value: availableScopes.values[availableScopes.values.length - 1].id });
  }

  useEffect(load, [availableScopes]);

  const createAttributionModal = () => {
    return <NxModal id="license-obligation-attribution-modal" onClose={ attributionModalCloseHandler }>
      <NxForm onCancel={ attributionModalCloseHandler }
              submitBtnText="Save"
              onSubmit={ attributionModalCloseHandler }>
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
                         isPristine={ true }
                         value={ attributionText }
                         onChange={ payload => setAttributionText({ name: name, value: payload }) }
              />
          </NxFormGroup>
          <NxFieldset label="Update Obligation Review Status">
            <NxCheckbox isChecked={ obligationFulfilled }
                        onChange={ () => setObligationFulfilled(({ name: name, value: !obligationFulfilled })) }>
              Mark &quot;{ name }&quot; as fulfilled.
            </NxCheckbox>
          </NxFieldset>
          <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
            <select className="nx-form-select"
                    value={ scope }
                    onChange={ payload => setScope({ name: name, value: payload.currentTarget.value }) }>
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
          <NxButton variant="tertiary" onClick={ () => setShowAttributionModal(true) }>
            <NxFontAwesomeIcon icon={ isAttributionPresent() ? faPen : faPlus }/>
            <span>{ isAttributionPresent() ? 'Edit' : 'Add' } Attribution</span>
          </NxButton>
        </div>
        { showAttributionModal && createAttributionModal() }
      </header>
      <div className={ classes }>
        { isAttributionPresent() ? attributionText : 'None added' }
      </div>
    </section>
  );
}

LicenseObligationAttributionTile.propTypes = {
  setAttributionText: PropTypes.func.isRequired,
  setObligationFulfilled: PropTypes.func.isRequired,
  setScope: PropTypes.func.isRequired,
  name: PropTypes.string.isRequired,
  attributionText: PropTypes.string,
  obligationFulfilled: PropTypes.bool.isRequired,
  availableScopes: availableScopesPropType,
  scope: PropTypes.string
};
