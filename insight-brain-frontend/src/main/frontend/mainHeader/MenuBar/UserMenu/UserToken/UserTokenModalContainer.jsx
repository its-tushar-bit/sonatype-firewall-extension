/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import UserTokenModal from './UserTokenModal';
import * as userTokenActions from './userTokenActions';

const mapDispatchToProps = { ...userTokenActions };

const mapStateToProps = ({ userToken }) => ({ ...userToken });

const UserTokenModalContainer = connect(mapStateToProps, mapDispatchToProps)(UserTokenModal);
export default UserTokenModalContainer;
