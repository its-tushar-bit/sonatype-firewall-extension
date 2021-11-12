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
import { faPen } from '@fortawesome/pro-solid-svg-icons';
import NoticesModalContainer from './NoticesModalContainer';
import MenuBarBackButton from '../../../mainHeader/MenuBar/MenuBarBackButton';

export default function NoticeDetailsHeader(props) {
  const {
    component,
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    stageTypeId,
    hash,
    noticeIndex,
    $state,
    loadComponentAndNoticeDetails,
    setShowNoticesModal,
    showNoticesModal,
    componentIdentifier,
  } = props;

  function load() {
    loadComponentAndNoticeDetails(ownerType, ownerId, hash, noticeIndex, componentIdentifier);
  }

  useEffect(load, [ownerType, ownerId, hash, noticeIndex, componentIdentifier]);

  return (
    <LoadWrapper loading={loading} error={error} retryHandler={load}>
      <MenuBarBackButton
        href={backToComponentOverviewUrl($state, ownerType, ownerId, stageTypeId, hash, componentIdentifier)}
        text="Back to Component Obligations"
      />
      <div className="nx-page-title">
        <h1 className="nx-h1">Notice Files</h1>
        {createSubtitle(availableScopes, component)}
        <div className="nx-tile__actions">
          <NxButton id="edit-notices" variant="tertiary" onClick={() => setShowNoticesModal(true)}>
            <NxFontAwesomeIcon icon={faPen} />
            <span>{'Edit'}</span>
          </NxButton>
        </div>
      </div>
      {showNoticesModal && <NoticesModalContainer />}
    </LoadWrapper>
  );
}

NoticeDetailsHeader.propTypes = {
  component: componentPropType,
  loading: PropTypes.bool,
  error: PropTypes.string,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  stageTypeId: PropTypes.string,
  hash: PropTypes.string,
  noticeIndex: PropTypes.string,
  availableScopes: availableScopesPropType,
  $state: PropTypes.object.isRequired,
  showNoticesModal: PropTypes.bool.isRequired,
  setShowNoticesModal: PropTypes.func.isRequired,
  loadComponentAndNoticeDetails: PropTypes.func.isRequired,
  componentIdentifier: PropTypes.string,
};
