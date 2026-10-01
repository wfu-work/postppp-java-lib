#!/usr/bin/env python3
"""Run all SDK tests on Linux amd64 and archive reproducible report evidence."""
import argparse
import datetime as dt
import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--image', default='maven:3.9-eclipse-temurin-21')
    parser.add_argument('--date', default=dt.date.today().isoformat())
    args = parser.parse_args()
    evidence = ROOT / 'docs' / 'test-evidence' / args.date
    evidence.mkdir(parents=True, exist_ok=True)
    cmd = ['docker', 'run', '--rm', '--platform', 'linux/amd64',
           '-v', f'{ROOT}:/workspace', '-v', f'{Path.home() / ".m2"}:/root/.m2',
           '-w', '/workspace', args.image, 'mvn', '-B', '-o', 'clean', 'verify']
    start = dt.datetime.now(dt.timezone.utc)
    run = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (evidence / 'maven-verify-linux-amd64.log').write_text(run.stdout, encoding='utf-8')
    print(run.stdout)
    if run.returncode:
        raise SystemExit(f'Maven failed ({run.returncode}); see evidence log')
    suites, cases = [], []
    for path in sorted((ROOT / 'target' / 'surefire-reports').glob('TEST-*.xml')):
        tree = ET.parse(path)
        node = tree.getroot()
        suites.append({key: node.get(key) for key in ['name', 'tests', 'failures', 'errors', 'skipped', 'time']})
        for case in node.findall('testcase'):
            status = 'passed'
            if case.find('failure') is not None or case.find('error') is not None:
                status = 'failed'
            elif case.find('skipped') is not None:
                status = 'skipped'
            cases.append({'class': case.get('classname'), 'name': case.get('name'),
                          'seconds': float(case.get('time', '0')), 'status': status})
        # Archive assertion outcomes without machine-specific classpath/user properties.
        props = node.find('properties')
        if props is not None:
            node.remove(props)
        tree.write(evidence / path.name, encoding='utf-8', xml_declaration=True)
    totals = {key: sum(int(s.get(key) or 0) for s in suites) for key in ['tests', 'failures', 'errors', 'skipped']}
    if not suites or totals['tests'] < 12 or any(totals[k] for k in ['failures', 'errors', 'skipped']):
        raise SystemExit(f'Report gate failed: {totals}; require all 12+ tests to execute and pass')
    for path in (ROOT / 'target' / 'surefire-reports').glob('*.txt'):
        (evidence / path.name).write_bytes(path.read_bytes())
    jar = ROOT / 'target' / 'postppp-java-lib-1.0.0.jar'
    native = ROOT / 'src/main/resources/linux-x86-64/libPostppp.so'
    image = subprocess.check_output(['docker', 'image', 'inspect', args.image], text=True)
    image_info = json.loads(image)[0]
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    summary = {'report_date': args.date, 'start_utc': start.isoformat(),
               'end_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
               'command': cmd, 'git_head': head, 'image': args.image, 'image_id': image_info['Id'],
               'runtime_os': 'Linux', 'runtime_arch': 'amd64', 'totals': totals,
               'suites': suites, 'cases': cases,
               'native_sha256': sha(native), 'native_bytes': native.stat().st_size,
               'jar_sha256': sha(jar), 'jar_bytes': jar.stat().st_size,
               'scope': 'Java SDK contract and native ABI smoke tests; no real PPP accuracy acceptance'}
    (evidence / 'summary.json').write_text(json.dumps(summary, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    source = {str(p.relative_to(ROOT)): sha(p) for folder in ['src/main/java', 'src/test/java']
              for p in sorted((ROOT / folder).rglob('*.java'))}
    source['pom.xml'] = sha(ROOT / 'pom.xml')
    (evidence / 'source-baseline.json').write_text(json.dumps(source, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    hashes = {p.name: sha(p) for p in sorted(evidence.iterdir()) if p.is_file() and p.name != 'sha256.json'}
    (evidence / 'sha256.json').write_text(json.dumps(hashes, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'Report gate passed: {totals}; evidence at {evidence}')

if __name__ == '__main__':
    main()
