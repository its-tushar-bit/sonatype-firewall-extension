/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { getDaysFromNow, getExpiryDate } from '../../../util/jsUtil';
import { getUserLimits } from '../gettingStartedUtils';
import GettingStartedDocLink from './GettingStartedDocLink';

export default function ProductLicenceSummary({ license, tenantMode }) {
  const userLimits = getUserLimits(license);
  const isSingleTenant = tenantMode !== 'multi-tenant';

  return (
    <section id="product-license-summary" className="nx-tile" aria-labelledby="product-license-title">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 id="product-license-title" className="nx-h2">
            Product License
          </h2>
        </div>
      </header>
      <div className="nx-tile-content">
        Read more about these in our{' '}
        <GettingStartedDocLink
          href="http://links.sonatype.com/products/nxiq/doc/licensing-and-features"
          text="feature matrix"
        />
      </div>

      <div className="nx-tile-content">
        <div className="nx-grid-row">
          <div className="nx-grid-col nx-grid-col--33">
            <div>
              <dl className="nx-read-only nx-read-only--grid">
                <div className="nx-read-only__item" id="license-products">
                  <dt className="nx-read-only__label">Licence Type</dt>
                  {license.products.map((product, i) => (
                    <dd className="nx-read-only__data" key={i}>
                      {product}
                    </dd>
                  ))}
                </div>
              </dl>
            </div>
          </div>

          <div className="nx-grid-col nx-grid-col--33">
            {userLimits && (
              <div>
                {userLimits.length > 0 && (
                  <>
                    <dt className="nx-read-only__label">Licensed Developers</dt>
                    <span className="nx-list__subtext">
                      {userLimits.length === 1 && <span id="license-licensed-developers">{userLimits[0].count}</span>}
                      {userLimits.length !== 1 && (
                        <dl className="iq-license-info__nested-definitions" id="license-licensed-developers">
                          {userLimits.map((limit, i) => (
                            <div key={i}>
                              <dt className="nx-read-only__label">{limit.name}</dt>
                              <dd className="nx-read-only__data">{limit.count}</dd>
                            </div>
                          ))}
                        </dl>
                      )}
                    </span>
                  </>
                )}
              </div>
            )}

            {license.applicationLimitToDisplay !== null && (
              <>
                <dt className="nx-read-only__label">Licensed Applications</dt>
                <dd className="nx-read-only__data" id="license-application-limit">
                  {license.applicationLimitToDisplay} ({license.applicationCountToDisplay} in use)
                </dd>
              </>
            )}

            {license.sbomLimitToDisplay !== null && (
              <>
                <dt className="nx-read-only__label">Licensed SBOMs</dt>
                <dd className="nx-read-only__data" id="license-sbom-limit">
                  {license.sbomLimitToDisplay}
                  {license.sbomCountToDisplay ? ` (${license.sbomCountToDisplay} in use)` : ''}
                </dd>
              </>
            )}

            {isSingleTenant && (
              <>
                <dt className="nx-read-only__label">Fingerprint</dt>
                <dd className="nx-read-only__data" id="license-fingerprint">
                  {license.fingerprint}
                </dd>
              </>
            )}
          </div>

          <div className="nx-grid-col nx-grid-col--33">
            <div>
              <dt className="nx-read-only__label">Expiration Date</dt>
              <dd className="nx-read-only__data visual-testing-ignore" id="license-expiry-date">
                {getExpiryDate(license.expiryTimestamp)}
              </dd>

              <dt className="nx-read-only__label">Days Remaining</dt>
              <dd className="nx-read-only__data visual-testing-ignore" id="license-days-to-expiration">
                {getDaysFromNow(license.expiryTimestamp)}
              </dd>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

ProductLicenceSummary.propTypes = {
  license: PropTypes.shape({
    applicationCountToDisplay: PropTypes.number,
    applicationLimitToDisplay: PropTypes.number,
    expiryTimestamp: PropTypes.number.isRequired,
    fingerprint: PropTypes.string.isRequired,
    products: PropTypes.array,
    sbomCountToDisplay: PropTypes.number,
    sbomLimitToDisplay: PropTypes.number,
  }),
  tenantMode: PropTypes.string,
};
