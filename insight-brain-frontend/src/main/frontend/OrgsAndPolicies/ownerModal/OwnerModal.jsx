/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef, useMemo } from 'react';
import * as PropTypes from 'prop-types';

import { useSelector, useDispatch } from 'react-redux';
import { isNil } from 'ramda';
import { getRobotUrl } from 'MainRoot/util/CLMLocation';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectOwnerModalSlice,
  selectNewOwnerName,
  selectNewOwnerAppId,
  selectValidationError,
  selectIsApplication,
} from './ownerModalSelectors';
import {
  selectIsOrganization,
  selectIsRepositoryManager,
  selectIsRootOrganization,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import UnsavedChangesModal from '../../unsavedChangesModal/UnsavedChangesModal';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectDisplayedOrganization,
  selectOwnersFlattenEntries,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import {
  NxModal,
  NxH2,
  NxH3,
  NxStatefulForm,
  NxFormGroup,
  NxTextInput,
  NxFieldset,
  NxRadio,
  NxButton,
  NxFontAwesomeIcon,
  NxFileUpload,
  NxOverflowTooltip,
  NxReadOnly,
} from '@sonatype/react-shared-components';
import { actions, iconTypes } from './ownerModalSlice';
import { faSync } from '@fortawesome/free-solid-svg-icons';
import { selectIsScmOnboarding } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function OwnerModal({ shouldRedirectToNewOrg }) {
  const dispatch = useDispatch();

  const {
    isModalOpen,
    isEditMode,
    submitMaskState,
    submitError,
    ownerIconType,
    robotHash,
    ownerIcon,
    isDirty,
    isUnsavedChangesModalOpen,
  } = useSelector(selectOwnerModalSlice);

  const isRootOrg = useSelector(selectIsRootOrganization);
  const isApp = useSelector(selectIsApplication);
  const isOrganization = useSelector(selectIsOrganization);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isScmOnboarding = useSelector(selectIsScmOnboarding);
  const newOwnerName = useSelector(selectNewOwnerName);
  const ownerAppId = useSelector(selectNewOwnerAppId);
  const ownersFlatEntries = useSelector(selectOwnersFlattenEntries);
  const selectedOwner = useSelector(selectSelectedOwner);
  const validationErrors = useSelector(selectValidationError);
  const displayedOrganization = useSelector(selectDisplayedOrganization);
  const uiRouterState = useRouterState();
  const scmOnboardingHref = uiRouterState.href('scmOnboardingOrg', {
    organizationId: displayedOrganization?.id,
  });

  const contentRef = useRef(null);
  const closeModalWithCheck = () => dispatch(actions.closeModal({ isDirty }));
  const closeUnsavedChangesModal = () => dispatch(actions.closeUnsavedChangesModal());
  const closeModal = () => dispatch(actions.closeModal());
  const createNewOwner = () => dispatch(actions.createNewOwner(shouldRedirectToNewOrg));
  const editCurrentOwner = () => dispatch(actions.editCurrentOwner());
  const onChangeOwnerName = (value) =>
    dispatch(
      actions.setNewOwnerName({
        value,
        appsList: ownersFlatEntries.applications,
        orgsList: ownersFlatEntries.organizations,
        isApp,
        selectedOwner,
      })
    );
  const onChangeAppId = (value) =>
    dispatch(actions.setNewOwnerAppId({ value, appsList: ownersFlatEntries.applications, selectedOwner }));
  const updateRobotIcon = () => dispatch(actions.updateRobotIcon());
  const setCustomIcon = (file) => dispatch(actions.setCustomIcon(file));
  const setIconType = async (value) => {
    await dispatch(actions.setOwnerIconType({ value, selectedOwner }));
    scrollToBottom();
  };

  const scrollToBottom = () => {
    if (!isNil(contentRef.current)) {
      contentRef.current.scrollTop = contentRef.current.scrollHeight;
    }
  };

  useEffect(() => {
    return () => {
      closeModal();
    };
  }, []);

  const robotUrl = useMemo(() => getRobotUrl(isEditMode ? isApp : !isRootOrg, robotHash), [
    robotHash,
    isApp,
    isEditMode,
    isRootOrg,
  ]);

  const additionalFooterButtons = (
    <>
      <NxButton variant="tertiary" type="button" className="nx-form__cancel-btn" onClick={closeModalWithCheck}>
        Cancel
      </NxButton>
      {!isSbomManager && !isEditMode && isApp && (
        <a href={scmOnboardingHref} className="nx-btn nx-btn--secondary">
          Import Apps
        </a>
      )}
    </>
  );

  const getHeaderTitle = () => {
    if (isApp) {
      return 'Application';
    }
    if (isOrganization || isScmOnboarding || isSbomManager) {
      return 'Organization';
    }
    if (isRepositoryManager) {
      return 'Repository Manager';
    }
  };

  const showIconOptions = isApp || isOrganization || isScmOnboarding || isRepositoryManager;

  return (
    <>
      {isModalOpen ? (
        <NxModal id="owner-editor" onCancel={closeModalWithCheck}>
          <NxStatefulForm
            onSubmit={isEditMode ? editCurrentOwner : createNewOwner}
            submitMaskState={submitMaskState}
            submitBtnText={isEditMode ? 'Update' : 'Create'}
            submitError={submitError}
            validationErrors={validationErrors}
            additionalFooterBtns={additionalFooterButtons}
          >
            <NxModal.Header>
              <NxH2>
                {isEditMode ? 'Edit ' : 'New '}
                {getHeaderTitle()}
              </NxH2>
              {!isEditMode && (
                <NxOverflowTooltip>
                  <NxH3 className="nx-truncate-ellipsis">
                    <b>Adding to: </b>
                    {displayedOrganization?.name}
                  </NxH3>
                </NxOverflowTooltip>
              )}
            </NxModal.Header>
            <NxModal.Content ref={contentRef}>
              {isRepositoryManager && (
                <NxReadOnly>
                  <NxReadOnly.Label>Repository Manager ID</NxReadOnly.Label>
                  <NxReadOnly.Data>{selectedOwner.id}</NxReadOnly.Data>
                </NxReadOnly>
              )}

              <NxFormGroup id="editor-owner-name" label={`${getHeaderTitle()} Name`} isRequired>
                <NxTextInput onChange={onChangeOwnerName} {...newOwnerName} validatable={true} />
              </NxFormGroup>

              {!isEditMode && isApp && (
                <NxFormGroup id="editor-new-id" label="Application ID" isRequired>
                  <NxTextInput onChange={onChangeAppId} {...ownerAppId} validatable={true} />
                </NxFormGroup>
              )}

              {showIconOptions && (
                <NxFieldset label="Icon" isRequired>
                  <NxRadio name="icon" value="" onChange={setIconType} isChecked={ownerIconType === ''}>
                    Use a default icon
                  </NxRadio>
                  <NxRadio
                    name="icon"
                    value={iconTypes.custom}
                    onChange={setIconType}
                    isChecked={ownerIconType === iconTypes.custom}
                  >
                    Upload a custom icon
                  </NxRadio>
                  <NxRadio
                    name="icon"
                    value={iconTypes.robot}
                    onChange={setIconType}
                    isChecked={ownerIconType === iconTypes.robot}
                  >
                    Get a robot
                  </NxRadio>
                </NxFieldset>
              )}

              {ownerIconType === iconTypes.robot && (
                <div id="robot-icon-selector">
                  <NxButton variant="tertiary" type="button" onClick={updateRobotIcon}>
                    <NxFontAwesomeIcon icon={faSync} />
                    <span>Get Another Robot</span>
                  </NxButton>
                  <div className="iq-owner-icon-preview">
                    <img src={robotUrl} className="iq-owner-icon-large" />
                    <img src={robotUrl} className="iq-owner-icon" />
                    <img src={robotUrl} className="iq-owner-icon-tiny" />
                  </div>
                </div>
              )}

              {ownerIconType === iconTypes.custom && (
                <NxFileUpload
                  {...ownerIcon}
                  isRequired
                  accept="image/jpeg, image/png, image/gif, image/bmp, image/wbmp"
                  onChange={setCustomIcon}
                  aria-label="upload icon image"
                />
              )}
            </NxModal.Content>
          </NxStatefulForm>
        </NxModal>
      ) : null}

      {isUnsavedChangesModalOpen ? (
        <UnsavedChangesModal onContinue={closeModal} onClose={closeUnsavedChangesModal} />
      ) : null}
    </>
  );
}

OwnerModal.propTypes = {
  shouldRedirectToNewOrg: PropTypes.bool,
};
