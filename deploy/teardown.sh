#!/usr/bin/env bash
# Deletes everything deploy.sh created (server, its disk and database, security group, key pair),
# so AWS stops billing. This cannot be undone.
set -euo pipefail

REGION="${AWS_REGION:-ap-south-1}"
NAME="hospital-booking"
KEY_NAME="$NAME-key"
KEY_FILE="$HOME/.ssh/$KEY_NAME.pem"

ec2() { aws --region "$REGION" ec2 "$@"; }

read -r -p "Delete the $NAME server and ALL its data in $REGION? Type 'delete' to confirm: " answer
[ "$answer" = "delete" ] || { echo "Cancelled."; exit 1; }

IDS="$(ec2 describe-instances \
    --filters Name=tag:Name,Values="$NAME" Name=instance-state-name,Values=pending,running,stopping,stopped \
    --query 'Reservations[].Instances[].InstanceId' --output text)"
if [ -n "$IDS" ]; then
    echo "==> Terminating $IDS"
    ec2 terminate-instances --instance-ids $IDS >/dev/null
    ec2 wait instance-terminated --instance-ids $IDS
fi

SG_ID="$(ec2 describe-security-groups --filters Name=group-name,Values="$NAME-sg" \
    --query 'SecurityGroups[0].GroupId' --output text)"
if [ "$SG_ID" != "None" ]; then
    echo "==> Deleting security group $SG_ID"
    ec2 delete-security-group --group-id "$SG_ID"
fi

if ec2 describe-key-pairs --key-names "$KEY_NAME" >/dev/null 2>&1; then
    echo "==> Deleting key pair $KEY_NAME"
    ec2 delete-key-pair --key-name "$KEY_NAME"
fi
rm -f "$KEY_FILE"

echo "Done. Nothing from this project is left running in $REGION."
