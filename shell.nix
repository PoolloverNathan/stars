{
  pkgs ? import (fetchTarball "https://github.com/nixos/nixpkgs/archive/fe51d34885f7b5e3e7b59572796e1bcb427eccb1.tar.gz") {},
  nixGL ? import (fetchTarball "https://github.com/nix-community/nixGL/archive/310f8e49a149e4c9ea52f1adf70cdc768ec53f8a.tar.gz") { inherit pkgs; },
}:

pkgs.mkShell {
  buildInputs = [
    pkgs.gradle_8
    pkgs.zulu21
    pkgs.bashInteractive
    nixGL.auto.nixGLDefault
  ];
}
