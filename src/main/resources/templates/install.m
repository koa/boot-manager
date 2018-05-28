#!/bin/bash -ex
curl --retry 10 --fail {{contextRoot}}/coreos/ignition-disk/{{hostname}} -o ignition.json
coreos-install -d /dev/sda -i ignition.json -b {{baseUrl}}
udevadm settle
systemctl reboot