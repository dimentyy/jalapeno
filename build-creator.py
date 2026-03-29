#!/usr/bin/env python3

import argparse
import os
import subprocess
import sys
from pathlib import Path
from typing import Any

TARGETS = ["darwin-arm64", "windows-x64", "linux-x64"]


ROOT = Path(__file__).resolve().parent
RELAY_DIR = ROOT / "relay"
CREATOR_DIR = ROOT / "creator-app"


def main() -> None:
    args = parse_args()

    targets = args.targets or TARGETS

    if not args.electron_only:
        build_relay(targets)

    if not args.relay_only:
        build_electron(targets)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "targets",
        nargs="*",
        choices=[*TARGETS, []],
    )

    group = parser.add_mutually_exclusive_group()
    group.add_argument("--relay-only", action="store_true")
    group.add_argument("--electron-only", action="store_true")

    return parser.parse_args()


def run(args: Any, **kwargs) -> None:
    print(" ".join(map(str, args)))

    subprocess.run(args, check=True, stdout=sys.stdout, stderr=sys.stderr, **kwargs)


def go_build(goos: str, goarch: str, output: Path):
    run(
        ("go", "build", "-o", output, "."),
        cwd=RELAY_DIR,
        env={**os.environ, "GOOS": goos, "GOARCH": goarch},
    )


def build_relay(targets: list[str]):
    for target in targets:
        match target:
            case "darwin-arm64":
                go_build("darwin", "arm64", RELAY_DIR / "relay-darwin-arm64")
            case "windows-x64":
                go_build("windows", "amd64", RELAY_DIR / "relay-windows-x64.exe")
            case "linux-x64":
                go_build("linux", "amd64", RELAY_DIR / "relay-linux-x64")


def build_electron(targets: list[str]):
    run(["npm", "install", "--quiet"], cwd=CREATOR_DIR)

    for target in targets:
        match target:
            case "darwin-arm64":
                flags = ["--mac"]
            case "windows-x64":
                flags = ["--win"]
            case "linux-x64":
                flags = ["--linux"]

        run(["npx", "electron-builder", *flags], cwd=CREATOR_DIR)  # noqa


if __name__ == "__main__":
    main()
