/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {useEffect, useState} from 'react';
import * as PropTypes from 'prop-types';
import {NxFontAwesomeIcon, NxForm, NxFormGroup, NxModal, NxTextInput} from '@sonatype/react-shared-components';
import {faSitemap} from '@fortawesome/pro-solid-svg-icons';
import {initialState, userInput} from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import {isNil, reject} from 'ramda';
import {validateMaxLength, validateNonEmpty, validatePatternMatch} from '../../../util/validationUtil';
import LoadError from '../../../react/LoadError';
import {Messages} from '../../../util/CommonServices';

/*
 * Displays a modal to allow creation of a new IQ organization.
 *
 * * setIsNewOrganizationModalVisible is a function to close the modal
 * * addOrganization is a function to create a new organization
 * * addOrganizationError is displayed in case of an error
 *
 * This component currently lives within scmOnboarding but the intention is that i can be used as generic component in
 * IQ. See https://issues.sonatype.org/browse/INT-4524
 */
function NewOrganizationModal({setIsNewOrganizationModalVisible, addOrganization, addOrganizationError}) {

  const ORGANIZATION_REGEX = /^[^!@#$%^&*()\\=£+|[\]{};:~`"',.<>/?]*$/;

  const validateOrgNameChange = (val) => reject(isNil, [
    validateNonEmpty(val),
    validatePatternMatch(ORGANIZATION_REGEX, 'Organization name contains an invalid character', val),
    validateMaxLength(200, val)]);

  const [newOrganizationName, setNewOrganizationName] = useState(initialState('', validateOrgNameChange));

  const [submitError, setSubmitError] = useState(null);

  const newOrganizationNameChanged = (newValue) => {
    setNewOrganizationName(userInput(validateOrgNameChange, newValue));
    setSubmitError(null);
  };

  const addOrganizationClicked = () => addOrganization(newOrganizationName.trimmedValue);

  const closeModal = () => setIsNewOrganizationModalVisible(false);

  useEffect(() => setSubmitError(Messages.getHttpErrorMessage(addOrganizationError)), [addOrganizationError]);

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
