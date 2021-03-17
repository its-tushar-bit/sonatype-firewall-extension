/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { legalFilesPropType } from '../../advancedLegalPropTypes';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import LicensesModalContainer from './LicensesModalContainer';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function LicenseTextsTile(props) {
  const {
    setShowLicensesModal,
    licenseFiles,
    showLicensesModal
  } = props;

  const isLicensePresent = () => licenseFiles.length > 0;

  const classes = classnames('nx-tile-content', { 'license-no-legal-elements-text': !isLicensePresent() });

  return (
    <section id="license-texts-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">License Texts</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton id="edit-licenses" variant="tertiary" onClick={ () => setShowLicensesModal(true) }>
            <NxFontAwesomeIcon icon={ isLicensePresent() ? faPen : faPlus }/>
            <span>{ isLicensePresent() ? 'Edit' : 'Add' }</span>
          </NxButton>
        </div>
        { showLicensesModal && <LicensesModalContainer/> }
      </header>
      <div className={ classes }>
        { isLicensePresent() ? licenseFiles.map(createItem) : 'None found' }
      </div>
    </section>
  );
}

const createItem = (license, index) => (
  <section id={ 'license-section-' + index } key={ index } className="nx-tile-subsection legal-file">
    <div className="legal-file-section-header">
      <span id={ 'license-path-' + index } className="legal-file-path">{ license.relPath }</span>
      <span className="nx-tile__actions">
        <a href="">View More Details <NxFontAwesomeIcon icon={ faChevronRight }/></a>
      </span>
    </div>
    <blockquote id={ 'license-text-' + index } className="nx-blockquote">
      <div className="legal-file-content">
        { license.originalContent }
      </div>
    </blockquote>
  </section>
);

LicenseTextsTile.propTypes = {
  setShowLicensesModal: PropTypes.func.isRequired,
  licenseFiles: legalFilesPropType,
  showLicensesModal: PropTypes.bool.isRequired
};
