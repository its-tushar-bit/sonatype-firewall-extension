/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { availableScopesPropType, legalFilesPropType } from '../../advancedLegalPropTypes';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import NoticesModalContainer from './NoticesModalContainer';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { createLegalFileTileItem } from '../common/utils';

export default function NoticeTextsTile(props) {
  const { setShowNoticesModal, noticeFiles, showNoticesModal, ownerType, ownerId, stageTypeId, hash, $state } = props;

  const isNoticePresent = () => noticeFiles && noticeFiles.length > 0;

  const enabledNotices = noticeFiles.filter((noticeFile) => noticeFile.originalStatus === 'enabled');

  const classes = classnames('nx-tile-content', {
    'license-no-legal-elements-text': !isNoticePresent(),
  });

  const noticeDetailsTargetState = () =>
    stageTypeId ? 'legal.stageTypeComponentNoticeDetails.noticeDetails' : 'legal.componentNoticeDetails.noticeDetails';

  const createItem = (license, index) =>
    createLegalFileTileItem('notice', license, index, $state, noticeDetailsTargetState(), {
      ownerType,
      ownerId,
      hash,
      stageTypeId,
      noticeIndex: index,
    });

  return (
    <section id="notice-texts-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Notice Files</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton id="edit-notices" variant="tertiary" onClick={() => setShowNoticesModal(true)}>
            <NxFontAwesomeIcon icon={isNoticePresent() ? faPen : faPlus} />
            <span>{isNoticePresent() ? 'Edit' : 'Add'}</span>
          </NxButton>
        </div>
        {showNoticesModal && <NoticesModalContainer />}
      </header>
      <div className={classes}>{enabledNotices.length > 0 ? enabledNotices.map(createItem) : 'None found'}</div>
    </section>
  );
}

NoticeTextsTile.propTypes = {
  setShowNoticesModal: PropTypes.func.isRequired,
  noticeFiles: legalFilesPropType,
  showNoticesModal: PropTypes.bool.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  stageTypeId: PropTypes.string,
  availableScopes: availableScopesPropType,
  hash: PropTypes.string.isRequired,
  $state: PropTypes.object.isRequired,
};
