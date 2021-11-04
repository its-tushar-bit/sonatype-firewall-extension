/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { availableScopesPropType, componentPropType } from '../advancedLegalPropTypes';
import LoadWrapper from '../../react/LoadWrapper';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faEdit } from '@fortawesome/free-solid-svg-icons';
import { backToComponentOverviewUrl, createSubtitle } from '../legalUtility';
import CopyrightOverrideFormContainer from './CopyrightOverrideFormContainer';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';

export default function CopyrightDetailsHeader(props) {
  const {
    component,
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    hash,
    componentIdentifier,
    stageTypeId,
    copyrightIndex,
    $state,
    showEditCopyrightOverrideModal,

    loadComponentAndCopyrightDetails,
    setDisplayCopyrightOverrideModal,
  } = props;

  function load() {
    loadComponentAndCopyrightDetails(ownerType, ownerId, hash, copyrightIndex, componentIdentifier);
  }

  useEffect(load, [ownerType, ownerId, hash, copyrightIndex, componentIdentifier]);

  return (
    <LoadWrapper loading={loading} error={error} retryHandler={load}>
      <MenuBarBackButton
        href={backToComponentOverviewUrl($state, ownerType, ownerId, stageTypeId, hash, componentIdentifier)}
        text="Back to Component Obligations"
      />
      <div className="nx-page-title">
        <h1 className="nx-h1">Copyright Notices</h1>
        {createSubtitle(availableScopes, component)}
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
  component: componentPropType,
  loading: PropTypes.bool,
  error: PropTypes.string,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string,
  componentIdentifier: PropTypes.string,
  stageTypeId: PropTypes.string,
  copyrightIndex: PropTypes.string,
  availableScopes: availableScopesPropType,
  $state: PropTypes.object.isRequired,
  showEditCopyrightOverrideModal: PropTypes.bool,

  loadComponentAndCopyrightDetails: PropTypes.func.isRequired,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
};
