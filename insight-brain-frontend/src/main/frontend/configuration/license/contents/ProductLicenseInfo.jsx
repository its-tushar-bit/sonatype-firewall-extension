/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { identity } from 'ramda';
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';

import { formatDate } from '../../../util/dateUtils';

export const EXPIRATION_DATE_FORMAT = 'MMMM DD, YYYY';
const mkLimit = (name, count) => ({ name, count });

export default function ProductLicenseInfo({ license }) {
  const userLimits = [
    license.licensedUsersToDisplay && mkLimit('Lifecycle', license.licensedUsersToDisplay),
    license.licensedUsersToDisplay && mkLimit('Lifecycle Cloud', license.licensedUsersToDisplay),
    license.firewallUsersToDisplay && mkLimit('Firewall', license.firewallUsersToDisplay),
  ].filter(identity);

  const displayUserLimits = userLimits.length > 0;
  const displayApplicationLimit = license.applicationLimitToDisplay !== null;
  const displaySbomLimit = license.sbomLimitToDisplay !== null;

  return (
    <div className="nx-grid-row">
      <div className="nx-grid-col nx-grid-col--33">
        <dl className="nx-read-only">
          <dt className="nx-read-only__label">Company</dt>
          <dd className="nx-read-only__data" id="license-contact-company">
            {license.contactCompany}
          </dd>
          <dt className="nx-read-only__label">Primary Contact Name</dt>
          <dd className="nx-read-only__data" id="license-contact-name">
            {license.contactName}
          </dd>
          <dt className="nx-read-only__label">Primary Contact Email</dt>
          <dd className="nx-read-only__data" id="license-contact-email">
            {license.contactEmail}
          </dd>
        </dl>
      </div>
      <div className="nx-grid-col nx-grid-col--66">
        <dl className="nx-read-only nx-read-only--grid iq-product-license__specifications">
          <div className="nx-read-only__item iq-product-license__expiration-date">
            <dt className="nx-read-only__label">Expiration Date</dt>
            <dd className="nx-read-only__data visual-testing-ignore" id="license-expiry-date">
              {formatDate(license.expiryTimestamp, EXPIRATION_DATE_FORMAT)}
            </dd>
          </div>
          <div className="nx-read-only__item iq-product-license__license-types">
            <dt className="nx-read-only__label">License Type(s)</dt>
            {(license.products || []).map((product, index) => (
              <dd className="nx-read-only__data license-product" key={index}>
                {product}
              </dd>
            ))}
          </div>
          <div className="nx-read-only__item iq-product-license__days-remaining">
            <dt className="nx-read-only__label">Days Remaining</dt>
            <dd className="nx-read-only__data visual-testing-ignore" id="license-days-to-expiration">
              {license.daysToExpiration}
            </dd>
          </div>
          <div
            className="nx-read-only__item iq-product-license__licensed-developers"
            id={userLimits.length > 1 ? 'license-licensed-developers' : ''}
          >
            {displayUserLimits && (
              <Fragment>
                <dt className="nx-read-only__label">Licensed Developers</dt>
                {userLimits.length === 1 ? (
                  <dd className="nx-read-only__data">
                    <span id="license-licensed-developers">{userLimits[0].count}</span>
                  </dd>
                ) : (
                  userLimits.map(({ name, count }) => (
                    <dd className="nx-read-only__data" key={name + '___' + count}>
                      {name} — {count}
                    </dd>
                  ))
                )}
              </Fragment>
            )}
          </div>
          <div className="nx-read-only__item iq-product-license__fingerprint">
            <dt className="nx-read-only__label">Fingerprint</dt>
            <dd className="nx-read-only__data" id="license-fingerprint">
              {license.fingerprint}
            </dd>
          </div>
          {displayApplicationLimit && (
            <div className="nx-read-only__item iq-product-license__licensed-applications">
              <dt className="nx-read-only__label">Licensed Applications</dt>
              <dd className="nx-read-only__data" id="license-application-limit">
                {`${license.applicationLimitToDisplay} (${license.applicationCountToDisplay} in use)`}
              </dd>
            </div>
          )}
          {displaySbomLimit && (
            <div className="nx-read-only__item iq-product-license__licensed-sboms">
              <dt className="nx-read-only__label">Licensed SBOMs</dt>
              <dd className="nx-read-only__data" id="license-sbom-limit">
                {license.sbomLimitToDisplay}{' '}
                {license.sbomCountToDisplay ? `(${license.sbomCountToDisplay} in use)` : ''}
              </dd>
            </div>
          )}
        </dl>
      </div>
    </div>
  );
}

ProductLicenseInfo.propTypes = {
  license: PropTypes.shape({
    applicationCountToDisplay: PropTypes.number,
    applicationLimitToDisplay: PropTypes.number,
    contactCompany: PropTypes.string.isRequired,
    contactEmail: PropTypes.string.isRequired,
    contactName: PropTypes.string.isRequired,
    daysToExpiration: PropTypes.number.isRequired,
    expiryTimestamp: PropTypes.number.isRequired,
    fingerprint: PropTypes.string.isRequired,
    firewallUsersToDisplay: PropTypes.number,
    licensedUsersToDisplay: PropTypes.number,
    productEdition: PropTypes.string.isRequired,
    products: PropTypes.array,
    sbomCountToDisplay: PropTypes.number,
    sbomLimitToDisplay: PropTypes.number,
  }),
};
