/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxStatefulForm,
  NxH2,
  NxCombobox,
  NxStatefulErrorAlert,
  NxModal,
  NxFormGroup,
} from '@sonatype/react-shared-components';

import { selectContactSlice } from './selectContactModalSelectors';

import { actions } from './selectContactModalSlice';
import UnsavedChangesModal from 'MainRoot/unsavedChangesModal/UnsavedChangesModal';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = (isDirty, isValid) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  } else if (!isValid) {
    return 'Select a user to save, or clear the text box to deselect a contact for this application';
  }
};

const SelectContactModal = () => {
  const dispatch = useDispatch();
  const {
    fetchedUsers: {
      loading: fetchLoading,
      data: fetchData,
      loadError: fetchUsersLoadingError,
      partialError: fetchUsersPartialError,
    },
    submitError,
    query,
    isContactModalOpen,
    isDirty,
    isValid,
    submitMaskState,
    selectedContact,
    showUnsavedChangesModal,
  } = useSelector(selectContactSlice);
  const formRef = useRef(null);

  useEffect(() => {
    if (selectedContact) {
      // focus the submit button when the modal opens, unless the combobox is empty.
      // This prevents the modal from initially rendering with the combobox open
      formRef?.current?.querySelector('.nx-form__submit-btn')?.focus();
    }
  }, [isContactModalOpen]);

  const onChange = (query, item) => {
    dispatch(actions.setSelectedContact(item));
    dispatch(actions.setQuery(query));
  };

  const onSearch = (query) => {
    dispatch(actions.searchUsers(query));
  };

  const onCancel = () => {
    dispatch(actions.cancelContactModal());
  };

  const saveContact = () => {
    dispatch(actions.saveContact());
  };

  return isContactModalOpen ? (
    <>
      <NxModal id="select-contact-modal" onCancel={onCancel} aria-labelledby="select-contact-modal-title">
        <NxModal.Header>
          <NxH2 id="select-contact-modal-title">Select Contact</NxH2>
        </NxModal.Header>
        <NxStatefulForm
          ref={formRef}
          onSubmit={saveContact}
          onCancel={onCancel}
          submitBtnText="Save"
          submitMaskMessage="Saving…"
          submitError={submitError}
          submitMaskState={submitMaskState}
          validationErrors={getValidationMessage(isDirty, isValid)}
          id="select-contact-modal-form"
        >
          <NxModal.Content>
            {fetchUsersPartialError && <NxStatefulErrorAlert>{fetchUsersPartialError}</NxStatefulErrorAlert>}
            <NxFormGroup
              label="Application Contact"
              sublabel="Search by name or use ‘*’ as wildcard (ex. ‘Isa*’ matches ‘Isaac Asimov’)"
              isRequired
            >
              <NxCombobox
                loading={fetchLoading}
                loadError={fetchUsersLoadingError}
                matches={fetchData}
                autoComplete={false}
                value={query}
                onChange={onChange}
                onSearch={onSearch}
              />
            </NxFormGroup>
          </NxModal.Content>
        </NxStatefulForm>
      </NxModal>
      {showUnsavedChangesModal && (
        <UnsavedChangesModal
          onContinue={() => dispatch(actions.closeContactModal())}
          onClose={() => dispatch(actions.closeUnsavedChangesModal())}
        />
      )}
    </>
  ) : null;
};

export default SelectContactModal;
