{
  description = "tinyjcbor — glibc JDK 21 + Maven dev shell (Jazzer's native driver needs glibc)";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs =
    { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      jdk = pkgs.jdk21;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          jdk
          (pkgs.maven.override { jdk_headless = jdk; })
        ];

        shellHook = ''
          export JAVA_HOME=${jdk}/lib/openjdk
          [ -d "$JAVA_HOME" ] || export JAVA_HOME=${jdk}
          # Replace, never append: the host LD_LIBRARY_PATH may contain incompatible (musl/foreign) libs.
          # Jazzer's libjazzer_driver.so needs libstdc++ from a glibc toolchain.
          export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath [ pkgs.stdenv.cc.cc.lib ]}
        '';
      };
    };
}
