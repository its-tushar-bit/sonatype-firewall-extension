/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { findIndex, propEq, slice } from 'ramda';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faGlobe, faSitemap, faTerminal, faCube, faCaretSquareRight } from '@fortawesome/free-solid-svg-icons';
import React from 'react';
import { isNilOrEmpty } from '../util/jsUtil';
import { NO_LICENSE_THREAT_GROUP_ASSIGNED } from './advancedLegalConstants';

export function isScopeOverride(originalOwnerId, ownerId, availableScopeValues) {
  const originalOwnerLevel = findIndex(propEq('id', originalOwnerId), availableScopeValues);
  const newOwnerLevel = findIndex(propEq('id', ownerId), availableScopeValues);
  return originalOwnerLevel > newOwnerLevel;
}

export function createSubtitle(availableScopes, component) {
  let availableScopeValuesReversed =
    (availableScopes && availableScopes.values && slice(0, 2, availableScopes.values)) || [];
  availableScopeValuesReversed.reverse();
  if (availableScopeValuesReversed.length > 1) {
    availableScopeValuesReversed = availableScopeValuesReversed.filter((obj) => obj.id !== 'ROOT_ORGANIZATION_ID');
  }
  if (component) {
    availableScopeValuesReversed.push({
      type: 'component',
      id: 'component',
      name: component.displayName,
    });
  }
  return (
    <div className="nx-page-title__description">
      {availableScopeValuesReversed.map((availableScope, index) => {
        return (
          <span key={index} className="iq-violation-details__subtitle-part">
            <NxFontAwesomeIcon icon={setScopeIcon(availableScope)} />
            <span>{availableScope.name}</span>
          </span>
        );
      })}
    </div>
  );
}

export function setScopeIcon(availableScope) {
  if (availableScope.id === 'ROOT_ORGANIZATION_ID') {
    return faGlobe;
  }
  switch (availableScope.type) {
    case 'organization':
      return faSitemap;
    case 'application':
      return faTerminal;
    case 'component':
      return faCube;
  }
  return faCaretSquareRight;
}

export function backToComponentOverviewUrl(
  $state,
  ownerType,
  ownerId,
  stageTypeId,
  hash,
  componentIdentifier,
  scanId = null,
  isSbomManager
) {
  let state = '';
  if (componentIdentifier && !hash) {
    state = 'componentOverviewByComponentIdentifier';
  } else {
    state = ownerType === 'organization' ? 'organizationComponentOverview' : 'applicationComponentOverview';
  }
  const params = {
    [ownerType === 'organization' ? 'organizationId' : 'applicationPublicId']: ownerId,
    ...(hash && { hash }),
    ...(componentIdentifier && { componentIdentifier }),
  };
  if (stageTypeId && ownerType === 'application') {
    state = 'applicationStageTypeComponentOverview';
    params.stageTypeId = stageTypeId;
  }
  if (componentIdentifier && hash && scanId) {
    state = 'applicationComponentOverviewByComponentIdentifier';
    params.scanId = scanId;
    params.tabId = 'legal';
  }

  state = `${isSbomManager ? LEGAL_SBOM_MANAGER_PARENT_ROUTE : LEGAL_PARENT_ROUTE}.${state}`;

  return $state.href($state.get(state), params);
}

export function getLicenseThreatGroupsFromLicense(license) {
  return isNilOrEmpty(license.licenseThreatGroups)
    ? [{ licenseThreatGroupName: NO_LICENSE_THREAT_GROUP_ASSIGNED }]
    : license.licenseThreatGroups;
}

export function getRelevantScope(scopeOwnerId, availableScopes) {
  const rootScope = {
    id: 'ROOT_ORGANIZATION_ID',
    label: 'Organization',
    name: 'Root Organization',
    publicId: 'ROOT_ORGANIZATION_ID',
    type: 'organization',
  };
  const availableScopeValues = (availableScopes && availableScopes.values && [...availableScopes.values]) || [];
  const scopeIndex = findIndex(propEq('id', scopeOwnerId), availableScopeValues);
  return scopeIndex < 0 ? rootScope : availableScopeValues[scopeIndex];
}

export function ifExistsElseEmpty(element, func) {
  return element ? func() : '';
}

export function attributionStatus(item) {
  return ifExistsElseEmpty(item, () => (item.status === 'enabled' ? 'Included' : 'Excluded'));
}
export function legalSource(item) {
  return ifExistsElseEmpty(item, () => (item.originalContentHash ? 'Sonatype Scan' : 'Manually added'));
}

export function createScopeOption(value) {
  return (
    <option key={value.id} value={value.id}>
      {value.label} - {value.name}
    </option>
  );
}

/**
 * Find the index of the single license in licenseMetadata.
 * If user clicked on a multi-license in the list we should select the first license in the multi.
 */
export function findSingleLicenseIndex(licenseId, licenseLegalMetadata) {
  const corrected = licenseLegalMetadata.findIndex((license) => !license.isMulti && license.licenseId === licenseId);
  if (corrected !== -1) {
    return corrected;
  }

  // Must be a multilicense
  return licenseLegalMetadata.findIndex((license) => !license.isMulti && licenseId.startsWith(license.licenseId));
}

/**
 * Find the index of the most similar single license in licenseMetadata.
 */
export function findSimilarLicenseIndex(licenseId, licenseLegalMetadata) {
  // licenseId === 'Apache 2.0+'
  // searching for 'Apache 2.0'
  let index = licenseLegalMetadata.findIndex((license) => license.licenseId.startsWith(licenseId.split('+')[0]));
  if (index === -1) {
    const matched = licenseId.match(/([A-Z\sa-z]+[A-Za-z])[^\d]*[\d]+\.?/);
    if (!matched) return 0;
    const [licenseNameWithMajorVersion, licenseNameWithoutMajorVersion] = matched;
    // searching for 'Apache 2.'
    index = licenseLegalMetadata.findIndex((license) => license.licenseId.startsWith(licenseNameWithMajorVersion));
    if (index === -1) {
      // searching for 'Apache'
      index = licenseLegalMetadata.findIndex((license) => license.licenseId.startsWith(licenseNameWithoutMajorVersion));
      // if nothing matches, the id will be 0, the first license in licenseLegalMetadata
      return index === -1 ? 0 : index;
    } else {
      return index;
    }
  } else {
    return index;
  }
}

/**
 * Given a license type name, a component and the licenseLegalMetadata, returns an array of the license name, id, isMulti flag, and an array of single licenses ids from the component's
 * effective license IDs. The resulting collection has an ascending order by licenseName property.
 *  example:
 * [{
 *    licenseId: id,
 *    licenseName: name
 *    isMulti: true,
      singleLicenseIds: ['license1', 'license2'],
 *  }]
*/
export function formatLicenseMeta(licenseType, component, licenseLegalMetadata) {
  if (component) {
    const licenses = component.licenseLegalData[licenseType];
    return licenseLegalMetadata
      .reduce((accumulated, l) => {
        if (licenses.includes(l.licenseId)) {
          return [
            ...accumulated,
            {
              licenseId: l.licenseId,
              licenseName: l.licenseName,
              isMulti: l.isMulti,
              singleLicenseIds: l.singleLicenseIds,
            },
          ];
        }
        return accumulated;
      }, [])
      .sort((a, b) => a.licenseName.localeCompare(b.licenseName));
  }
  return [];
}

const statusMap = new Map([
  ['OPEN', 'Open'],
  ['SELECTED', 'Selected'],
  ['OVERRIDDEN', 'Overridden'],
  ['ACKNOWLEDGED', 'Acknowledged'],
  ['CONFIRMED', 'Confirmed'],
]);

export const getStatusName = (id) => {
  if (statusMap.has(id)) {
    return statusMap.get(id);
  }
  //you send me junk, i send you junk back ;)
  return id;
};

export const LEGAL_SBOM_MANAGER_PARENT_ROUTE = 'sbomManager.legal';
export const LEGAL_PARENT_ROUTE = 'legal';

export function createLegalRoutes($stateProvider, parentRoute = LEGAL_PARENT_ROUTE) {
  const prefixed = (path) => `${parentRoute}${path}`;

  $stateProvider
    .state(parentRoute, {
      abstract: true,
    })
    .state(prefixed('.dashboard'), {
      url: '/legal/dashboard',
      component: 'legalDashboard',
      data: {
        title: 'Legal Dashboard',
        activeTab: 'applications',
      },
    })
    .state(prefixed('.applicationsDashboard'), {
      url: '/legal/applicationsDashboard',
      component: 'legalDashboard',
      data: {
        title: 'Legal Dashboard',
        activeTab: 'applications',
        disableCreateAttributionReportBtn: false,
      },
    })
    .state(prefixed('.componentsDashboard'), {
      url: '/legal/componentsDashboard',
      component: 'legalDashboard',
      data: {
        title: 'Legal Dashboard',
        activeTab: 'components',
        disableCreateAttributionReportBtn: true,
      },
    })
    .state(prefixed('.componentOverview'), {
      url: '/legal/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state(prefixed('.componentOverviewByComponentIdentifier'), {
      url: '/legal/component/componentIdentifier/{componentIdentifier}/repository/{repositoryId}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state(prefixed('.applicationComponentOverviewByComponentIdentifier'), {
      url:
        '/legal/component/componentIdentifier/{componentIdentifier}/application/{applicationPublicId}' +
        '/component/{hash}/scan/{scanId}/{tabId}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state(prefixed('.noticeFilesByComponentIdentifier'), {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/notices',
      component: 'componentNoticeDetails',
      abstract: true,
    })
    .state(prefixed('.noticeFilesByComponentIdentifier.noticeDetails'), {
      url: '/{noticeIndex}',
      component: 'noticeDetailsContents',
      data: {
        title: 'Notice Details',
      },
    })
    .state(prefixed('.organizationComponentOverview'), {
      url: '/legal/organization/{organizationId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state(prefixed('.applicationComponentOverview'), {
      url: '/legal/application/{applicationPublicId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state(prefixed('.applicationStageTypeComponentOverview'), {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state(prefixed('.applicationDetails'), {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}',
      component: 'legalApplicationDetails',
      data: {
        title: 'Application Details',
      },
    })
    .state(prefixed('.attributionReport'), {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReport',
      component: 'attributionReportForm',
      data: {
        title: 'Attribution Report',
        isDirty: ['attributionReports', 'attributionReports', 'isFormDirty'],
      },
    })
    .state(prefixed('.attributionReportMultiApp'), {
      url: '/legal/application/attributionReport',
      component: 'attributionReportForm',
      data: {
        title: 'Attribution Report (Multiple Applications)',
        isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
        isMultiApp: true,
      },
    })
    .state(prefixed('.attributionReportTemplate'), {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReportTemplate',
      component: 'attributionReportTemplateForm',
      data: {
        title: 'Attribution Report Templates',
        isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
      },
    })
    .state(prefixed('.attributionReportTemplateMultiApp'), {
      url: '/legal/application/attributionReportTemplate',
      component: 'attributionReportTemplateForm',
      data: {
        title: 'Attribution Report Templates',
        isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
        isMultiApp: true,
      },
    })
    .state(prefixed('.componentCopyrightDetails'), {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/copyrights',
      component: 'componentCopyrightDetails',
      abstract: true,
    })
    .state(prefixed('.componentCopyrightDetails.copyrightDetails'), {
      url: '/{copyrightIndex}',
      component: 'copyrightDetailsContents',
      data: {
        title: 'Copyright Details',
      },
    })
    .state(prefixed('.componentCopyrightDetailsByComponentIdentifier'), {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/copyrights',
      component: 'componentCopyrightDetails',
      abstract: true,
    })
    .state(prefixed('.componentCopyrightDetailsByComponentIdentifier.copyrightDetails'), {
      url: '/{copyrightIndex}',
      component: 'copyrightDetailsContents',
      data: {
        title: 'Copyright Details',
      },
    })
    .state(prefixed('.stageTypeComponentCopyrightDetails'), {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/copyrights',
      component: 'componentCopyrightDetails',
      abstract: true,
    })
    .state(prefixed('.stageTypeComponentCopyrightDetails.copyrightDetails'), {
      url: '/{copyrightIndex}',
      component: 'copyrightDetailsContents',
      data: {
        title: 'Copyright Details',
      },
    })
    .state(prefixed('.componentNoticeDetails'), {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/notices',
      component: 'componentNoticeDetails',
      abstract: true,
    })
    .state(prefixed('.componentNoticeDetails.noticeDetails'), {
      url: '/{noticeIndex}',
      component: 'noticeDetailsContents',
      data: {
        title: 'Notice Details',
      },
    })
    .state(prefixed('.stageTypeComponentNoticeDetails'), {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/notices',
      component: 'componentNoticeDetails',
      abstract: true,
    })
    .state(prefixed('.stageTypeComponentNoticeDetails.noticeDetails'), {
      url: '/{noticeIndex}',
      component: 'noticeDetailsContents',
      data: {
        title: 'Notice Details',
      },
    })
    .state(prefixed('.componentLicenseFilesDetails'), {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenseFiles',
      component: 'componentLicenseFilesDetails',
      abstract: true,
    })
    .state(prefixed('.componentLicenseFilesDetails.licenseFilesDetails'), {
      url: '/{licenseIndex}',
      component: 'licenseFilesDetailsContents',
      data: {
        title: 'License Files Details',
      },
    })
    .state(prefixed('.componentLicenseFilesDetailsByComponentIdentifier'), {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenseFiles',
      component: 'componentLicenseFilesDetails',
      abstract: true,
    })
    .state(prefixed('.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails'), {
      url: '/{licenseIndex}',
      component: 'licenseFilesDetailsContents',
      data: {
        title: 'License Files Details',
      },
    })
    .state(prefixed('.stageTypeComponentLicenseFilesDetails'), {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenseFiles',
      component: 'componentLicenseFilesDetails',
      abstract: true,
    })
    .state(prefixed('.stageTypeComponentLicenseFilesDetails.licenseFilesDetails'), {
      url: '/{licenseIndex}',
      component: 'licenseFilesDetailsContents',
      data: {
        title: 'License Files Details',
      },
    })
    .state(prefixed('.componentLicenseDetails'), {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    })
    .state(prefixed('.componentLicenseDetailsByComponentIdentifier'), {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    })
    .state(prefixed('.componentLicenseDetailsByComponentIdentifierAndHashAndScanId'), {
      url:
        '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/component/{hash}/scan/{scanId}' +
        '/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    })
    .state(prefixed('.stageTypeComponentLicenseDetails'), {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    });
}
