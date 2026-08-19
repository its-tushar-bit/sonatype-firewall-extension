/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { componentPropType, licenseLegalMetadataPropType } from '../advancedLegalPropTypes';
import { formatLicenseMeta } from '../legalUtility';

export default function ComponentLicenseOverviewTile(props) {
  const { component, licenseLegalMetadata } = props;

  const EMPTY = {
    declaredLicenses: [],
    observedLicenses: [],
    effectiveLicenses: [],
  };
  const licenseLegalData = component ? component.licenseLegalData : EMPTY;
  const createLicensesList = (licenses) => licenses.map((license) => license.licenseName).join(', ');
  const effectiveLicenses = createLicensesList(formatLicenseMeta('effectiveLicenses', component, licenseLegalMetadata));
  const declaredLicenses = createLicensesList(formatLicenseMeta('declaredLicenses', component, licenseLegalMetadata));
  const observedLicenses = createLicensesList(formatLicenseMeta('observedLicenses', component, licenseLegalMetadata));

  return (
    <section id="component-license-overview-tile" className="nx-tile">
      <div className="nx-tile-content nx-grid">
        <div className="nx-grid-row">
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Declared Licenses</dt>
              <dd
                id="component-license-overview__declared-licenses"
                key="component-license-overview__declared-licenses"
                className="nx-read-only__data"
              >
                {declaredLicenses}
              </dd>
            </dl>
          </div>
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Observed Licenses</dt>
              <dd
                id="component-license-overview__observed-licenses"
                key="component-license-overview__observed-licenses"
                className="nx-read-only__data"
              >
                {observedLicenses}
              </dd>
            </dl>
          </div>
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Effective Licenses</dt>
              <dd
                id="component-license-overview__effective-licenses"
                key="component-license-overview__effective-licenses"
                className="nx-read-only__data"
              >
                {effectiveLicenses}
              </dd>
            </dl>
          </div>
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Status</dt>
              <dd
                id="component-license-overview__effective-license-status"
                key="component-license-overview__effective-license-status"
                className="nx-read-only__data"
              >
                {licenseLegalData.effectiveLicenseStatus}
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
  licenseLegalMetadata: licenseLegalMetadataPropType,
};
