/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import { licenseLegalMetadataPropType } from '../advancedLegalPropTypes';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { NxThreatIndicator } from '@sonatype/react-shared-components';
import { isMultiLicense } from './componentLicenseDetailsActions';
import { LEGAL_PARENT_ROUTE, LEGAL_SBOM_MANAGER_PARENT_ROUTE } from 'MainRoot/legal/legalUtility';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function LicenseList(props) {
  const { ownerType, ownerId, hash, stageTypeId, componentLicenseDetails, licenseLegalMetadata, isSbomManager } = props;

  const $state = useRouterState();

  const selectedLicense = parseInt(componentLicenseDetails.licenseIndex);

  const licenseDetailsTargetState = () => {
    const statePrefix = isSbomManager ? LEGAL_SBOM_MANAGER_PARENT_ROUTE : LEGAL_PARENT_ROUTE;
    if (stageTypeId) {
      return `${statePrefix}.stageTypeComponentLicenseDetails`;
    }
    return hash
      ? `${statePrefix}.componentLicenseDetails`
      : `${statePrefix}.componentLicenseDetailsByComponentIdentifier`;
  };

  const licenseRef = React.useRef(new Map());

  const licenseItem = (item, index) => (
    <li key={index} className="nx-list__item nx-list__item--link">
      <a
        href={$state.href(licenseDetailsTargetState(), { ownerType, ownerId, hash, licenseIndex: index })}
        className={listLinkClass(index)}
      >
        <span className="nx-list__text nx-truncate-ellipsis" ref={(element) => licenseRef.current.set(index, element)}>
          {item}
        </span>
        <span className="nx-list__subtext">{licenseThreat(item)}</span>
      </a>
    </li>
  );

  const licenseThreat = (licenseName) => {
    const license = licenseLegalMetadata.find((licenseMetadata) => licenseMetadata.licenseId === licenseName);
    if (!license || !license.threatGroup) {
      return <span />;
    }
    return (
      <Fragment>
        <NxThreatIndicator policyThreatLevel={license.threatGroup.threatLevel} />
        <span>{license.threatGroup.name}</span>
      </Fragment>
    );
  };

  const listLinkClass = (index) => classnames('nx-list__link', { selected: index === selectedLicense });

  function makeListItem(item, index) {
    if (isMultiLicense(licenseLegalMetadata, item)) {
      return null;
    } else {
      return licenseItem(item, index);
    }
  }

  const listItems = licenseLegalMetadata.map((l, i) => makeListItem(l.licenseId, i));

  useEffect(() => {
    if (selectedLicense && licenseRef.current && licenseRef.current.get(selectedLicense)) {
      licenseRef.current.get(selectedLicense).scrollIntoView({
        behavior: 'auto',
        block: 'center',
      });
    }
  }, [selectedLicense]);

  return (
    <aside id="license-list" className="nx-scrollable nx-viewport-sized__scrollable">
      <ul className="nx-list nx-list--clickable">{listItems}</ul>
    </aside>
  );
}

LicenseList.propTypes = {
  componentLicenseDetails: PropTypes.object,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  stageTypeId: PropTypes.string,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  isSbomManager: PropTypes.bool,
};
