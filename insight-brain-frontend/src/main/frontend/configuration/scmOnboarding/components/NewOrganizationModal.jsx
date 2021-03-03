/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {useState} from 'react';
import * as PropTypes from 'prop-types';
import {NxFontAwesomeIcon, NxForm, NxFormGroup, NxModal, NxTextInput} from '@sonatype/react-shared-components';
import {faSitemap} from '@fortawesome/pro-solid-svg-icons';
import {initialState, userInput} from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import {isNil, reject} from 'ramda';
import {validateNonEmpty} from '../../../util/validationUtil';
import LoadError from '../../../react/LoadError';
import {Messages} from '../../../util/CommonServices';

function NewOrganizationModal({setIsNewOrganizationModalVisible, addOrganization, addOrganizationError}) {

  const validateOrgNameChange = (val) => reject(isNil, [validateNonEmpty(val)]);

  const [newOrganizationName, setNewOrganizationName] = useState(initialState('', validateOrgNameChange));

  const newOrganizationNameChanged = (newValue) => setNewOrganizationName(userInput(validateOrgNameChange, newValue));

  const addOrganizationClicked = () => addOrganization(newOrganizationName.value);

  const closeModal = () => setIsNewOrganizationModalVisible(false);

  const submitError = Messages.getHttpErrorMessage(addOrganizationError);

  return (
    <NxModal onClose={closeModal} variant='narrow' id='new-organization-modal'>
      <NxForm
          onSubmit={addOrganizationClicked}
          onCancel={closeModal}
          validationErrors={newOrganizationName.validationErrors}
          submitError={submitError}
          submitBtnText='Create'
          submitErrorTitleMessage='Failed to create organization.'>
        <header className='nx-modal-header'>
          <h2 className='nx-h2'>
            <NxFontAwesomeIcon icon={faSitemap}/>
            <span>New Organization</span>
          </h2>
        </header>
        <div className='nx-modal-content'>
          <NxFormGroup label='Organization Name' isRequired>
            <NxTextInput
                id='new-organization-modal-org-name'
                {...newOrganizationName}
                aria-required={true}
                placeholder='Organization'
                validatable={true}
                onChange={newOrganizationNameChanged}
            />
          </NxFormGroup>
        </div>
      </NxForm>
    </NxModal>
  );
}

NewOrganizationModal.propTypes = {
  setIsNewOrganizationModalVisible: PropTypes.func.isRequired,
  addOrganization: PropTypes.func.isRequired,
  addOrganizationError: LoadError.propTypes.error
};

export default NewOrganizationModal;
