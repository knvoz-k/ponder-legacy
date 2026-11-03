#!/usr/bin/env python3
"""Prepare pinned source dependencies for a standalone legacy repository."""
import hashlib
import json
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def main():
    lock = ROOT / 'build-dependencies.json'
    dependencies = json.loads(lock.read_text()) if lock.exists() else []
    for dep in dependencies:
        sibling = ROOT.parent / dep['directory']
        if (sibling / 'build.gradle').is_file():
            continue
        revision = dep.get('revision')
        if not revision or len(revision) != 40:
            raise SystemExit(f"Source dependency has not passed publication acceptance: {dep['repository']}")
        target = ROOT / '.legacy-deps' / dep['directory']
        target.mkdir(parents=True, exist_ok=True)
        if not (target / '.git').is_dir():
            subprocess.run(['git', 'init', '-q', str(target)], check=True)
        if subprocess.run(['git', '-C', str(target), 'cat-file', '-e', revision+'^{commit}'], capture_output=True).returncode:
            subprocess.run(['git', '-C', str(target), 'fetch', '--depth=1', 'https://github.com/'+dep['repository']+'.git', revision], check=True)
        subprocess.run(['git', '-C', str(target), 'checkout', '--detach', revision], check=True)
    # AE2S Ponder has an independently pinned upstream runtime and development jar.
    lock = ROOT / 'dependency-lock.json'
    if lock.exists():
        ae2s = json.loads(lock.read_text()).get('ae2s', {})
        for kind in ['dev', 'runtime']:
            artifact = ae2s.get(kind)
            if not artifact:
                continue
            target = ROOT / 'libs' / artifact['file']
            if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == artifact['sha256']:
                continue
            target.parent.mkdir(exist_ok=True)
            request = urllib.request.Request(artifact['url'], headers={'User-Agent':'mcyudream-legacy-build'})
            with urllib.request.urlopen(request, timeout=120) as response:
                data = response.read()
            if hashlib.sha256(data).hexdigest() != artifact['sha256']:
                raise SystemExit('AE2S checksum mismatch: '+target.name)
            target.write_bytes(data)


if __name__ == '__main__':
    main()
