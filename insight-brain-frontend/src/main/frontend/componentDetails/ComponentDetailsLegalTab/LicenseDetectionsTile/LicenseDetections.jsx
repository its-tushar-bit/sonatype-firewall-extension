/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxLoadWrapper, NxTextLink } from '@sonatype/react-shared-components';
import { faPen } from '@fortawesome/pro-solid-svg-icons';

import { renderLicensesList, renderObservedLicenses } from '../LegalTabUtils';
import { useSelector } from 'react-redux';
import { selectIsPrioritiesPageContainer } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function LicenseDetections({
  licenseOverride,
  declaredLicenses,
  effectiveLicenses,
  observedLicenses,
  hiddenObservedLicenses,
  supportAlpObservedLicenses,
  loadLicenses,
  loading,
  loadError,
  toggleShowEditLicensesPopover,
  identificationSource,
  reviewObligationsClickHandler,
  getReviewObligationsHref,
  isAdvancedLegalPackSupported,
}) {
  useEffect(() => {
    loadLicenses();
  }, []);

  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);

  const isClaimed = identificationSource === 'Manual';

  const getLicenseOverrideStatus = () => {
    const status = licenseOverride
      ?.find((override) => !!override.licenseOverride?.status)
      ?.licenseOverride.status.toLowerCase();
    return status ?? 'open';
  };

  const getExternalLinkOrButton = () => {
    return isPrioritiesPageContainer ? (
      <NxTextLink
        id="component-details-review-obligations"
        className="nx-btn nx-btn--primary"
        newTab
        variant="primary"
        href={getReviewObligationsHref()}
      >
        <span>Review Obligations</span>
      </NxTextLink>
    ) : (
      <NxButton id="component-details-review-obligations" variant="primary" onClick={reviewObligationsClickHandler}>
        <span>Review Obligations</span>
      </NxButton>
    );
  };

  return (
    <NxLoadWrapper loading={loading} retryHandler={loadLicenses} error={loadError}>
      {() => (
        <Fragment>
          <header className="nx-tile-header">
            <hgroup className="nx-page-title__headings">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2" id="license-detections-title">
                  License Detections
                </h2>
              </div>
              <div className="nx-tile__actions">
                <NxButton
                  id="component-details-edit-licenses"
                  variant="tertiary"
                  onClick={toggleShowEditLicensesPopover}
                >
                  <NxFontAwesomeIcon icon={faPen} />
                  <span>Edit</span>
                </NxButton>

                {isAdvancedLegalPackSupported && getExternalLinkOrButton()}
              </div>
              <h3 className="nx-tile-header__subtitle" id="status-container">
                Status:{' '}
                <span className="status-subtitle" id="status-subtitle" data-testid="status-subtitle">
                  {getLicenseOverrideStatus()}
                </span>
              </h3>
            </hgroup>
          </header>
          <div className="nx-tile-content">
            <div className="nx-grid-row" id="license-detections-tile">
              <div className="nx-grid-col nx-grid-col--33">
                <dl className="nx-read-only nx-read-only--grid">
                  <div>
                    <dt className="nx-read-only__label">Effective Licenses</dt>
                    <dd
                      className="nx-read-only__data"
                      id="effective-licenses-container"
                      data-testid="effective-licenses-container"
                    >
                      <ul className="iq-legal-list">{renderLicensesList(effectiveLicenses, isClaimed, true)}</ul>
                    </dd>
                  </div>
                </dl>
              </div>
              <div className="nx-grid-col nx-grid-col--33">
                <dl className="nx-read-only nx-read-only--grid">
                  <div>
                    <dt className="nx-read-only__label">Declared Licenses</dt>
                    <dd
                      className="nx-read-only__data"
                      id="declared-licenses-container"
                      data-testid="declared-licenses-container"
                    >
                      <ul className="iq-legal-list">{renderLicensesList(declaredLicenses, isClaimed)}</ul>
                    </dd>
                  </div>
                </dl>
              </div>
              <div className="nx-grid-col nx-grid-col--33">
                <dl className="nx-read-only nx-read-only--grid">
                  <div>
                    <dt className="nx-read-only__label">Observed Licenses</dt>
                    <dd
                      className="nx-read-only__data"
                      id="observed-licenses-container"
                      data-testid="observed-licenses-container"
                    >
                      <ul className="iq-legal-list">
                        {renderObservedLicenses(
                          observedLicenses,
                          isClaimed,
                          hiddenObservedLicenses,
                          isAdvancedLegalPackSupported,
                          supportAlpObservedLicenses
                        )}
                      </ul>
                    </dd>
                  </div>
                </dl>
              </div>
            </div>
          </div>
        </Fragment>
      )}
    </NxLoadWrapper>
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

export const multiLicensesPropTypes = PropTypes.shape({
  licenseId: PropTypes.string.isRequired,
  licenseName: PropTypes.string.isRequired,
  licenses: PropTypes.arrayOf(licensesPropTypes).isRequired,
});

export const licenseOverridePropTypes = PropTypes.shape({
  licenseOverride: PropTypes.shape({
    comment: PropTypes.string,
    componentIdentifier: PropTypes.shape({
      format: PropTypes.string,
      coordinates: PropTypes.object,
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

LicenseDetections.propTypes = {
  declaredLicenses: PropTypes.arrayOf(multiLicensesPropTypes),
  effectiveLicenses: PropTypes.arrayOf(multiLicensesPropTypes),
  licenseOverride: PropTypes.arrayOf(licenseOverridePropTypes),
  loadLicenses: PropTypes.func.isRequired,
  observedLicenses: PropTypes.arrayOf(multiLicensesPropTypes),
  hiddenObservedLicenses: PropTypes.bool,
  supportAlpObservedLicenses: PropTypes.bool,
  loading: PropTypes.bool,
  loadError: PropTypes.string,
  toggleShowEditLicensesPopover: PropTypes.func.isRequired,
  identificationSource: PropTypes.string,

  reviewObligationsClickHandler: PropTypes.func,
  getReviewObligationsHref: PropTypes.func,
  isAdvancedLegalPackSupported: PropTypes.bool,
};
