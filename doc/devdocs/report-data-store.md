<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Report Datastore
The Report Datastore is an abstraction that allows different storage options for storing report data. Report data is the data returned from HDS, in the form of a file `report.zip`. The original implementation stored this file on the filesystem, and could even modify the zip by adding additional entries. The abstraction intends to hide these types of details so that it is up to the storage layer to determine how to store and retrieve the report data.

## File Report Datastore
The default report datastore uses the filesystem, at a predetermined path relative to the configured sonatype-work directory from the config.yaml. The report.zip is stored on the filesystem, sometimes additional third party JSON files are added as additional entries. As entries are read from the report.zip, they're stored in a directory report.cache that shares the same parent as the report.zip. Re-evaluations currently delete these "cached" files. 

NB: These aren't actually a cache, and not much is gained by extracting unmodified entries to disk other than using more storage space.

## S3 Report Datastore
An optionally configured datastore that is backed by AWS S3. The `report.zip` is not stored in its original form, but rather extracted into a set of objects sharing the same base key path. The last segment of the key is `report.files` to separate it from `report.cache`. This implementation still requires some local disk usage, as it downloads the report.zip from HDS to a temporary file, and then uploads it to S3. 

There is another key prefix that ends with `additional.files` where files that don't come from the report.zip get stored in S3. NB: Previous implementations of the file data store would update the report.zip. This is no longer done in favor of using `additional.files` directory for the file base implementation as well.

To enable S3 you need to specify the config in your config.yml file as follows:
```yaml
storage:
  type: S3 # must match exactly, as this is an enum. Other option is File, but as File is default its better to omit the config entirely
  s3Config:
    # Endpoint is optional, defaults to AWS S3. This example would be for localstack
    endpoint: http://127.0.0.1:4566
    bucketName: "<bucket_name>"
    region: us-east-2 # or any other valid region
```

## Migration
Migration will be covered in a future story https://sonatype.atlassian.net/browse/CLM-33226

## Local testing
Run the LocalStack docker image for S3. This basic setup doesn't configure persistence across restarts of the LocalStack
instance. For more advanced setup see https://hub.docker.com/r/localstack/localstack.

```shell
docker run \
  --rm \
  -p 4566:4566 \
  localstack/localstack:s3-latest
```
Note: There are other ways to run LocalStack https://docs.localstack.cloud/user-guide/aws/s3/, however you're on your own :)

Configure the application to use your LocalStack S3 by adding the following to your config.yml
```yaml
storage:
  type: S3
  s3Config:
    endpoint: http://127.0.0.1:4566
    bucketName: "iq-server-localstack-report-datastore"
    region: us-east-2
```

### LocalStack S3 Basic Operations
**Bucket Operations**

```
# Create bucket
aws --endpoint-url=http://localhost:4566 s3 mb s3://<bucket-name>

# List buckets
aws --endpoint-url=http://localhost:4566 s3 ls

# Delete objects(files) from bucket
aws --endpoint-url=http://localhost:4566 s3 rm s3://<bucket-name> --recursive

# Delete bucket and all contents
aws --endpoint-url=http://localhost:4566 s3 rb s3://<bucket-name> --force
```

**File Operations**

```
# Download file
aws --endpoint-url=http://localhost:4566 s3 cp s3://<bucket-name>/<contents-key> ./

# Read file content
aws --endpoint-url=http://localhost:4566 s3 cp s3://<bucket-name>/<contents-key> -

# List bucket contents
aws --endpoint-url=http://localhost:4566 s3 ls s3://<bucket-name>/<contents-key>
```

you can set up [awslocal](https://docs.localstack.cloud/user-guide/integrations/aws-cli/) that runs within the LocalStack environment without specifying the `--endpoint-url` parameter or a profile

Run IQ server, run a scan and policy evaluation, and confirm that the report objects are stored in S3 and not on disk.
You should be able to see a list of the files in your bucket using http://127.0.0.1:4566/iq-server-localstack-report-datastore
