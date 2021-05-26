/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { availableScopesPropType } from '../../advancedLegalPropTypes';
import LoadWrapper from '../../../react/LoadWrapper';
import { NxBackButton, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { createSubtitle } from '../../legalUtility';
import { faPen } from '@fortawesome/pro-solid-svg-icons';
import NoticesModalContainer from './NoticesModalContainer';

export default function NoticeDetailsHeader(props) {
  const {
    loading,
    error,
    availableScopes,
    ownerType,
    ownerId,
    hash,
    noticeIndex,
    $state,
    loadComponentAndNoticeDetails,
    setShowNoticesModal,
    showNoticesModal,
  } = props;

  const backUrl = () => {
    const state =
      ownerType === 'organization' ? 'legal.organizationComponentOverview' : 'legal.applicationComponentOverview';
    const params = {
      [ownerType === 'organization' ? 'organizationId' : 'applicationPublicId']: ownerId,
      hash: hash,
    };
    return $state.href($state.get(state), params);
  };

  function load() {
    loadComponentAndNoticeDetails(ownerType, ownerId, hash, noticeIndex);
  }

  useEffect(load, [ownerType, ownerId, hash, noticeIndex]);

  return (
    <LoadWrapper loading={loading} error={error} retryHandler={load}>
      <NxBackButton href={backUrl()} targetPageTitle="Component Obligations" />
      <div className="nx-page-title">
        <h1 className="nx-h1">Notice Files</h1>
        {createSubtitle(availableScopes)}
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
  loading: PropTypes.bool,
  error: PropTypes.string,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  noticeIndex: PropTypes.string,
  availableScopes: availableScopesPropType,
  $state: PropTypes.object.isRequired,
  showNoticesModal: PropTypes.bool.isRequired,
  setShowNoticesModal: PropTypes.func.isRequired,

  loadComponentAndNoticeDetails: PropTypes.func.isRequired,
};
