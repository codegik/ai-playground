# Dyad POC

This is a POC using Dyad to build apps locally.

## Install

I'm using Omarchy linux based on Arch distro. Here is how to run it.

### Install debtap

``` 
yay -S debtap
sudo debtap -u
```

### Install Dyad

```
curl -O https://github.com/dyad-sh/dyad/releases/download/v0.34.0/dyad_0.34.0_amd64.deb
debtap dyad_0.34.0_amd64.deb
sudo pacman -U dyad-0.34.0-1-x86_64.pkg.tar.zst
```
During the installation, you will be asked to edit the PKGBUILD file. 

Then will find depencency `depends = gtk`, you must change to `depends = gtk3`.

Make sure there will be only one `depends = gtk3` in the file. Then run:

```
sudo pacman -U dyad-0.34.0-1-x86_64.pkg.tar.zst
```

## Run it

```
dyad
```
