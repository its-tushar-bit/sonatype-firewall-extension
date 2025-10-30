/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userActions from './userActions';
import userReducer from './userReducer';

export default angular.module('userModule', []).factory('userActions', userActions).value('userReducer', userReducer);
