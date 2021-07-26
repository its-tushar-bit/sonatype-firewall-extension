/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxFontAwesomeIcon, NxAccordion, useToggle } from '@sonatype/react-shared-components';
import { availableScopesPropType, legalFilesPropType } from '../../advancedLegalPropTypes';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import LicensesModalContainer from './LicenseFilesModalContainer';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { createLegalFileTileItem } from '../common/utils';

export default function LicenseFilesTile(props) {
  const {
    setShowLicenseFilesModal,
    licenseFiles,
    showLicenseFilesModal,
    ownerType,
    ownerId,
    stageTypeId,
    hash,
    $state,
  } = props;

  const isLicensePresent = () => licenseFiles && licenseFiles.length > 0;

  const enabledLicenses = licenseFiles.filter((licenseFile) => licenseFile.originalStatus === 'enabled');

  const classes = classnames('nx-tile-content', {
    'license-no-legal-elements-text': !isLicensePresent(),
  });

  const licenseFileDetailsTargetState = () =>
    stageTypeId
      ? 'legal.stageTypeComponentLicenseFilesDetails.licenseFilesDetails'
      : 'legal.componentLicenseFilesDetails.licenseFilesDetails';

  const createItem = (license, index) =>
    createLegalFileTileItem('license', license, index, $state, licenseFileDetailsTargetState(), {
      ownerType,
      ownerId,
      hash,
      stageTypeId,
      licenseIndex: index,
    });

  const [open, toggleOpen] = useToggle(true);

  return (
    <React.Fragment>
      <NxAccordion open={open} onToggle={toggleOpen} id="license-texts-tile">
        <NxAccordion.Header>
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2 nx-accordion__header-title">License Files</h2>
            </div>
            <div className="nx-tile__actions">
              <NxButton id="edit-license-files" variant="tertiary" onClick={() => setShowLicenseFilesModal(true)}>
                <NxFontAwesomeIcon icon={isLicensePresent() ? faPen : faPlus} />
                <span>{isLicensePresent() ? 'Edit' : 'Add'}</span>
              </NxButton>
            </div>
          </header>
        </NxAccordion.Header>
        <div className={classes}>{enabledLicenses.length > 0 ? enabledLicenses.map(createItem) : 'None found'}</div>
      </NxAccordion>
      {showLicenseFilesModal && <LicensesModalContainer />}
    </React.Fragment>
  );
}

LicenseFilesTile.propTypes = {
  setShowLicenseFilesModal: PropTypes.func.isRequired,
  licenseFiles: legalFilesPropType,
  showLicenseFilesModal: PropTypes.bool.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  availableScopes: availableScopesPropType,
  stageTypeId: PropTypes.string,
  hash: PropTypes.string.isRequired,
  $state: PropTypes.object.isRequired,
};
