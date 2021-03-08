/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';

export const applicationPropType = PropTypes.shape({
  applicationId: PropTypes.string.isRequired,
  applicationName: PropTypes.string.isRequired,
  lastScanTime: PropTypes.number.isRequired,
  applicationTagNames: PropTypes.arrayOf(PropTypes.string).isRequired,
  componentsReviewedCount: PropTypes.number.isRequired,
  componentsTotalCount: PropTypes.number.isRequired
});

export const componentPropType = PropTypes.shape({
  displayName: PropTypes.string.isRequired,
  licenseLegalData: PropTypes.shape({
    effectiveLicenses: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    copyrights: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.string,
      content: PropTypes.string.isRequired,
      originalContentHash: PropTypes.string,
      status: PropTypes.string.isRequired
    }).isRequired).isRequired,
    noticeFiles: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.string,
      content: PropTypes.string.isRequired,
      relPath: PropTypes.string.isRequired,
      originalContentHash: PropTypes.string
    }).isRequired).isRequired,
    licenseFiles: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.string,
      content: PropTypes.string.isRequired,
      relPath: PropTypes.string.isRequired,
      originalContentHash: PropTypes.string
    }).isRequired).isRequired,
    componentCopyrightId: PropTypes.string,
    componentCopyrightScopeOwnerId: PropTypes.string
  }).isRequired
});

export const licenseLegalMetadataPropType = PropTypes.arrayOf(PropTypes.shape({
  licenseId: PropTypes.string.isRequired,
  licenseName: PropTypes.string.isRequired,
  obligations: PropTypes.arrayOf(PropTypes.shape({
    licenseObligation: PropTypes.shape({
      name: PropTypes.string.isRequired,
      obligationTexts: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired
    })
  }))
}).isRequired);

export const licenseObligationsPropType = PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      status: PropTypes.string.isRequired,
      comment: PropTypes.string,
      attributions: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string,
        content: PropTypes.string
      }).isRequired).isRequired
    })
);

export const licenseObligationLicensesPropTypes = PropTypes.arrayOf(PropTypes.shape({
  name: PropTypes.string.isRequired,
  texts: PropTypes.arrayOf(PropTypes.string).isRequired
}));

export const licenseObligationPropType = PropTypes.shape({
  name: PropTypes.string.isRequired,
  licenses: licenseObligationLicensesPropTypes,
  status: PropTypes.string.isRequired,
  comment: PropTypes.string,
  attributions: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string,
    content: PropTypes.string
  }).isRequired).isRequired
});

export const licenseObligationsPropTypes = PropTypes.arrayOf(licenseObligationPropType.isRequired);

export const scopePropType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired
}).isRequired;

export const availableScopesPropType = PropTypes.shape({
  loading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  values: PropTypes.arrayOf(scopePropType)
}).isRequired;
