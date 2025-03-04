/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { faPen } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import { componentPropType, licenseLegalMetadataPropType } from './advancedLegalPropTypes';
import { findSingleLicenseIndex, findSimilarLicenseIndex, formatLicenseMeta } from './legalUtility';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxTag,
  NxTextLink,
  NxAccordion,
  useToggle,
} from '@sonatype/react-shared-components';
import { statusTagPropsMap } from './advancedLegalConstants';
import EditLicensesPopoverContainer from 'MainRoot/legal/license/EditLicensesPopover/EditLicensesPopoverContainer';

export default function LicenseDetailsTile(props) {
  const {
    component,
    licenseLegalMetadata,
    showLicensesModal,
    setShowLicensesModal,
    ownerType,
    ownerId,
    hash,
    scanId,
    componentIdentifier,
    stageTypeId,
    $state,
  } = props;
  const effectiveLicenses = formatLicenseMeta('effectiveLicenses', component, licenseLegalMetadata);
  const declaredLicenses = formatLicenseMeta('declaredLicenses', component, licenseLegalMetadata);
  const observedLicenses = formatLicenseMeta('observedLicenses', component, licenseLegalMetadata);

  const isLicensePresent = (licenses) => licenses.length > 0;
  const licenseDetailsTargetState = () => {
    if (stageTypeId) {
      return 'legal.stageTypeComponentLicenseDetails';
    }
    if (componentIdentifier && hash && scanId) {
      return 'legal.componentLicenseDetailsByComponentIdentifierAndHashAndScanId';
    }
    return hash ? 'legal.componentLicenseDetails' : 'legal.componentLicenseDetailsByComponentIdentifier';
  };

  const createItem = (license) => {
    if (license.isMulti) {
      const multipleLicenseNames = license.licenseName.split(' or ');
      const multipleLicenseNamesLength = multipleLicenseNames.length;
      return multipleLicenseNames.sort().map((licenseName, multiIndex) => {
        return (
          <Fragment key={multiIndex}>
            <NxTextLink
              href={$state.href(licenseDetailsTargetState(), {
                ownerType,
                ownerId,
                hash,
                scanId,
                componentIdentifier,
                stageTypeId,
                licenseIndex: findSimilarLicenseIndex(licenseName, licenseLegalMetadata),
              })}
            >
              {licenseName}
            </NxTextLink>
            {multipleLicenseNamesLength > multiIndex + 1 ? <span>{' or '}</span> : null}
          </Fragment>
        );
      });
    }
    return (
      <NxTextLink
        href={$state.href(licenseDetailsTargetState(), {
          ownerType,
          ownerId,
          hash,
          scanId,
          componentIdentifier,
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
        <Fragment key={index}>
          {createItem(item)}
          {licensesLength > index + 1 ? <span>{', '}</span> : null}
        </Fragment>
      ))
    ) : (
      <span>None found</span>
    );
  };

  const [open, toggleOpen] = useToggle(true);

  return (
    <Fragment>
      {showLicensesModal && <EditLicensesPopoverContainer />}
      <NxAccordion open={open} onToggle={toggleOpen} id="license-details-tile">
        <NxAccordion.Header>
          <NxAccordion.Title>Licenses</NxAccordion.Title>
          <div className="nx-btn-bar">
            <NxButton id="edit-licenses" variant="tertiary" onClick={() => setShowLicensesModal(true)}>
              <NxFontAwesomeIcon icon={faPen} />
              <span>Edit</span>
            </NxButton>
          </div>
        </NxAccordion.Header>
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
      </NxAccordion>
    </Fragment>
  );
}

LicenseDetailsTile.propTypes = {
  component: componentPropType,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  showLicensesModal: PropTypes.bool,
  setShowLicensesModal: PropTypes.func.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string,
  scanId: PropTypes.string,
  componentIdentifier: PropTypes.string,
  stageTypeId: PropTypes.string,
  $state: PropTypes.object.isRequired,
};
