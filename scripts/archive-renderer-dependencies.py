"""Archive the exact locked Android crates and their actual license texts.

Inputs come from cargo metadata --locked --filter-platform for each shipped ABI.
No network, guessed licenses, absolute paths or mutable dependency versions in
the output. Crate bytes are checked against Cargo.lock before being archived.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import tomllib

parser=argparse.ArgumentParser()
parser.add_argument('--metadata',type=Path,nargs='+',required=True)
parser.add_argument('--lock',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
parser.add_argument('--supplements',type=Path,required=True)
args=parser.parse_args()
lock=tomllib.loads(args.lock.read_text(encoding='utf-8'))
checksums={(p['name'],p['version']):p.get('checksum') for p in lock['package']}
packages={}
targets={}
for metadata in args.metadata:
    data=json.loads(metadata.read_text(encoding='utf-8-sig'))
    ids={n['id'] for n in data['resolve']['nodes']}
    selected=[p for p in data['packages'] if p['id'] in ids and p['source']]
    targets[metadata.stem]=sorted(p['name']+'@'+p['version'] for p in selected)
    for package in selected:packages[package['id']]=package
output=args.output
output.mkdir(parents=True,exist_ok=True)
records=[]
supplements=json.loads((args.supplements/'SOURCES.json').read_text(encoding='utf-8'))
for package in sorted(packages.values(),key=lambda p:(p['name'],p['version'])):
    name,version=package['name'],package['version']
    directory=Path(package['manifest_path']).parent
    assert package['source']=='registry+https://github.com/rust-lang/crates.io-index',package['source']
    crate=directory.parent.parent.parent/'cache'/directory.parent.name/(name+'-'+version+'.crate')
    checksum=hashlib.sha256(crate.read_bytes()).hexdigest()
    assert checksum==checksums[(name,version)],f'Crate checksum mismatch: {name} {version}'
    archive=output/'registry-sources'/crate.name
    archive.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(crate,archive)
    notices=[]
    for source in sorted(directory.rglob('*')):
        if not source.is_file() or not source.name.lower().startswith(('license','licence','copying','notice','copyright')):continue
        relative=source.relative_to(directory)
        target=output/'licenses'/(name+'-'+version)/relative
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(source,target)
        notices.append({'path':target.relative_to(output).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
    supplement=next((s for s in supplements if s['package']==name and s['version']==version),None)
    if supplement:
        vcs=json.loads((directory/'.cargo_vcs_info.json').read_text())
        assert vcs['git']['sha1']==supplement['revision'],name
        for item in supplement['licenses']:
            source=args.supplements/item['path']
            assert hashlib.sha256(source.read_bytes()).hexdigest()==item['sha256'],source
            target=output/'licenses'/(name+'-'+version)/item['name']
            target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
            notices.append({'path':target.relative_to(output).as_posix(),'sha256':item['sha256'],'origin':item['download_url'],'revision':supplement['revision']})
    assert notices,f'No actual license text for {name} {version}'
    records.append({'name':name,'version':version,'spdx':package['license'],'source':package['source'],
                    'repository':package['repository'],'crate':archive.relative_to(output).as_posix(),
                    'sha256':checksum,'license_texts':notices,'supplement_declaration':supplement.get('declaration') if supplement else None})
manifest={'schema':1,'lock_sha256':hashlib.sha256(args.lock.read_bytes()).hexdigest(),'targets':targets,'packages':records}
(output/'DEPENDENCIES.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(output/'NOTICE.txt').write_text('Android Rust renderer dependencies\n\nExact versions/checksums/SPDX expressions and retained license texts are in DEPENDENCIES.json and licenses/.\n'
    'The corresponding unmodified crates.io source archives are in registry-sources/.\n'
    'SPDX alternatives are retained as declared; unicode-ident also requires Unicode-3.0.\n\n'+
    '\n'.join(p['name']+' '+p['version']+' — '+p['spdx'] for p in records)+'\n',encoding='utf-8')
print(f'PASS {len(records)} exact crate source archives and license bundles')
