provider "aws" {
region = "us-east-1"
}
resource "aws_instance" "one" {
count = 4
ami = "ami-0ddc798b3f1a5117e"
instance_type = "t2.micro"
key_name = "Awsdevops"
vpc_security_group_ids = ["sg-053d3c714ed5a9a87"]
tags = {
Name = var.instance_names[count.index]
}
}
variable "instance_names" {
default = ["jenkins", "ansible", "tomcat-1", "tomcat-2"]
}
