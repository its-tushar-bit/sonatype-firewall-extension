/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { componentPropType } from '../advancedLegalPropTypes';

export default function ComponentLicenseOverviewTile(props) {
  const { component } = props;

  const EMPTY = {
    declaredLicenses: [],
    observedLicenses: [],
    effectiveLicenses: [],
  };
  const licenseLegalData = component ? component.licenseLegalData : EMPTY;

  return (
    <section id="component-license-overview-tile" className="nx-tile">
      <div className="nx-tile-content nx-grid">
        <div className="nx-grid-row">
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Declared Licenses</dt>
              <dd id="component-license-overview__declared-licenses" className="nx-read-only__data">
                {licenseLegalData.declaredLicenses.join(', ')}
              </dd>
            </dl>
          </div>
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Observed Licenses</dt>
              <dd id="component-license-overview__observed-licenses" className="nx-read-only__data">
                {licenseLegalData.observedLicenses.join(', ')}
              </dd>
            </dl>
          </div>
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Effective Licenses</dt>
              <dd id="component-license-overview__effective-licenses" className="nx-read-only__data">
                {licenseLegalData.effectiveLicenses.join(', ')}
              </dd>
            </dl>
          </div>
        </div>
      </div>
    </section>
  );
}

ComponentLicenseOverviewTile.propTypes = {
  component: componentPropType,
};
