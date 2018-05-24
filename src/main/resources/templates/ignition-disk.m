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
        "units": [{
           "name": "00-primary.network",
           "contents": "[Match]\nName=eth-srv\n\n[Network]\nAddress=2a02:168:e000:1c0::10:{{suffix}}/64\n\nDHCP=yes\n[DHCP]\nUseMTU=true\nUseDomains=true\n[Route]\nGateway=2a02:168:e000:1c0::"
         },{
           "name": "00-primary.link",
           "contents": "[Match]\nMACAddress={{mac}}\n\n[Link]\nName=eth-srv"
      }]
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