/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxLoadWrapper } from '@sonatype/react-shared-components';
import { faPen } from '@fortawesome/pro-solid-svg-icons';

import { renderLicensesList } from '../LegalTabUtils';
export default function LicenseDetectionsTile({
  licenseOverride,
  declaredlicenses,
  effectiveLicenses,
  observedlicenses,
  // selectableLicenses,
  // allLicenses,
  loadLicenses,
  loading,
  loadError,
  toggleShowEditLicensesPopover,
}) {
  const getLicenseOverrideStatus = () => {
    const status = licenseOverride
      ?.find((override) => !!override.licenseOverride?.status)
      ?.licenseOverride.status.toLowerCase();
    return status ?? 'open';
  };

  useEffect(() => {
    loadLicenses();
  }, []);

  return (
    <section className="nx-tile license-detections-tile" id="component-details-legal-license-detections-tile">
      <header className="nx-tile-header">
        <hgroup className="nx-page-title__headings">
          <div className="nx-tile-header__title">
            <h2 className="nx-h2" id="license-detections-title">
              License Detections
            </h2>
          </div>
          {!loading && !loadError && (
            <Fragment>
              <NxButton
                id="component-details-edit-licenses"
                className="nx-tile__actions"
                variant="tertiary"
                onClick={toggleShowEditLicensesPopover}
              >
                <NxFontAwesomeIcon icon={faPen} />
                <span>Edit</span>
              </NxButton>
              <h3 className="nx-tile-header__subtitle" id="status-container">
                Status:{' '}
                <span className="status-subtitle" id="status-subtitle">
                  {getLicenseOverrideStatus()}
                </span>
              </h3>
            </Fragment>
          )}
        </hgroup>
      </header>
      <div className="nx-tile-content">
        <NxLoadWrapper loading={loading} retryHandler={loadLicenses} error={loadError}>
          <div className="nx-grid-row" id="license-detections-tile">
            <div className="nx-grid-col nx-grid-col--33">
              <dl className="nx-read-only nx-read-only--grid">
                <div>
                  <dt className="nx-read-only__label">Effective Licenses</dt>
                  <dd className="nx-read-only__data" id="effective-licenses-container">
                    {renderLicensesList(effectiveLicenses)}
                  </dd>
                </div>
              </dl>
            </div>
            <div className="nx-grid-col nx-grid-col--33">
              <dl className="nx-read-only nx-read-only--grid">
                <div>
                  <dt className="nx-read-only__label">Declared Licenses</dt>
                  <dd className="nx-read-only__data" id="declared-licenses-container">
                    {renderLicensesList(declaredlicenses)}
                  </dd>
                </div>
              </dl>
            </div>
            <div className="nx-grid-col nx-grid-col--33">
              <dl className="nx-read-only nx-read-only--grid">
                <div>
                  <dt className="nx-read-only__label">Observed Licenses</dt>
                  <dd className="nx-read-only__data" id="observed-licenses-container">
                    {renderLicensesList(observedlicenses)}
                  </dd>
                </div>
              </dl>
            </div>
          </div>
        </NxLoadWrapper>
      </div>
    </section>
  );
}

export const licensePropTypes = PropTypes.shape({
  licenseId: PropTypes.string,
  licenseName: PropTypes.string,
});

export const licensesPropTypes = PropTypes.shape({
  license: licensePropTypes,
  threatLevel: PropTypes.number,
});

export const licenseOverridePropTypes = PropTypes.shape({
  licenseOverride: PropTypes.shape({
    comment: PropTypes.string,
    componentIdentifier: PropTypes.shape({
      format: PropTypes.string,
      coordinates: PropTypes.shape({
        name: PropTypes.string,
        qualifier: PropTypes.string,
        version: PropTypes.string,
      }),
    }),
    coordinates: PropTypes.shape({
      name: PropTypes.string,
      qualifier: PropTypes.string,
      version: PropTypes.string,
    }),
    name: PropTypes.string,
    qualifier: PropTypes.string,
    version: PropTypes.string,
    format: PropTypes.string,
    id: PropTypes.string,
    licenseIds: PropTypes.arrayOf(PropTypes.string),
    ownerId: PropTypes.string,
    status: PropTypes.string,
  }),
  ownerId: PropTypes.string,
  ownerName: PropTypes.string,
  ownerType: PropTypes.string,
});

LicenseDetectionsTile.propTypes = {
  allLicenses: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      longDisplayName: PropTypes.string,
      shortDisplayName: PropTypes.string,
    })
  ),
  declaredlicenses: PropTypes.arrayOf(licensesPropTypes),
  effectiveLicenses: PropTypes.arrayOf(licensesPropTypes),
  licenseOverride: PropTypes.arrayOf(licenseOverridePropTypes),
  loadLicenses: PropTypes.func.isRequired,
  observedlicenses: PropTypes.arrayOf(licensesPropTypes),
  selectableLicenses: PropTypes.arrayOf(licensePropTypes),
  loading: PropTypes.bool,
  loadError: PropTypes.string,
  toggleShowEditLicensesPopover: PropTypes.func.isRequired,
};
