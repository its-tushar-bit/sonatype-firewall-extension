/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxFontAwesomeIcon,
  NxGrid,
  NxH2,
  NxModal,
  NxReadOnly,
  NxStatefulForm,
  NxSubmitMask,
} from '@sonatype/react-shared-components';
import { faCube, faExclamationCircle, faExclamationTriangle, faCircleCheck } from '@fortawesome/pro-solid-svg-icons';
import { selectCreatePRModal } from 'MainRoot/manualPullRequest/createPRModalSelectors';
import { actions } from './createPRModalSlice';

export default function CreatePRModal({ onSuccess }) {
  const dispatch = useDispatch();

  const {
    name,
    fullName,
    isModalOpen,
    breakingChangesCount,
    defaultBranch,
    targetVersion,
    currentVersion,
    submitMaskState,
    error,
  } = useSelector(selectCreatePRModal);

  const onClose = () => {
    dispatch(actions.reset());
  };

  const createPullRequest = async () => {
    const { payload } = await dispatch(actions.createPR());
    onSuccess?.(payload);
  };

  const breakingChangesText = () => {
    if (breakingChangesCount == null || breakingChangesCount < 0) {
      return (
        <>
          <NxFontAwesomeIcon
            icon={faExclamationTriangle}
            className="iq-create-pr-modal__breaking-changes iq-create-pr-modal__breaking-changes--unknown"
          />
          <span>Unknown</span>
        </>
      );
    }

    if (breakingChangesCount === 0) {
      return (
        <>
          <NxFontAwesomeIcon
            icon={faCircleCheck}
            className="iq-create-pr-modal__breaking-changes iq-create-pr-modal__breaking-changes--none"
          />
          <span>None</span>
        </>
      );
    }

    if (breakingChangesCount <= 5) {
      return (
        <>
          <NxFontAwesomeIcon
            icon={faExclamationCircle}
            className="iq-create-pr-modal__breaking-changes iq-create-pr-modal__breaking-changes--multiple"
          />
          <span>Few</span>
        </>
      );
    }

    return (
      <>
        <NxFontAwesomeIcon
          icon={faExclamationCircle}
          className="iq-create-pr-modal__breaking-changes iq-create-pr-modal__breaking-changes--multiple"
        />
        <span>Multiple</span>
      </>
    );
  };

  return (
    <>
      {isModalOpen && (
        <NxModal
          variant="narrow"
          id="iq-create-pr-modal"
          className="iq-create-pr-modal"
          aria-labelledby="iq-create-pr-modal-header"
          onCancel={onClose}
        >
          <NxStatefulForm
            onSubmit={createPullRequest}
            submitBtnText="Create"
            onCancel={onClose}
            submitError={error}
            submitErrorTitleMessage="Failure to create pull request."
          >
            <NxModal.Header className="iq-create-pr-modal__header">
              <NxH2 id="iq-create-pr-modal-header">Create Pull Request</NxH2>
            </NxModal.Header>
            <NxModal.Content>
              <NxReadOnly>
                <NxReadOnly.Label>Title</NxReadOnly.Label>
                <NxReadOnly.Data id="iq-create-pr-modal-pr-title">
                  Bump {name} to {targetVersion}
                </NxReadOnly.Data>

                <NxReadOnly.Label>Component</NxReadOnly.Label>
                <NxReadOnly.Data className="iq-create-pr-modal__component-name">
                  <NxFontAwesomeIcon className="iq-create-pr-modal__component-name-icon" icon={faCube} />
                  <span id="iq-create-pr-modal-component-name">{fullName}</span>
                </NxReadOnly.Data>
              </NxReadOnly>

              <NxGrid.Row className="iq-create-pr-modal__version-grid">
                <NxGrid.Column className="nx-grid-col--33">
                  <NxReadOnly>
                    <NxReadOnly.Label>Current Version</NxReadOnly.Label>
                    <NxReadOnly.Data id="iq-create-pr-modal-current-version">{currentVersion}</NxReadOnly.Data>
                  </NxReadOnly>
                </NxGrid.Column>
                <NxGrid.Column className="nx-grid-col--33">
                  <NxReadOnly>
                    <NxReadOnly.Label>Suggested Version</NxReadOnly.Label>
                    <NxReadOnly.Data id="iq-create-pr-modal-target-version">{targetVersion}</NxReadOnly.Data>
                  </NxReadOnly>
                </NxGrid.Column>
                <NxGrid.Column className="nx-grid-col--33">
                  <NxReadOnly>
                    <NxReadOnly.Label>Breaking Changes</NxReadOnly.Label>
                    <NxReadOnly.Data id="iq-create-pr-modal-breaking-changes">{breakingChangesText()}</NxReadOnly.Data>
                  </NxReadOnly>
                </NxGrid.Column>
              </NxGrid.Row>
              <NxReadOnly>
                <NxReadOnly.Label>Target Branch</NxReadOnly.Label>
                <NxReadOnly.Data id="iq-create-pr-modal-default-branch">{defaultBranch}</NxReadOnly.Data>
              </NxReadOnly>
            </NxModal.Content>
          </NxStatefulForm>
        </NxModal>
      )}
      {submitMaskState != null && <NxSubmitMask success={submitMaskState} />}
    </>
  );
}
