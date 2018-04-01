#!ipxe

kernel {{baseUrl}}/coreos_production_pxe.vmlinuz initrd=coreos_production_pxe_image.cpio.gz coreos.first_boot=1 console=tty0 console=ttyS0,115200n8 coreos.autologin=ttyS0 coreos.config.url={{contextRoot}}/coreos/ignition-ram/{{hostname}} sshkey="{{sshKey}}"
initrd {{baseUrl}}/coreos_production_pxe_image.cpio.gz
boot