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
  reviewCompletedCount: PropTypes.number.isRequired,
  reviewTotalCount: PropTypes.number.isRequired
});

export const componentPropType = PropTypes.shape({
  displayName: PropTypes.string.isRequired,
  licenseLegalData: PropTypes.shape({
    effectiveLicenses: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    copyrights: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    noticeFiles: PropTypes.arrayOf(PropTypes.shape({
      content: PropTypes.string.isRequired,
      relPath: PropTypes.string.isRequired
    }).isRequired).isRequired,
    licenseFiles: PropTypes.arrayOf(PropTypes.shape({
      content: PropTypes.string.isRequired,
      relPath: PropTypes.string.isRequired
    }).isRequired).isRequired
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

export const licenseObligationPropTypes = PropTypes.arrayOf(PropTypes.shape({
  name: PropTypes.string.isRequired,
  licenses: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    texts: PropTypes.arrayOf(PropTypes.string).isRequired
  }))
}).isRequired);
