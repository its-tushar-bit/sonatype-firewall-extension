/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxModal, NxH2, NxFormGroup, NxTextInput, NxButton, NxStatefulForm } from '@sonatype/react-shared-components';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { validateNonEmpty } from 'MainRoot/util/validationUtil';
import { actions } from 'MainRoot/firewall/iqProxy/firewallIqProxySlice';
import { selectCreatingManager, selectCreateManagerError } from 'MainRoot/firewall/iqProxy/firewallIqProxySelectors';

const { initialState, userInput } = nxTextInputStateHelpers;

export default function AddRepositoryManagerModal({ onClose }) {
  const [managerName, setManagerName] = useState(initialState('', validateNonEmpty));
  const dispatch = useDispatch();
  const creatingManager = useSelector(selectCreatingManager);
  const createManagerError = useSelector(selectCreateManagerError);

  const onChangeName = (value) => {
    setManagerName(userInput(validateNonEmpty, value));
  };

  const onSave = () => {
    return dispatch(actions.createVirtualRepositoryManager({ name: managerName.trimmedValue }))
      .unwrap()
      .then((data) => {
        onClose(data.name);
      });
  };

  const additionalFooterButtons = (
    <NxButton variant="tertiary" type="button" className="nx-form__cancel-btn" onClick={() => onClose(null)}>
      Cancel
    </NxButton>
  );

  return (
    <NxModal id="add-repository-manager-modal" onCancel={() => onClose(null)}>
      <NxStatefulForm
        onSubmit={onSave}
        submitBtnText="Save"
        validationErrors={managerName.validationErrors}
        submitError={createManagerError}
        loading={creatingManager}
        additionalFooterBtns={additionalFooterButtons}
      >
        <NxModal.Header>
          <NxH2>New Repository Manager</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxFormGroup label="Repository Manager Name" isRequired>
            <NxTextInput onChange={onChangeName} {...managerName} validatable />
          </NxFormGroup>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );
}

AddRepositoryManagerModal.propTypes = {
  onClose: PropTypes.func.isRequired,
};
