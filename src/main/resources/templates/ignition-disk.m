{
	"ignition": {
		"version": "2.1.0",
		"config": {
			
		}
	},
	"storage": {
		"filesystems": [
			{
				"mount": {
					"device": "/dev/disk/by-label/ROOT",
					"format": "btrfs",
					"wipeFilesystem": true,
					"options": [
						"--label=ROOT"
					]
				}
			}
		]
	},
	"systemd": {
		
	},
	"networkd": {
		
	},
	"passwd": {
		"users": [
			{
				"name": "core",
				"sshAuthorizedKeys": [
					"{{sshKey}}"
				]
			}
		]
	}
}