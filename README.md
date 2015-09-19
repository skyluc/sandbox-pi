# sandbox-pi
Raspberry Pi related stuff

## install imange

```
dd bs=4M if=sparkpi-2015-08-07-raspbian-wheezy.img of=/dev/sdc
```

## wifi configuration

in `/etc/wpa_supplicant/wpa_supplicant.conf`

```
network={
  ssid="ssid"
  psk="passwork/passphrase"
  group=CCMP TKIP
  key_mgmt=WPA-PSK
  pairwise=CCMP TKIP
  auth_alg=OPEN
}
```

## bootstrap

* resize the root partition
* set manual configuration for eth0:

  ```
  #auto eth0
  allow-hotplug eth0
  iface eth0 inet static
  address 10.110.100.1
  netmask 255.255.255.0
  network 10.110.100.0
  broadcast 10.110.100.255
  ```

* mkdir `~/.ssh`
* copy keys `id_rsa_pi_github` and `id_rsa_pi_github.pub` in `~/.ssh`
* create `~/.ssh/config` file:
 
  ```
  Host github.com

  HostName github.com
  IdentityFile ~/.ssh/id_rsa_pi_github
  User git
  ```

* create a local key pair:

  ```
  ssh-keygen
  ```

* push public key locally:

  ```
  ssh-copy-id localhost
  ```

* push public key to tinc-gateway

  ```
  ssh-copy-id luc@nt.skyluc.org
  ```

* install pip, and the Python modules for ansible

  ```
  sudo apt-get update
  sudo apt-get install python-pip python-dev build-essential
  sudo easy_install pip 
  sudo pip install --upgrade virtualenv 
  sudo pip install paramiko PyYAML Jinja2 httplib2 six
  ```

* mkdir `~/dev/ansible`
* in `~/dev/ansible`:

  ```
  git clone git://github.com/ansible/ansible.git --recursive
  ```

* in `~/dev/ansible/ansible`:

  ```
  source ./hacking/env-setup
  ```

* mkdir `~/dev/pi`
* in `~/dev/pi/`:

  ```
  git clone -o perso git@github.com:skyluc/sandbox-pi
  ```

* in `~/dev/pi/sandbox-pi/ansible`:

  ```
  ansible-playbook -i hosts gateway_init.yml
  ```

* connect the nodes
* copy the public key to the nodes:

  ```
  ssh-copy-id 10.110.100.2
  ```

* check you can connect:

  ```
  ssh 10.110.100.2
  ```

* in `~/dev/pi/sandbox-pi/ansible`:

  ```
  ansible-playbook -i hosts nodes_init.yml
  ```

  WARNING: might need to reboot for tinc to correctly install

* set the user password on all nodes


