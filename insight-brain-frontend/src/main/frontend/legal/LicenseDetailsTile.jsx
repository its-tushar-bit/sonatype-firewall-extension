/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { componentPropType, licenseLegalMetadataPropType } from './advancedLegalPropTypes';
import { findSingleLicenseIndex, findSimilarLicenseIndex, formatLicenseMeta } from './legalUtility';
import { NxTag, NxTextLink, NxAccordion, useToggle } from '@sonatype/react-shared-components';
import { statusTagPropsMap } from './advancedLegalConstants';

export default function LicenseDetailsTile(props) {
  const { component, licenseLegalMetadata, ownerType, ownerId, hash, stageTypeId, $state } = props;
  const effectiveLicenses = formatLicenseMeta('effectiveLicenses', component, licenseLegalMetadata);
  const declaredLicenses = formatLicenseMeta('declaredLicenses', component, licenseLegalMetadata);
  const observedLicenses = formatLicenseMeta('observedLicenses', component, licenseLegalMetadata);

  const isLicensePresent = (licenses) => licenses.length > 0;
  const licenseDetailsTargetState = () =>
    stageTypeId ? 'legal.stageTypeComponentLicenseDetails' : 'legal.componentLicenseDetails';

  const createItem = (license) => {
    if (license.isMulti) {
      const multipleLicenseNames = license.licenseName.split(' or ');
      const multipleLicenseNamesLength = multipleLicenseNames.length;
      return multipleLicenseNames.sort().map((licenseName, multiIndex) => {
        return (
          <React.Fragment key={multiIndex}>
            <NxTextLink
              href={$state.href(licenseDetailsTargetState(), {
                ownerType,
                ownerId,
                hash,
                stageTypeId,
                licenseIndex: findSimilarLicenseIndex(licenseName, licenseLegalMetadata),
              })}
            >
              {licenseName}
            </NxTextLink>
            {multipleLicenseNamesLength > multiIndex + 1 ? <span>{' or '}</span> : null}
          </React.Fragment>
        );
      });
    }
    return (
      <NxTextLink
        href={$state.href(licenseDetailsTargetState(), {
          ownerType,
          ownerId,
          hash,
          stageTypeId,
          licenseIndex: findSingleLicenseIndex(license.licenseId, licenseLegalMetadata),
        })}
      >
        {license.licenseName}
      </NxTextLink>
    );
  };

  const getStatusTag = () => {
    const effectiveLicenseStatus = props.component.licenseLegalData.effectiveLicenseStatus;
    const tagColor = statusTagPropsMap[effectiveLicenseStatus];
    if (tagColor) {
      return <NxTag color={tagColor}>{effectiveLicenseStatus}</NxTag>;
    }
  };

  const formatLicenseList = (licensesList) => {
    const licensesLength = licensesList.length;
    return isLicensePresent(licensesList) ? (
      licensesList.map((item, index) => (
        <React.Fragment key={index}>
          {createItem(item)}
          {licensesLength > index + 1 ? <span>{', '}</span> : null}
        </React.Fragment>
      ))
    ) : (
      <span>None found</span>
    );
  };

  const [open, toggleOpen] = useToggle(true);

  return (
    <NxAccordion open={open} onToggle={toggleOpen} id="license-details-tile">
      <NxAccordion.Header>
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 className="nx-h2 nx-accordion__header-title">Licenses</h2>
          </div>
        </header>
      </NxAccordion.Header>
      <div className="nx-tile-content">
        <dl className="nx-read-only">
          <dt className="nx-read-only__label">Effective Licenses</dt>
          <dd className="nx-read-only__data license-details-tile__effective-licenses">
            {formatLicenseList(effectiveLicenses)}
            {getStatusTag()}
          </dd>
          <dt className="nx-read-only__label">Declared Licenses</dt>
          <dd className="nx-read-only__data license-details-tile__declared-licenses">
            {formatLicenseList(declaredLicenses)}
          </dd>
          <dt className="nx-read-only__label">Observed Licenses</dt>
          <dd className="nx-read-only__data license-details-tile__observed-licenses">
            {formatLicenseList(observedLicenses)}
          </dd>
        </dl>
      </div>
    </NxAccordion>
  );
}

LicenseDetailsTile.propTypes = {
  component: componentPropType,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string.isRequired,
  stageTypeId: PropTypes.string,
  $state: PropTypes.object.isRequired,
};
