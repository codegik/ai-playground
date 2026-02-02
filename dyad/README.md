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



