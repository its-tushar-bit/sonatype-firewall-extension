# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

variable "assume_role_arn" {
  default = "arn:aws:iam::960315589060:role/ZionJenkins"
}

variable "aws_region" {
  default = "us-east-1"
}

variable "owner" {
}

variable "sonatype_group" {
  default = "iq"
}

variable "environment" {
  default = "performance-iq"
}

variable "platform" {
  default = "lifecycle-test"
}

variable "build_key" {
  default = ""
}

# will auto-shutdown after the specified duration in minutes
variable "duration" {
  default = "120"
}

variable "use_postgres" {
  default = false
}
