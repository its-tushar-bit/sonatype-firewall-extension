/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { availableScopesPropType } from '../advancedLegalPropTypes';
import LoadWrapper from '../../react/LoadWrapper';
import { NxBackButton, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faEdit } from '@fortawesome/free-solid-svg-icons';
import { backToComponentOverviewUrl, createSubtitle } from '../legalUtility';
import CopyrightOverrideFormContainer from './CopyrightOverrideFormContainer';

export default function CopyrightDetailsHeader(props) {
  const {
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    hash,
    stageTypeId,
    copyrightIndex,
    $state,
    showEditCopyrightOverrideModal,

    loadComponentAndCopyrightDetails,
    setDisplayCopyrightOverrideModal,
  } = props;

  function load() {
    loadComponentAndCopyrightDetails(ownerType, ownerId, hash, copyrightIndex);
  }

  useEffect(load, [ownerType, ownerId, hash, copyrightIndex]);

  return (
    <LoadWrapper loading={loading} error={error} retryHandler={load}>
      <NxBackButton
        href={backToComponentOverviewUrl($state, ownerType, ownerId, stageTypeId, hash)}
        targetPageTitle="Component Obligations"
      />
      <div className="nx-page-title">
        <h1 className="nx-h1">Copyrights</h1>
        {createSubtitle(availableScopes)}
        <div className="nx-btn-bar">
          <NxButton variant="tertiary" onClick={setDisplayCopyrightOverrideModal}>
            <NxFontAwesomeIcon icon={faEdit} />
            <span>Edit/Add Copyrights</span>
          </NxButton>
        </div>
        {showEditCopyrightOverrideModal && <CopyrightOverrideFormContainer />}
      </div>
    </LoadWrapper>
  );
}

CopyrightDetailsHeader.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string,
  stageTypeId: PropTypes.string,
  copyrightIndex: PropTypes.string,
  availableScopes: availableScopesPropType,
  $state: PropTypes.object.isRequired,
  showEditCopyrightOverrideModal: PropTypes.bool,

  loadComponentAndCopyrightDetails: PropTypes.func.isRequired,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
};
