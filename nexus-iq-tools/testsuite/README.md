<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Performance Test Suite

Assets related to automated performance testing for the IQ Server. This module includes a
[Terraform](https://www.terraform.io/) configuration for deploying an AWS server and companion shell scripts to
load binaries on the remote machine, execute IQ performance tests, and bring the execution logs and result summary
back to the local machine

## Local Requirements

To provision an ec2 instance and execute a performance evaluation for IQ, you'll need:

1. [Terraform](https://www.terraform.io/)
2. AWS CLI (available via brew)
3. python
4. bash shell

### AWS CLI

Talk with @ops to get AWS credentials. You need access to the `perf-lifecycle-admin` role:

`arn:aws:iam::960315589060:role/admin`

added to `~/.aws/config`:
```
[profile perf-lifecycle]
   region = us-east-1
   role_arn = arn:aws:iam::960315589060:role/admin
   source_profile = default
```

Confirm you have access with:

`aws --profile perf-lifecycle s3 ls iq-perf-datasets/unscrubbed/ods/small/`

(You should see a list of zip files).

### Configure your shell

There are a number of required terraform variables that are left undefined so that they can be loaded from your shell.

Add the following lines to your shell's rc:

```bash
# build_key allows us to distinguish between multiple concurrent, set this to something that uniquely IDs you
export TF_VAR_build_key=`whoami`
# owner allows ops to contact the creator of the environment
# set this to your own email address
export TF_VAR_owner=`git config user.email`
# set this to how many hours the environment must live for tracking purposes; if omitted, it will default to 1 hour
export TF_VAR_duration=1
```

You might also need to provide aws credentials:
```bash
# aws secret key
export TF_VAR_secret_key=$AWS_SECRET_ACCESS_KEY
# aws access key
export TF_VAR_access_key=$AWS_ACCESS_KEY_ID
```

Note: these values can also be populated in a `terraform.tfvars` file.


## Running Performance Test

1. Configure test inputs:
   * `./configure_test.py \-iq ../../insight-brain-service/target/insight-brain-service-*-server.jar -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar -auto ../target/nexus-iq-tools-*-automation.zip -lic /path/to/*lifecycle*.lic`
   * confirm creation of a folder starting with `awsPerfRun_`
2. Initialize terraform with `terraform init`
   * note: this only needs to be done for first run or to update terraform
3. Bootstrap your node with `terraform apply`
   * This takes under 2 minutes to complete.
4. Execute test with: `./run_aws_test.sh`
 * this will:
   * push your test directory to the remote node
   * execute the test on the remote node
   * pull the results down to the local machine
5. OPTIONAL - repeat steps #1 & #3 to run additional tests, the most recent `awsPerfRun_` folder will always be used
6. Teardown your instance with `terraform destroy`


## Accessing Nodes via SSH

To log in to the instance directly via SSH run the generated connection script.

> ./scripts/connect.sh

Once logged in, you may switch to a root user with `sudo -s`

## For the brave

If you've recently done a build of the insight-brain repository and have a license that can be found with the `locate`
command, and like default options...

`./i_feel_lucky.sh`
