/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop } from 'ramda';
import { connect } from 'react-redux';
import ProductLicense from './ProductLicense';
import * as productLicenseActions from './productLicenseActions';
import { stateGo } from '../../reduxUiRouter/routerActions';

export default connect(prop('productLicense'), { ...productLicenseActions, stateGo })(ProductLicense);
