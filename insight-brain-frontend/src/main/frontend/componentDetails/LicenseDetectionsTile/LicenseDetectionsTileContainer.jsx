/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import LicenseDetectionsTile from './LicenseDetectionsTile';

function mapStateToProps() {
  return {};
}

const mapDispatchToProps = {};

export const LicenseDetectionsTileContainer = connect(mapStateToProps, mapDispatchToProps)(LicenseDetectionsTile);
