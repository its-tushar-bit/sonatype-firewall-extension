/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';
import LoadWrapper from '../react/LoadWrapper';

export const applicationPropType = PropTypes.shape({
  applicationId: PropTypes.string.isRequired,
  applicationPublicId: PropTypes.string.isRequired,
  applicationName: PropTypes.string.isRequired,
  lastScanTime: PropTypes.number.isRequired,
  stageTypeId: PropTypes.string.isRequired,
  applicationTagNames: PropTypes.arrayOf(PropTypes.string).isRequired,
  componentsReviewedCount: PropTypes.number.isRequired,
  componentsTotalCount: PropTypes.number.isRequired,
});

export const applicationsTabPropType = PropTypes.shape({
  results: PropTypes.arrayOf(applicationPropType).isRequired,
  totalResultsCount: PropTypes.number.isRequired,
  backendPage: PropTypes.number.isRequired,
  error: LoadWrapper.propTypes.error,
  loading: PropTypes.bool,
  sortField: PropTypes.oneOf([
    'APPLICATION_NAME_ASC',
    'APPLICATION_NAME_DESC',
    'LAST_SCAN_TIME_ASC',
    'LAST_SCAN_TIME_DESC',
    'TAG_NAMES_ASC',
    'TAG_NAMES_DESC',
    null,
  ]),
});

export const legalFilePropType = PropTypes.shape({
  id: PropTypes.string,
  content: PropTypes.string.isRequired,
  relPath: PropTypes.string,
  originalContentHash: PropTypes.string,
});

export const legalFilesPropType = PropTypes.arrayOf(legalFilePropType);

export const licenseObligationsPropType = PropTypes.arrayOf(
  PropTypes.shape({
    name: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    comment: PropTypes.string,
    lastUpdatedByUsername: PropTypes.string,
    lastUpdatedAt: PropTypes.number,
  })
);

export const licenseObligationAttributionsPropType = PropTypes.arrayOf(
  PropTypes.shape({
    id: PropTypes.string,
    content: PropTypes.string,
    obligationName: PropTypes.string,
    lastUpdatedByUsername: PropTypes.string,
    lastUpdatedAt: PropTypes.number,
  })
);

export const copyrightPropType = PropTypes.shape({
  id: PropTypes.string,
  content: PropTypes.string.isRequired,
  originalContentHash: PropTypes.string,
  status: PropTypes.string.isRequired,
});

export const licenseLegalDataPropType = PropTypes.shape({
  effectiveLicenses: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  effectiveLicenseStatus: PropTypes.string.isRequired,
  copyrights: PropTypes.arrayOf(copyrightPropType.isRequired).isRequired,
  highestEffectiveLicenseThreatGroup: PropTypes.shape({
    licenseThreatGroupCategory: PropTypes.string.isRequired,
    licenseThreatGroupLevel: PropTypes.number.isRequired,
    licenseThreatGroupName: PropTypes.string.isRequired,
  }),
  noticeFiles: legalFilesPropType,
  licenseFiles: legalFilesPropType,
  componentCopyrightId: PropTypes.string,
  componentCopyrightScopeOwnerId: PropTypes.string,
  componentCopyrightLastUpdatedByUsername: PropTypes.string,
  componentCopyrightLastUpdatedAt: PropTypes.number,
  componentNoticesId: PropTypes.string,
  componentNoticesScopeOwnerId: PropTypes.string,
  componentNoticesLastUpdatedByUsername: PropTypes.string,
  componentNoticesLastUpdatedAt: PropTypes.number,
  componentLicensesId: PropTypes.string,
  componentLicensesScopeOwnerId: PropTypes.string,
  componentLicensesLastUpdatedByUsername: PropTypes.string,
  componentLicensesLastUpdatedAt: PropTypes.number,
  obligations: licenseObligationsPropType,
  attributions: licenseObligationAttributionsPropType,
});

export const componentPropType = PropTypes.shape({
  displayName: PropTypes.string.isRequired,
  licenseLegalData: licenseLegalDataPropType,
  stageScans: PropTypes.arrayOf(
    PropTypes.shape({
      stageName: PropTypes.string.isRequired,
      scanId: PropTypes.string,
      scanDate: PropTypes.number,
    })
  ),
});

export const licenseLegalMetadataPropType = PropTypes.arrayOf(
  PropTypes.shape({
    licenseId: PropTypes.string.isRequired,
    licenseName: PropTypes.string.isRequired,
    singleLicenseIds: PropTypes.arrayOf(PropTypes.string),
    threatGroup: PropTypes.shape({
      threatLevel: PropTypes.number,
    }),
    obligations: PropTypes.arrayOf(
      PropTypes.shape({
        licenseObligation: PropTypes.shape({
          name: PropTypes.string.isRequired,
          obligationTexts: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
        }),
      })
    ),
  }).isRequired
);

export const licenseObligationLicensesPropTypes = PropTypes.arrayOf(
  PropTypes.shape({
    name: PropTypes.string.isRequired,
    texts: PropTypes.arrayOf(PropTypes.string).isRequired,
  })
);

export const licenseObligationPropType = PropTypes.shape({
  name: PropTypes.string.isRequired,
  licenses: licenseObligationLicensesPropTypes,
  status: PropTypes.string.isRequired,
  comment: PropTypes.string,
});

export const licenseObligationsPropTypes = PropTypes.arrayOf(licenseObligationPropType.isRequired);

export const scopePropType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
}).isRequired;

export const availableScopesPropType = PropTypes.shape({
  loading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  values: PropTypes.arrayOf(scopePropType),
}).isRequired;

export const componentCopyrightDetailsPropType = PropTypes.shape({
  loading: PropTypes.bool,
  selectedCopyright: copyrightPropType,
  filePathsPage: PropTypes.number.isRequired,
  filePaths: PropTypes.arrayOf(
    PropTypes.shape({
      filePath: PropTypes.string.isRequired,
      copyrightMatches: PropTypes.number.isRequired,
    })
  ),
  totalFileMatches: PropTypes.number.isRequired,
  selectedFilePath: PropTypes.string,
  copyrightFileCounts: PropTypes.objectOf(PropTypes.number),
  copyrightContexts: PropTypes.arrayOf(
    PropTypes.shape({
      filePath: PropTypes.string,
      contexts: PropTypes.arrayOf(PropTypes.string),
    })
  ),
});

export const componentNoticeDetailsPropType = PropTypes.shape({
  loading: PropTypes.bool,
  selectedNotice: legalFilePropType,
});

export const componentLicenseDetailsPropType = PropTypes.shape({
  loading: PropTypes.bool,
  licenseIndex: PropTypes.number.isRequired,
  selectedLicense: legalFilePropType.isRequired,
});
