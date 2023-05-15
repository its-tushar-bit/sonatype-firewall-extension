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
import { actions } from 'MainRoot/OrgsAndPolicies/moveOwner/moveOwnerSlice';
import { selectMoveOwnerSlice, selectMoveOwnerWarnings } from 'MainRoot/OrgsAndPolicies/moveOwner/moveOwnerSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import SuccessMoveAppModal from 'MainRoot/OrgsAndPolicies/moveOwner/SuccessMoveAppModal';
import SuccessMoveOrgModal from 'MainRoot/OrgsAndPolicies/moveOwner/SuccessMoveOrgModal';
import NoAvailableToMoveOrgsWarning from 'MainRoot/OrgsAndPolicies/moveOwner/NoAvailableToMoveOrgsWarning';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
import FetchCSVButton from './FetchCSVButton';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectOwnerById } from '../ownerSideNav/ownerSideNavSelectors';

const MoveOwnerModal = () => {
  const dispatch = useDispatch();
  const closeModal = () => dispatch(actions.closeMoveOwnerModal());
  const {
    isMoveOwnerModalOpen,
    submitMaskState,
    submitError,
    selectedOrganization,
    isShowSuccessModal,
    isDirty,
    fetchOrgs: { organizations, loadError, loading, isShowNoAvailableOrgsWarning },
  } = useSelector(selectMoveOwnerSlice);

  const selectedOwner = useSelector(selectSelectedOwner);
  const selectedOwnerFromMap = useSelector((state) => selectOwnerById(state, selectedOwner.id));

  const isApp = useSelector(selectIsApplication);
  const warnings = useSelector(selectMoveOwnerWarnings);
  const closeSuccessModal = () => dispatch(actions.closeSuccessModal());

  useEffect(() => {
    if (isMoveOwnerModalOpen) {
      doLoad();
    }
  }, [isMoveOwnerModalOpen]);

  useEffect(() => {
    return () => {
      closeModal();
      closeSuccessModal();
    };
  }, []);

  const moveOrganization = () => dispatch(actions.moveOrganization(selectedOrganization));
  const moveApplication = () => dispatch(actions.moveApplication(selectedOrganization));
  const doLoad = () => dispatch(actions.loadAvailableToMoveOrganizations(selectedOwner.id));

  const onChange = (event) => {
    dispatch(
      actions.setOrganization({
        movedApplicationId: isApp ? selectedOwner.id : null,
        movedOrganizationId: isApp ? null : selectedOwner.id,
        targetParentOrganizationId: event.target.value,
        currentParentOrganizationId: isApp ? selectedOwner.organizationId : selectedOwner.parentOrganizationId,
      })
    );
  };

  const getErrorProps = (submitError) => {
    if (!isApp && submitError?.response?.status === 409) {
      return {
        submitErrorTitleMessage: <b>Incompatible Destination:</b>,
        submitError: `There are configuration conflicts preventing the move operation. 
                Errors details can be accessed by fetching a CSV file for download.`,
      };
    } else if (isApp && submitError?.incompatibilities) {
      return {
        submitErrorTitleMessage: <b>Incompatible Destination:</b>,
        submitError: submitError.incompatibilities.join('. '),
      };
    }
    return {
      submitError: Messages.getHttpErrorMessage(submitError),
    };
  };

  return (
    <>
      {isMoveOwnerModalOpen && (
        <NxModal id="move-owner-modal" onCancel={closeModal}>
          <NxModal.Header>
            <NxH2>Move {selectedOwner.name}</NxH2>
          </NxModal.Header>
          {!isShowNoAvailableOrgsWarning ? (
            <>
              {!isApp && (
                <p id="move-modal-info-message">
                  Moving {selectedOwner.name} will move {selectedOwnerFromMap.subOrgs + selectedOwnerFromMap.totalApps}{' '}
                  descendants. Confirm inheritance details after the move is complete.
                </p>
              )}
              <NxStatefulForm
                onSubmit={isApp ? moveApplication : moveOrganization}
                onCancel={closeModal}
                doLoad={doLoad}
                loadError={loadError}
                loading={loading}
                submitBtnText="Move"
                submitMaskState={submitMaskState}
                validationErrors={!isDirty ? MSG_NO_CHANGES_TO_SAVE : null}
                additionalFooterBtns={
                  <FetchCSVButton
                    isApp={isApp}
                    submitError={submitError}
                    currentOrganizationId={selectedOwner?.id}
                    parentOrganizationId={selectedOrganization?.organizationId}
                  />
                }
                {...getErrorProps(submitError)}
              >
                <NxModal.Content>
                  <NxFormGroup label="New Parent Organization" isRequired>
                    <NxFormSelect onChange={onChange} defaultValue={selectedOrganization?.organizationId}>
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
      {isShowSuccessModal &&
        (isApp ? (
          <SuccessMoveAppModal warnings={warnings} closeModal={closeSuccessModal} />
        ) : (
          <SuccessMoveOrgModal warnings={warnings} closeModal={closeSuccessModal} />
        ))}
    </>
  );
};

export default MoveOwnerModal;
