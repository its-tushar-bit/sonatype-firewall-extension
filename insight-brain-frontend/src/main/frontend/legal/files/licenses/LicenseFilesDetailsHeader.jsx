/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { availableScopesPropType, componentPropType } from '../../advancedLegalPropTypes';
import LoadWrapper from '../../../react/LoadWrapper';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { backToComponentOverviewUrl, createSubtitle } from '../../legalUtility';
import { faEdit } from '@fortawesome/free-solid-svg-icons';
import LicenseFilesModalContainer from './LicenseFilesModalContainer';
import MenuBarBackButton from '../../../mainHeader/MenuBar/MenuBarBackButton';

export default function LicenseFilesDetailsHeader(props) {
  const {
    component,
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    stageTypeId,
    hash,
    componentIdentifier,
    licenseIndex,
    $state,
    loadComponentAndLicenseDetails,
    setShowLicenseFilesModal,
    showLicenseFilesModal,
  } = props;

  function load() {
    loadComponentAndLicenseDetails(ownerType, ownerId, hash, licenseIndex, componentIdentifier);
  }

  useEffect(load, [ownerType, ownerId, hash, licenseIndex]);

  return (
    <LoadWrapper loading={loading} error={error} retryHandler={load}>
      <MenuBarBackButton
        href={backToComponentOverviewUrl($state, ownerType, ownerId, stageTypeId, hash, componentIdentifier)}
        text="Back to Component Obligations"
      />
      <div className="nx-page-title">
        <h1 className="nx-h1">License Files</h1>
        {createSubtitle(availableScopes, component)}
        <div className="nx-btn-bar">
          <NxButton id="edit-license-files" variant="tertiary" onClick={() => setShowLicenseFilesModal(true)}>
            <NxFontAwesomeIcon icon={faEdit} />
            <span>Edit License Files</span>
          </NxButton>
        </div>
      </div>
      {showLicenseFilesModal && <LicenseFilesModalContainer />}
    </LoadWrapper>
  );
}

LicenseFilesDetailsHeader.propTypes = {
  component: componentPropType,
  loading: PropTypes.bool,
  error: PropTypes.string,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  stageTypeId: PropTypes.string,
  hash: PropTypes.string,
  componentIdentifier: PropTypes.string,
  licenseIndex: PropTypes.string,
  availableScopes: availableScopesPropType,
  $state: PropTypes.object.isRequired,
  showLicenseFilesModal: PropTypes.bool,

  loadComponentAndLicenseDetails: PropTypes.func.isRequired,
  setShowLicenseFilesModal: PropTypes.func.isRequired,
};
