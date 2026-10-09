#!/usr/bin/env python3
"""Stage locked official GTNH development artifacts and RFG, hash verified.

Python standard library only. Writes exclusively to this project's .reference/maven.
Does not install packages, alter a game instance, or fetch moving branches.
"""
import hashlib
import os
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = ROOT / '.reference' / 'maven'
ARTIFACTS = (
    ('GT5-Unofficial', '5.09.51.482', 'gregtech-5.09.51.482-dev.jar',
     '18f6996e74fda161cf82bf27d5cf9b59c7f5762fca1f53ee438618aa65c766f1'),
    ('StructureLib', '1.4.23', 'structurelib-1.4.23-dev.jar',
     '3184cc1f94644e9409f8835a8e42e997bfc625e6449aa2fae76604642d0a963b'),
    ('GTNHLib', '0.7.10', 'gtnhlib-0.7.10-dev.jar',
     'ab07604d386ae98f64af96fd0a74cbdd8c33a983f284b39013a6b2fdb369a622'),
    ('ModularUI', '1.2.20', 'modularui-1.2.20-dev.jar',
     'eda1500d1465aa5abf7a48c2ec7dd8799d416dd77b6d46cc2672d5c3cd1b9538'),
)


def valid(path, digest):
    return (path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest() == digest
            and zipfile.is_zipfile(path))


def stage(artifact, version, asset, digest, group='com.github.GTNewHorizons',
          repository=None, classifier='dev'):
    directory = REPOSITORY / group.replace('.', '/') / artifact / version
    directory.mkdir(parents=True, exist_ok=True)
    suffix = '-' + classifier if classifier else ''
    target = directory / (artifact + '-' + version + suffix + '.jar')
    if not valid(target, digest):
        url = 'https://github.com/GTNewHorizons/{}/releases/download/{}/{}'.format(
            repository or artifact, version, asset)
        temporary = target.with_suffix('.jar.part')
        for attempt in range(3):
            try:
                request = urllib.request.Request(url, headers={'User-Agent': 'FluxArc-dependency-bootstrap/1'})
                with urllib.request.urlopen(request, timeout=120) as response, temporary.open('wb') as output:
                    while True:
                        chunk = response.read(1024 * 1024)
                        if not chunk:
                            break
                        output.write(chunk)
                if not valid(temporary, digest):
                    raise ValueError('SHA256/ZIP validation failed: ' + asset)
                os.replace(temporary, target)
                break
            except Exception:
                if temporary.exists():
                    temporary.unlink()
                if attempt == 2:
                    raise
    pom = '<project><modelVersion>4.0.0</modelVersion><groupId>{}</groupId>'.format(group)
    pom += '<artifactId>{}</artifactId><version>{}</version></project>'.format(artifact, version)
    (directory / (artifact + '-' + version + '.pom')).write_text(pom, encoding='utf-8')
    print('{}:{} SHA256 {}'.format(artifact, version, digest))


if __name__ == '__main__':
    for artifact in ARTIFACTS:
        stage(*artifact)
    stage('retrofuturagradle', '1.4.1', 'retrofuturagradle-1.4.1.jar',
          '78831fef9733beba60c37b306a1d084e18db68a9c93efaf20f29c3f416d07e6f',
          group='com.gtnewhorizons', repository='RetroFuturaGradle', classifier='')
    plugin = 'com.gtnewhorizons.retrofuturagradle'
    marker = plugin + '.gradle.plugin'
    directory = REPOSITORY / plugin.replace('.', '/') / marker / '1.4.1'
    directory.mkdir(parents=True, exist_ok=True)
    pom = ('<project><modelVersion>4.0.0</modelVersion><groupId>' + plugin + '</groupId>'
           '<artifactId>' + marker + '</artifactId><version>1.4.1</version>'
           '<dependencies><dependency><groupId>com.gtnewhorizons</groupId>'
           '<artifactId>retrofuturagradle</artifactId><version>1.4.1</version>'
           '</dependency></dependencies></project>')
    (directory / (marker + '-1.4.1.pom')).write_text(pom, encoding='utf-8')
