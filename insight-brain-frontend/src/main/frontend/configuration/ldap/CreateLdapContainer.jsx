/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { prop } from 'ramda';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { actions } from '../ldap/ldapConfigSlice';
import CreateLdap from './CreateLdap';

export default connect(prop('ldapConfig'), { ...actions, stateGo })(CreateLdap);
