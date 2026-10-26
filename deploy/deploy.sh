#!/usr/bin/env bash
# Deploys the app to a single EC2 server (app + MySQL 8 + Nginx).
# First run creates everything; later runs just upload the new jar and restart.
#
# Usage:  ./deploy/deploy.sh            (region defaults to ap-south-1 / Mumbai)
set -euo pipefail

REGION="${AWS_REGION:-ap-south-1}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.micro}"
NAME="hospital-booking"
KEY_NAME="$NAME-key"
KEY_FILE="$HOME/.ssh/$KEY_NAME.pem"

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$DEPLOY_DIR")"

ec2() { aws --region "$REGION" ec2 "$@"; }
ssh_cmd() { ssh -i "$KEY_FILE" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 "ubuntu@$IP" "$@"; }

echo "==> Checking AWS credentials"
aws sts get-caller-identity --query Account --output text >/dev/null

echo "==> Building the jar"
(cd "$ROOT" && ./mvnw -B -q clean package -DskipTests)
JAR="$(ls "$ROOT"/target/hospital-appointment-booking-*.jar | grep -v '\.original$' | head -1)"

echo "==> Key pair"
if ! ec2 describe-key-pairs --key-names "$KEY_NAME" >/dev/null 2>&1; then
    mkdir -p "$HOME/.ssh"
    ec2 create-key-pair --key-name "$KEY_NAME" --query KeyMaterial --output text > "$KEY_FILE"
    chmod 400 "$KEY_FILE"
    echo "    created $KEY_FILE"
elif [ ! -f "$KEY_FILE" ]; then
    echo "Key pair $KEY_NAME exists in AWS but $KEY_FILE is missing. Run ./deploy/teardown.sh first." >&2
    exit 1
fi

echo "==> Security group"
VPC_ID="$(ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
SG_ID="$(ec2 describe-security-groups --filters Name=group-name,Values="$NAME-sg" Name=vpc-id,Values="$VPC_ID" \
    --query 'SecurityGroups[0].GroupId' --output text)"
if [ "$SG_ID" = "None" ]; then
    SG_ID="$(ec2 create-security-group --group-name "$NAME-sg" --vpc-id "$VPC_ID" \
        --description "Hospital booking: HTTP from anywhere, SSH from the deployer" --query GroupId --output text)"
    ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 80 --cidr 0.0.0.0/0 >/dev/null
fi
# SSH only from this computer's current public IP
MY_IP="$(curl -s https://checkip.amazonaws.com)"
ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 22 --cidr "$MY_IP/32" >/dev/null 2>&1 || true

echo "==> EC2 instance"
INSTANCE_ID="$(ec2 describe-instances \
    --filters Name=tag:Name,Values="$NAME" Name=instance-state-name,Values=pending,running,stopping,stopped \
    --query 'Reservations[0].Instances[0].InstanceId' --output text)"
FIRST_DEPLOY=false
if [ "$INSTANCE_ID" = "None" ]; then
    FIRST_DEPLOY=true
    AMI_ID="$(aws --region "$REGION" ssm get-parameter \
        --name /aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id \
        --query Parameter.Value --output text)"
    USER_DATA="$(mktemp)"
    sed "s/__DB_PASS__/$(openssl rand -hex 16)/g" "$DEPLOY_DIR/user-data.sh" > "$USER_DATA"
    INSTANCE_ID="$(ec2 run-instances --image-id "$AMI_ID" --instance-type "$INSTANCE_TYPE" \
        --key-name "$KEY_NAME" --security-group-ids "$SG_ID" \
        --user-data "file://$USER_DATA" \
        --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":20,"VolumeType":"gp3"}}]' \
        --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$NAME}]" \
        --query 'Instances[0].InstanceId' --output text)"
    rm -f "$USER_DATA"
    echo "    launched $INSTANCE_ID ($INSTANCE_TYPE, Ubuntu 24.04)"
else
    ec2 start-instances --instance-ids "$INSTANCE_ID" >/dev/null 2>&1 || true
    echo "    reusing $INSTANCE_ID"
fi
ec2 wait instance-running --instance-ids "$INSTANCE_ID"
IP="$(ec2 describe-instances --instance-ids "$INSTANCE_ID" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"

echo "==> Waiting for SSH on $IP"
for _ in $(seq 1 60); do ssh_cmd true 2>/dev/null && break; sleep 5; done

if $FIRST_DEPLOY; then
    echo "==> Installing Java 21, MySQL 8 and Nginx on the server (takes 3-5 minutes)"
fi
ssh_cmd "cloud-init status --wait >/dev/null; cloud-init status" | grep -q "done" \
    || { echo "Server setup failed. Check: ssh -i $KEY_FILE ubuntu@$IP 'sudo tail -50 /var/log/cloud-init-output.log'" >&2; exit 1; }

echo "==> Uploading $(basename "$JAR") and restarting"
scp -i "$KEY_FILE" -q "$JAR" "ubuntu@$IP:/tmp/app.jar"
ssh_cmd "sudo mv /tmp/app.jar /opt/hospital/app.jar && sudo chown hospital:hospital /opt/hospital/app.jar && sudo systemctl restart hospital"

echo "==> Waiting for the app to start"
for _ in $(seq 1 60); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://$IP/")" = "200" ]; then
        echo
        echo "Deployed!  http://$IP"
        echo "Swagger:   http://$IP/swagger-ui.html"
        echo "SSH:       ssh -i $KEY_FILE ubuntu@$IP"
        echo "Logs:      ssh -i $KEY_FILE ubuntu@$IP 'sudo journalctl -u hospital -f'"
        exit 0
    fi
    sleep 5
done
echo "App did not respond. Check logs: ssh -i $KEY_FILE ubuntu@$IP 'sudo journalctl -u hospital -n 100'" >&2
exit 1
