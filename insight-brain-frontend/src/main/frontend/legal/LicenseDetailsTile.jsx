/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faAngleRight } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import { licenseLegalMetadataPropType } from './advancedLegalPropTypes';

export default function LicenseDetailsTile(props) {
  const { licenseNames, licenseLegalMetadata, ownerType, ownerId, hash, $state } = props;

  const isLicensePresent = () => licenseNames.length > 0;

  /**
   * Find the index of the license in licenseMetadata.
   * According to PM, if user clicked on a multi-license in the list we should select the first license in the multi.
   */
  function findTrueLicenseIndex(index) {
    const licenseName = licenseNames[index];
    const corrected = licenseLegalMetadata.findIndex(
      (license) => !license.isMulti && license.licenseName === licenseName
    );
    if (corrected !== -1) {
      return corrected;
    }
    // Must be a multilicense
    const correctedAgain = licenseLegalMetadata.findIndex((license) => licenseName.startsWith(license.licenseName));
    return correctedAgain;
  }

  const createItem = (license, index) => {
    return (
      <li className="nx-list__item nx-list__item--link" key={index}>
        <a
          className="nx-list__link"
          href={$state.href('legal.componentLicenseDetails', {
            ownerType,
            ownerId,
            hash,
            licenseIndex: findTrueLicenseIndex(index),
          })}
        >
          <span className="nx-list__text">{license}</span>
          <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
        </a>
      </li>
    );
  };

  return (
    <section id="license-details-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Licenses</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list nx-list--clickable">
          {isLicensePresent() ? licenseNames.map(createItem) : 'None found'}
        </ul>
      </div>
    </section>
  );
}

LicenseDetailsTile.propTypes = {
  licenseNames: PropTypes.arrayOf(PropTypes.string.isRequired),
  licenseLegalMetadata: licenseLegalMetadataPropType,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string.isRequired,
  $state: PropTypes.object.isRequired,
};
