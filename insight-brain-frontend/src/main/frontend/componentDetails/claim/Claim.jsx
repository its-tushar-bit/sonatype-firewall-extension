/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxFormGroup, NxTextInput, NxButton, NxDateInput } from '@sonatype/react-shared-components';

import RevokeClaimModal, { revokeClaimModalPropTypes } from './RevokeClaimModal';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = ({ isDirty, validationError }) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

export default function Claim({
  claimId,
  loading,
  loadError,
  claimMaskState,
  claimError,
  isDirty,
  validationError,
  inputFields,
  setGroupId,
  setExtension,
  setArtifactId,
  setCreatedTime,
  setVersion,
  setClassifier,
  setComment,
  claim,
  loadComponentIdentified,
  resetForm,
  resetTab,
  revokeMaskState,
  revokeError,
  revoke,
  resetRevokeError,
  showRevokeModal,
  toggleShowRevokeModal,
}) {
  const { groupId, artifactId, classifier, extension, version, comment, createTime } = inputFields;
  const isClaimed = !!claimId;

  useEffect(() => {
    loadComponentIdentified();

    return () => {
      resetTab();
    };
  }, []);

  function onCloseRevokeModal() {
    toggleShowRevokeModal();
    resetRevokeError();
  }

  return (
    <Fragment>
      <section className="nx-tile">
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 className="nx-h2">Claim Component</h2>
          </div>
        </header>
        <div className="nx-tile-content">
          <NxStatefulForm
            id="component-details-claim"
            autoComplete="off"
            onSubmit={claim}
            submitBtnText={isClaimed ? 'Update' : 'Claim'}
            submitMaskMessage="Claiming…"
            loadError={loadError}
            loading={loading}
            doLoad={loadComponentIdentified}
            validationErrors={getValidationMessage({ isDirty, validationError })}
            submitMaskState={claimMaskState}
            submitError={claimError}
            additionalFooterBtns={
              <Fragment>
                {isClaimed && (
                  <NxButton
                    type="button"
                    variant="error"
                    id="component-details-claim-revoke"
                    onClick={toggleShowRevokeModal}
                  >
                    Revoke
                  </NxButton>
                )}
                <NxButton type="button" id="component-details-claim-cancel" onClick={resetForm} disabled={!isDirty}>
                  Cancel
                </NxButton>
              </Fragment>
            }
          >
            <div className="nx-form-row">
              <NxFormGroup label="Group ID" isRequired>
                <NxTextInput {...groupId} onChange={setGroupId} validatable={true} id="groupId" aria-required={true} />
              </NxFormGroup>
              <NxFormGroup label="Extension" isRequired>
                <NxTextInput
                  {...extension}
                  onChange={setExtension}
                  validatable={true}
                  id="extension"
                  aria-required={true}
                />
              </NxFormGroup>
            </div>
            <div className="nx-form-row">
              <NxFormGroup label="Artifact ID" isRequired>
                <NxTextInput
                  {...artifactId}
                  onChange={setArtifactId}
                  validatable={true}
                  id="artifactId"
                  aria-required={true}
                />
              </NxFormGroup>
              <NxFormGroup label="Created" isRequired={!!createTime.trimmedValue}>
                <NxDateInput {...createTime} onChange={setCreatedTime} validatable={true} id="claim-creation-time" />
              </NxFormGroup>
            </div>
            <div className="nx-form-row">
              <NxFormGroup label="Version" isRequired>
                <NxTextInput {...version} onChange={setVersion} validatable={true} id="version" aria-required={true} />
              </NxFormGroup>
              <NxFormGroup label="Classifier">
                <NxTextInput {...classifier} onChange={setClassifier} id="classifier" />
              </NxFormGroup>
            </div>
            <NxFormGroup label="Comment">
              <NxTextInput
                type="textarea"
                className="nx-text-input--long"
                {...comment}
                onChange={setComment}
                id="comment"
              />
            </NxFormGroup>
          </NxStatefulForm>
        </div>
      </section>
      {showRevokeModal && (
        <RevokeClaimModal
          revokeMaskState={revokeMaskState}
          revokeError={revokeError}
          closeModal={onCloseRevokeModal}
          revoke={revoke}
        />
      )}
    </Fragment>
  );
}

Claim.propTypes = {
  claimId: PropTypes.string,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  claimMaskState: PropTypes.bool,
  claimError: PropTypes.string,
  isDirty: PropTypes.bool,
  showRevokeModal: PropTypes.bool,
  validationError: PropTypes.string,
  inputFields: PropTypes.object,
  toggleShowRevokeModal: PropTypes.func.isRequired,
  setGroupId: PropTypes.func.isRequired,
  setExtension: PropTypes.func.isRequired,
  setArtifactId: PropTypes.func.isRequired,
  setCreatedTime: PropTypes.func.isRequired,
  setVersion: PropTypes.func.isRequired,
  setClassifier: PropTypes.func.isRequired,
  setComment: PropTypes.func.isRequired,
  claim: PropTypes.func.isRequired,
  loadComponentIdentified: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  resetTab: PropTypes.func.isRequired,
  ...revokeClaimModalPropTypes,
};
