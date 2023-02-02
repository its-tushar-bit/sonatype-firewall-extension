/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxModal,
  NxH2,
  NxStatefulForm,
  NxFormGroup,
  NxFormSelect,
  NxButton,
  NxFooter,
  NxButtonBar,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/moveApplicationModal/moveApplicationSlice';
import { selectMoveApplicationSlice } from 'MainRoot/OrgsAndPolicies/moveApplicationModal/moveApplicationSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import SuccessMoveAppModal from 'MainRoot/OrgsAndPolicies/moveApplicationModal/SuccessMoveAppModal';
import NoAvailableToMoveOrgsWarning from 'MainRoot/OrgsAndPolicies/moveApplicationModal/NoAvailableToMoveOrgsWarning';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const MoveApplicationModal = () => {
  const dispatch = useDispatch();
  const closeModal = () => dispatch(actions.closeMoveAppModal());
  const {
    isMoveAppModalOpen,
    submitMaskState,
    submitError,
    selectedOrganization,
    isShowSuccessModal,
    isDirty,
    fetchOrgs: { organizations, loadError, loading, isShowNoAvailableOrgsWarning },
  } = useSelector(selectMoveApplicationSlice);
  const selectedOwner = useSelector(selectSelectedOwner);

  useEffect(() => {
    if (isMoveAppModalOpen) {
      doLoad();
    }
  }, [isMoveAppModalOpen]);

  useEffect(() => {
    return () => {
      closeModal();
      dispatch(actions.closeSuccessModal());
    };
  }, []);

  const moveApplication = () => dispatch(actions.moveApplication(selectedOrganization));
  const doLoad = () => dispatch(actions.loadAvailableToMoveOrganizations(selectedOwner.id));

  const onChange = (event) => {
    dispatch(
      actions.setOrganization({
        applicationId: selectedOwner.id,
        selectedOrganizationId: event.target.value,
        currentParentOrganization: selectedOwner.organizationId,
      })
    );
  };

  const getErrorProps = (submitError) => {
    if (submitError?.incompatibilities) {
      return {
        submitErrorTitleMessage: <b>Incompatible Destinations:</b>,
        submitError: submitError.incompatibilities.join('. '),
      };
    }
    return {
      submitError: Messages.getHttpErrorMessage(submitError),
    };
  };

  return (
    <>
      {isMoveAppModalOpen && (
        <NxModal id="move-application-modal" onCancel={closeModal}>
          <NxModal.Header>
            <NxH2>Move Application</NxH2>
          </NxModal.Header>
          {!isShowNoAvailableOrgsWarning ? (
            <>
              <NxStatefulForm
                onSubmit={moveApplication}
                onCancel={closeModal}
                doLoad={doLoad}
                loadError={loadError}
                loading={loading}
                submitBtnText="Move"
                submitMaskState={submitMaskState}
                validationErrors={!isDirty ? MSG_NO_CHANGES_TO_SAVE : null}
                {...getErrorProps(submitError)}
              >
                <NxModal.Content>
                  <NxFormGroup label="New Parent Organization" isRequired>
                    <NxFormSelect onChange={onChange}>
                      {organizations.map(({ organizationId, organizationName }) => (
                        <option key={organizationId} value={organizationId}>
                          {organizationName}
                        </option>
                      ))}
                    </NxFormSelect>
                  </NxFormGroup>
                </NxModal.Content>
              </NxStatefulForm>
              {!!loadError && (
                <NxFooter>
                  <NxButtonBar>
                    <NxButton onClick={closeModal}>OK</NxButton>
                  </NxButtonBar>
                </NxFooter>
              )}
            </>
          ) : (
            <NoAvailableToMoveOrgsWarning closeModal={closeModal} />
          )}
        </NxModal>
      )}
      {isShowSuccessModal && <SuccessMoveAppModal />}
    </>
  );
};

export default MoveApplicationModal;
