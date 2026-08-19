/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxErrorAlert,
  NxModal,
  NxH2,
  NxFormGroup,
  NxTextInput,
  NxButton,
  NxStatefulForm,
} from '@sonatype/react-shared-components';
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

  useEffect(() => {
    dispatch(actions.clearCreateManagerError());
  }, [dispatch]);

  const onChangeName = (value) => {
    setManagerName(userInput(validateNonEmpty, value));
    if (createManagerError) {
      dispatch(actions.clearCreateManagerError());
    }
  };

  const onSave = () => {
    return dispatch(actions.createVirtualRepositoryManager({ name: managerName.trimmedValue }))
      .unwrap()
      .then((data) => {
        onClose(data.name);
      })
      .catch(() => {
        // Failure state is surfaced inline on the name field via createManagerError;
        // swallow here so NxStatefulForm does not show its own submit-error card + Retry.
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
        loading={creatingManager}
        additionalFooterBtns={additionalFooterButtons}
      >
        <NxModal.Header>
          <NxH2>New Virtual Repository Manager</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxFormGroup label="Virtual Repository Manager Name" isRequired>
            <NxTextInput onChange={onChangeName} {...managerName} validatable />
          </NxFormGroup>
          {createManagerError && (
            <NxErrorAlert className="iq-add-virtual-repository-manager-modal__error">{createManagerError}</NxErrorAlert>
          )}
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );
}

AddRepositoryManagerModal.propTypes = {
  onClose: PropTypes.func.isRequired,
};
