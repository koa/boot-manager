#!/bin/bash -ex
curl --retry 10 --fail {{contextRoot}}/coreos/ignition-disk -o ignition.json
coreos-install -d /dev/sda -i ignition.json
udevadm settle
systemctl reboot