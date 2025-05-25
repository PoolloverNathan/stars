{ pkgs ? import (fetchTarball "https://github.com/nixos/nixpkgs/archive/fe51d34885f7b5e3e7b59572796e1bcb427eccb1.tar.gz") {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.gradle_8
    pkgs.zulu21
    pkgs.bashInteractive
  ];
}
