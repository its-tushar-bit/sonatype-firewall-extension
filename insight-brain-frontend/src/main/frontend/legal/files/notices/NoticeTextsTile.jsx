/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxFontAwesomeIcon, NxAccordion, useToggle } from '@sonatype/react-shared-components';
import { availableScopesPropType, legalFilesPropType } from '../../advancedLegalPropTypes';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import NoticesModalContainer from './NoticesModalContainer';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { LegalFileTileItem } from '../common/utils';
import { LEGAL_PARENT_ROUTE, LEGAL_SBOM_MANAGER_PARENT_ROUTE } from 'MainRoot/legal/legalUtility';

export default function NoticeTextsTile(props) {
  const {
    setShowNoticesModal,
    noticeFiles,
    showNoticesModal,
    ownerType,
    ownerId,
    stageTypeId,
    hash,
    componentIdentifier,
    isSbomManager,
  } = props;

  const isNoticePresent = () => noticeFiles && noticeFiles.length > 0;

  const enabledNotices = noticeFiles.filter((noticeFile) => noticeFile.originalStatus === 'enabled');

  const classes = classnames({
    'license-no-legal-elements-text': !isNoticePresent(),
  });

  const noticeDetailsTargetState = () => {
    const prefix = isSbomManager ? LEGAL_SBOM_MANAGER_PARENT_ROUTE : LEGAL_PARENT_ROUTE;
    if (hash) {
      return stageTypeId
        ? `${prefix}.stageTypeComponentNoticeDetails.noticeDetails`
        : `${prefix}.componentNoticeDetails.noticeDetails`;
    } else {
      return `${prefix}.noticeFilesByComponentIdentifier.noticeDetails`;
    }
  };

  const createItem = (notice, index) => (
    <LegalFileTileItem
      key={index}
      legalFileType="notice"
      object={notice}
      index={index}
      targetStateName={noticeDetailsTargetState()}
      routeParams={{
        ownerType,
        ownerId,
        hash,
        stageTypeId,
        noticeIndex: index,
        componentIdentifier,
      }}
    />
  );

  const [open, toggleOpen] = useToggle(true);

  return (
    <React.Fragment>
      <NxAccordion open={open} onToggle={toggleOpen} id="notice-texts-tile">
        <NxAccordion.Header>
          <NxAccordion.Title>Notice Files</NxAccordion.Title>
          <div className="nx-btn-bar">
            <NxButton id="edit-notices" variant="tertiary" onClick={() => setShowNoticesModal(true)}>
              <NxFontAwesomeIcon icon={isNoticePresent() ? faPen : faPlus} />
              <span>{isNoticePresent() ? 'Edit' : 'Add'}</span>
            </NxButton>
          </div>
        </NxAccordion.Header>
        <div className={classes}>{enabledNotices.length > 0 ? enabledNotices.map(createItem) : 'None found'}</div>
      </NxAccordion>
      {showNoticesModal && <NoticesModalContainer />}
    </React.Fragment>
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
  hash: PropTypes.string,
  componentIdentifier: PropTypes.string,
  isSbomManager: PropTypes.bool,
};
