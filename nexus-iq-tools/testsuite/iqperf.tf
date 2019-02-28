# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

provider "aws" {
  region = "${var.aws_region}"

  access_key = "${var.access_key}"
  secret_key = "${var.secret_key}"

  assume_role = {
    role_arn     = "${var.assume_role_arn}"
    session_name = "perf_session"
    external_id  = "perf_id"
  }
}

resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
}

resource "aws_key_pair" "auth" {
  key_name_prefix = "iqpertest"
  public_key      = "${tls_private_key.ssh_key.public_key_openssh}"
}

resource "aws_instance" "perftest" {
  iam_instance_profile = "${aws_iam_instance_profile.ec2_instance_profile.id}"

  connection {
    user        = "ec2-user"
    private_key = "${tls_private_key.ssh_key.private_key_pem}"
  }

  ami      = "ami-074e79ff43d863acb"
  key_name = "${aws_key_pair.auth.id}"

  vpc_security_group_ids = [
    "${aws_security_group.ec2.id}",
  ]

  instance_type = "m5d.2xlarge"

  tags {
    Name           = "perf-eng-lifecycle-test"
    Platform       = "${var.platform}"
    BuildKey       = "${var.build_key}"
    sonatype-group = "${var.sonatype_group}"
    owner          = "${var.owner}"
    environment    = "${var.environment}"
    ttl            = "${var.duration}"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo yum install -y java-1.8.0",
      "sudo yum install -y python3",
      "sudo pip3 install pipenv",
      "sudo yum install -y emacs-nox",
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "sudo mkdir -p /iqperf_eval/{files,data}",
      "sudo mkfs.ext4 -E nodiscard /dev/nvme1n1",
      "sudo mount /dev/nvme1n1 /iqperf_eval/data/",
      "sudo chown -R ec2-user:ec2-user /iqperf_eval/",
    ]
  }

  provisioner "file" {
    source      = "./scripts/test_execute.sh"
    destination = "/iqperf_eval/data/test_execute.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x /iqperf_eval/data/test_execute.sh",
    ]
  }

  provisioner "file" {
    source      = "./scripts/time_limit.sh"
    destination = "/iqperf_eval/time_limit.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x /iqperf_eval/time_limit.sh",
      "nohup sudo /iqperf_eval/time_limit.sh ${var.duration} &",
      "sleep 2",
    ]
  }
}

resource "aws_security_group" "ec2" {
  name        = "iq-perf-ec2-security-group"
  description = "IQ Perf Security Group"

  ## externally accessible ports
  # ssh
  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  tags {
    Platform       = "${var.platform}"
    BuildKey       = "${var.build_key}"
    sonatype-group = "${var.sonatype_group}"
    owner          = "${var.owner}"
    environment    = "${var.environment}"
    ttl            = "${var.duration}"
  }
}

resource "local_file" "pemfile" {
  content  = "${tls_private_key.ssh_key.private_key_pem}"
  filename = "./iqperf-ssh.pem"

  provisioner "local-exec" {
    command = "chmod 400 ./iqperf-ssh.pem"
  }
}

resource "local_file" "connect" {
  content  = "ssh -i ./iqperf-ssh.pem -o \"StrictHostKeyChecking no\" ec2-user@${aws_instance.perftest.public_ip}"
  filename = "./scripts/connect.sh"

  provisioner "local-exec" {
    command = "chmod +x ./scripts/connect.sh"
  }
}

resource "local_file" "dirupload" {
  content  = "scp -i ./iqperf-ssh.pem -o \"StrictHostKeyChecking no\"  -r $1 ec2-user@${aws_instance.perftest.public_ip}:/iqperf_eval/data/"
  filename = "./scripts/dirupload.sh"

  provisioner "local-exec" {
    command = "chmod +x ./scripts/dirupload.sh"
  }
}

resource "local_file" "runremote" {
  content  = "ssh -i ./iqperf-ssh.pem -o \"StrictHostKeyChecking no\" ec2-user@${aws_instance.perftest.public_ip} \"$1\""
  filename = "./scripts/run_remote.sh"

  provisioner "local-exec" {
    command = "chmod +x ./scripts/run_remote.sh"
  }
}

resource "local_file" "fetchresults" {
  content  = "scp -i ./iqperf-ssh.pem -o \"StrictHostKeyChecking no\" ec2-user@${aws_instance.perftest.public_ip}:/iqperf_eval/data/$1/results.tar ./$1/"
  filename = "./scripts/fetch_results.sh"

  provisioner "local-exec" {
    command = "chmod +x ./scripts/fetch_results.sh"
  }
}

### iam roles and policies

data "aws_iam_policy_document" "assume_role_policy" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type = "Service"

      identifiers = [
        "ec2.amazonaws.com",
      ]
    }
  }
}

resource "aws_iam_role" "ec2_role" {
  name_prefix        = "ec2_role"
  assume_role_policy = "${data.aws_iam_policy_document.assume_role_policy.json}"
}

data "aws_iam_policy_document" "iqperf_iam_policy" {
  statement {
    actions = [
      "ec2:DescribeInstances",
    ]

    resources = ["*"]
  }
}

resource "aws_iam_policy" "iqperf_policy" {
  name_prefix = "iqperf_policy"
  policy      = "${data.aws_iam_policy_document.iqperf_iam_policy.json}"
}

resource "aws_iam_role_policy_attachment" "iqperf_policy_attachment" {
  role       = "${aws_iam_role.ec2_role.name}"
  policy_arn = "${aws_iam_policy.iqperf_policy.arn}"
}

resource "aws_iam_instance_profile" "ec2_instance_profile" {
  name_prefix = "ec2_instance_profile"
  role        = "perf_lifecycle_ec2_admin"
}
