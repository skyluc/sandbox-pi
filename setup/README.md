# Bootstrap

## Install raspbian on all sd card

### Copy images

```bash
sudo dd bs=4M if=2015-11-21-raspbian-jessie.img of=/dev/sdc
```

### Re-mount the sdcard

### switch from root on sd to root on usb

* in the boot partition, edit `/cmdline.txt`, change `root` to `/dev/sda2`, and add `rootdelay=5`

```
dwc_otg.lpm_enable=0 console=ttyAMA0,115200 kgdboc=ttyAMA0,115200 console=tty1 root=/dev/sda2 rootfstype=ext4 elevator=deadline fsck.repair rootwait rootdelay=5
```

## Install raspbian on all usb drive/ssd

### Copy images

```bash
sudo dd bs=4M if=2015-11-21-raspbian-jessie.img of=/dev/sdc
```

### Re-mount the usb drive

### switch from root on sd to root on usb

* in the root partition, edit `/etc/fstab`, change the root device to `/dev/sda2`, and add `rootdelay=5`

```
/dev/sda2    /     ext4   default,noatime 0 1
```

## Launch lead Pi

### resize root partition

```
sudo fdisk /dev/sda
```

* `d` `2`, to delete the second partition

* `n` `p` `2` `131072` <default>, to recreate it with all the available space

* `w`, to write the new partition

* reboot

```
sudo resize2fs /dev/sda2
```

### Configure wifi connection

### upgrade

```
sudo apt-get update
sudo 
```

### minimal softwar

```
sudo apt-get install openjdk-8-jdk
```

```
mkdir -p opt/archive
cd opt/archive
wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.9/sbt-0.13.9.tgz
cd ..
tar xf archive/sbt-0.13.9.tgz
mv sbt sbt-0.13.9
ln -s sbt-0.13.9 sbt
```

```
cat > ~/.bash_aliases
export PATH=~/opt/sbt/bin:$PATH
```


