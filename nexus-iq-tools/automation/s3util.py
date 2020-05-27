#!/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import boto3
import botocore
import logging


log = logging.getLogger(__name__)


def role_arn_to_session(**args):
    """
    Usage :
        session = role_arn_to_session(
            RoleArn='arn:aws:iam::012345678901:role/example-role',
            RoleSessionName='ExampleSessionName')
        client = session.client('sqs')
    """
    client = boto3.client('sts')
    response = client.assume_role(**args)
    return boto3.Session(
        aws_access_key_id=response['Credentials']['AccessKeyId'],
        aws_secret_access_key=response['Credentials']['SecretAccessKey'],
        aws_session_token=response['Credentials']['SessionToken'])


def s3download(bucket, target_key, localfile):
    try:
        bucket.download_file(target_key, localfile)
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == "404":
            log.error("The object does not exist.")
        else:
            raise


def list_s3(target_bucket, role_arn=None):
    if role_arn:
        session = role_arn_to_session(
            RoleArn=role_arn,
            RoleSessionName='xIqDatasetSessionS3')
    else:
        session = boto3.Session()

    s3 = session.resource('s3')
    bucket = s3.Bucket(target_bucket)
    keys = []
    for found_obj in bucket.objects.all():
        keys.append(found_obj.key)

    return bucket, keys


def download_database_data(bucket_name, dataset_size, destination_file_path, is_postgres=False):
    s3 = boto3.resource("s3")
    bucket = s3.Bucket(bucket_name)
    if is_postgres:
        prefix = "testing/" + dataset_size + "/postgres"
    else:
        prefix = "testing/" + dataset_size + "/h2"
    last_s3_object = None
    for s3_object in bucket.objects.filter(Prefix=prefix):
        last_s3_object = s3_object
    bucket.download_file(last_s3_object.key, destination_file_path)


def main():
    bucket, keys = list_s3('iq-datasets')
    log.info(keys)


if __name__ == "__main__":
    main()
