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
		],
        "files": [{
	      "filesystem": "root",
	      "path": "/etc/hostname",
	      "mode": 420,
	      "contents": { "source": "data:,{{hostname}}" }
	    }]		
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