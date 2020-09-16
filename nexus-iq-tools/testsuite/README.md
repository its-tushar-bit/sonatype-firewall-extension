<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Performance Test Suite

Assets related to automated performance testing for the IQ Server. This module includes a
[Terraform](https://www.terraform.io/) configuration for deploying an AWS server and companion shell
scripts to load binaries on the remote machine, execute IQ performance tests, and bring the
execution logs and result summary back to the local machine.

macOS and Linux run the scripts without issues.

## Local Requirements

To provision an AWS EC2 instance and execute a performance evaluation for IQ, you'll need:

1. [Terraform](https://www.terraform.io/) (version v0.13)
2. AWS CLI
3. Python 3.7
4. Bash shell

### AWS CLI

Talk with @ops to get AWS credentials. You need access to the `perf-lifecycle-admin` role:
`arn:aws:iam::960315589060:role/admin`.

Configure your credentials with `aws configure`. Verify that they are in the `~/.aws/credentials`
file.

To access AWS here we use the environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`,
and `AWS_SESSION_TOKEN`.

To assume the previous role and get temporary values for the previous variables use:
```shell script
aws sts assume-role \
    --role-arn arn:aws:iam::960315589060:role/admin \
    --role-session-name perf_session \
    --external-id perf_id \
    --duration-seconds 7200
```

If you have installed the `jq` program, the following command can be used to get the values and set
them:
```shell script
temp=$(aws sts assume-role --role-arn arn:aws:iam::960315589060:role/admin \
    --role-session-name perf_session --external-id perf_id --duration-seconds 7200); \
    export AWS_ACCESS_KEY_ID=$(echo $temp | jq -r '.Credentials.AccessKeyId'); \
    export AWS_SECRET_ACCESS_KEY=$(echo $temp | jq -r '.Credentials.SecretAccessKey'); \
    export AWS_SESSION_TOKEN=$(echo $temp | jq -r '.Credentials.SessionToken'); unset temp
```
Verify their values with:
```shell script
echo $AWS_ACCESS_KEY_ID; echo $AWS_SECRET_ACCESS_KEY; echo $AWS_SESSION_TOKEN
```

Confirm you have access with: `aws s3 ls iq-perf-datasets/unscrubbed/ods/small/`

(You should see a list of zip files).

### Configure your shell

There are a number of required terraform variables that are left undefined so that they can be loaded from your shell.

Add the following lines to your shell's rc (`.profile`, `.bash_profile`, etc):

```shell script
# build_key allows us to distinguish between multiple concurrent, set this to something that uniquely IDs you
export TF_VAR_build_key=`whoami`
# owner allows ops to contact the creator of the environment
# set this to your own email address
export TF_VAR_owner=`git config user.email`
# set the role assumed for the development environment
export TF_VAR_assume_role_arn='arn:aws:iam::960315589060:role/admin'
```

Note: these values can also be populated in a `terraform.tfvars` file.

The Terraform AWS provider will try to find the credentials with different methods:
* Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
* Shared credentials file ($HOME/.aws/credentials)
* EC2 Role

So make sure one of them works. More details here: https://www.terraform.io/docs/providers/aws/index.html

## Running Performance Test

### Configure test inputs
Navigate to the folder `nexus-iq-tools/testsuite` in the terminal and run `./configure_test.py -h`
to know the available parameters.

Here is a template for the command. Replace `<license.lic>` with the path to the license: 
```shell script
./configure_test.py \
    -p performance-test-profile-small.json \
    -iq ../../insight-brain-service/target/insight-brain-service-*-server.jar \
    -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar \
    -auto ../target/nexus-iq-tools-*-automation.zip \
    -lic <license.lic> \
    -u template-urls.json
```
A folder with the `awsPerfRun_` prefix should be created.

To use Postgres the `--use-postgres` parameter can be used.

### Initialize Terraform
Use `terraform init`. This only needs to be done for first run or to update Terraform.

### Create testing environment on AWS 
Run `terraform apply`. This takes several minutes to complete.

### Execute test
Run `./run_aws_test.sh`. This will:
* Push your test directory to the remote node
* Execute the test on the remote node
* Pull the results down to the local machine

### Destroy testing environment on AWS
After the test finishes, it is important to destroy all resources created on AWS. For that use:
`terraform destroy`.

## Accessing AWS EC2 instance via SSH

To log in to the AWS EC2 instance directly via SSH run the generated connection script with:
`./scripts/connect.sh`

Once logged in, you may switch to a root user with `sudo -s`

## For the brave

If you've recently done a build of the insight-brain repository and have a license that can be found
with the `locate` command, and like default options try: `./i_feel_lucky.sh`
