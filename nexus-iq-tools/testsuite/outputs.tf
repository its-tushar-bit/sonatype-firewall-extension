output "node_public_dns" {
  value = "${aws_instance.perftest.public_ip}"
}

output "ssh_key" {
  value = "${tls_private_key.ssh_key.private_key_pem}"
}
