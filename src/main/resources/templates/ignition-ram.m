{
	"ignition": {
		"version": "2.0.0"
	},
	"systemd": {
		"units": [
			{
				"name": "installer.service",
				"enable": true,
				"contents": "[Unit]\nRequires=network-online.target\nAfter=network-online.target\n[Service]\nType=simple\nExecStart=/opt/installer\n[Install]\nWantedBy=multi-user.target"
			}
		]
	},
	"storage": {
		"files": [
			{
				"path": "/opt/installer",
				"filesystem": "root",
				"mode": 320,
				"contents": {
					"source": "{{contextRoot}}/coreos/install-script"
				}
			}
		]
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

