#!/usr/bin/env python3
"""Check release metadata; this does not replace runtime compatibility acceptance."""
import argparse
import json
import hashlib
from pathlib import Path
from zipfile import ZipFile
p = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--require-verified-hash', action='store_true',
                    help='Refuse release unless this jar matches the runtime-accepted SHA-256')
args = parser.parse_args()
identity = json.loads((p/'legacy-identity.json').read_text())
jars = [f for f in (p/'build/libs').glob(identity['modid']+'-*.jar') if not f.stem.endswith(('-sources','-api','-mcp','-javadoc'))]
if len(jars) != 1:
    raise SystemExit('Expected exactly one production jar: '+repr(jars))
with ZipFile(jars[0]) as archive:
    metadata = json.loads(archive.read('mcmod.info'))[0]
    assert metadata['modid'] == identity['modid'], metadata
    assert metadata['name'] == identity['name'], metadata
    assert metadata['authorList'] == ['yudream'], metadata
    logo = metadata['logoFile']
    assert logo in archive.namelist(), 'Missing project icon: ' + logo
    assert archive.read(logo) == (p/'branding/icon.png').read_bytes(), 'Packaged icon differs from project artwork'
    assert '${' not in archive.read('mcmod.info').decode(), 'Unexpanded metadata'
    assert not any('/dev/' in name or 'Smoke.class' in name for name in archive.namelist() if name.endswith('.class')), 'Development harness in release'
    classes = [name for name in archive.namelist() if name.endswith('.class')]
    assert not any(name.startswith(('hellfirepvp/modularmachinery/', 'github/kasuminova/mmce/',
                                    'net/createmod/ponder/verification/')) for name in classes), 'Dependency or harness bundled in release'
    assert 'net/createmod/ponder/mmce/PonderMMCEMod.class' not in classes, 'Old standalone MMCE entrypoint bundled'
    assert 'net/createmod/ponder/mmce/PonderMMCEEntrypoint.class' in classes, 'MMCE optional integration missing'
    assert archive.namelist().count('mixins.ponder.refmap.json') == 1, 'Expected one SRG refmap'
    expected_tag = 'v' + metadata['version']
    assert identity['release_tag'] == expected_tag, 'Release tag differs from packaged version'
sha256 = hashlib.sha256(jars[0].read_bytes()).hexdigest()
if args.require_verified_hash and sha256 != identity['verified_jar_sha256']:
    raise SystemExit('Runtime acceptance required for new jar SHA-256: ' + sha256
                     + '\nAccepted: ' + identity['verified_jar_sha256'])
print('PASS:', jars[0].name, metadata['modid'], 'SHA-256:', sha256)
