{ pkgs ? import <nixpkgs> {
  config.allowUnfreePredicate =  with builtins; (pkg: elem (parseDrvName pkg.name).name [
              "google-chrome"
            ]);
} }:

let
  lib = import <nixpkgs/lib>;
  NPM_CONFIG_PREFIX = toString ./npm_config_prefix;
  chromeCopy = pkgs.runCommand "chromeCopy" {
    buildInputs = [ pkgs.google-chrome ];
  } ''
       set -ex
       mkdir -p $out
       cp -R --no-preserve=mode,ownership --symbolic-link ${pkgs.google-chrome}/* $out/
       mv $out/bin/google-chrome-stable $out/bin/chrome
    '';

in pkgs.mkShell {
  name = "kotbridge-env";
  packages = with pkgs;
    [
      git
      chromedriver
      jdk
      nodejs_18 # Major version used by KMP
    ];

  inherit NPM_CONFIG_PREFIX;

  shellHook = ''
export JAVA_HOME="${pkgs.jdk.home}"
export GRADLE_OPTS="-Dorg.gradle.java.home=${pkgs.jdk.home}"
export NIX=1
export PATH="${chromeCopy}/bin:${NPM_CONFIG_PREFIX}/bin:$PATH"
export SE_AVOID_BROWSER_DOWNLOAD=true
export SE_BROWSER_PATH="${chromeCopy}/bin/chrome"
export SE_BROWSER=chrome
export SE_OFFLINE=true
'';
}
