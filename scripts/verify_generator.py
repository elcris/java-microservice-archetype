"""End-to-end verification. Python standard library only; needs Java 21 and Maven."""
import argparse
import json
import os
from pathlib import Path
import socket
import shutil
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--maven', help='Maven executable; otherwise use repository wrapper')
parser.add_argument('--repo', help='Optional Maven local repository')
parser.add_argument('--database-it', action='store_true')
args = parser.parse_args()
base = [args.maven] if args.maven else ([str(ROOT/'mvnw.cmd')] if os.name == 'nt' else ['sh', str(ROOT/'mvnw')])
base += ['-B', '-ntp']
if args.repo:
    base += ['-Dmaven.repo.local=' + str(Path(args.repo).resolve())]
(ROOT/'target').mkdir(exist_ok=True)
OUT = Path(tempfile.mkdtemp(prefix='verification-', dir=ROOT/'target'))
summary = {'results': [], 'databaseIntegrationExecuted': args.database_it}


def run(label, arguments, cwd=ROOT, expected=0):
    with (OUT/(label+'.log')).open('w', encoding='utf-8') as log:
        result = subprocess.run(base + arguments, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, timeout=900)
    if result.returncode != expected:
        tail = (OUT/(label+'.log')).read_text(encoding='utf-8', errors='replace')[-10000:]
        raise RuntimeError(f'{label}: exit {result.returncode}\n{tail}')
    print(label + ': PASS', flush=True)


def request(port, path, method='GET', data=None, headers=None):
    body = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(f'http://127.0.0.1:{port}{path}', data=body, method=method,
        headers={'Content-Type': 'application/json', **(headers or {})})
    try:
        response = urllib.request.urlopen(req, timeout=5)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, response.read()


def smoke(project, demo):
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    label = 'smoke-demo' if demo else 'smoke-default'
    command = [shutil.which('java') or 'java', '-jar', str(project/'target/service.jar'), '--server.port='+str(port)]
    if demo:
        command += ['--spring.profiles.active=demo']
    log_path = OUT/(label+'.log')
    with log_path.open('w', encoding='utf-8') as log:
        process = subprocess.Popen(command, cwd=project, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic()+90
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError(label+': service exited; see '+str(log_path))
                try:
                    status, _, body = request(port, '/actuator/health')
                    if status == 200 and json.loads(body)['status'] == 'UP':
                        break
                except (OSError, ValueError):
                    pass
                time.sleep(.5)
            else:
                raise RuntimeError(label+': startup timeout')
            resource = '/api/v1/entries/'+str(uuid.uuid4())
            if not demo:
                assert request(port, resource)[0] == 403
                assert request(port, '/openapi.yaml')[0] == 403
            else:
                payload = {'name': 'smoke-nonsensitive-example'}
                status, headers, body = request(port, resource, 'PUT', payload, {'X-Correlation-ID':'smoke-correlation'})
                assert status == 201, (status, body)
                original = json.loads(body)
                assert headers['Location'] == resource
                assert headers['X-Correlation-ID'] == 'smoke-correlation'
                assert set(original) == {'id', 'name', 'createdAt'}
                assert request(port, resource)[0] == 200
                assert json.loads(request(port, resource)[2]) == original
                status, _, body = request(port, resource, 'PUT', payload)
                assert status == 200 and json.loads(body) == original
                status, _, body = request(port, resource, 'PUT', {'name':'changed'})
                assert status == 409 and json.loads(body)['code'] == 'ENTRY_CONFLICT'
                status, headers, body = request(port, resource, 'PUT', {'name':' '})
                assert status == 400 and json.loads(body)['code'] == 'VALIDATION_ERROR'
                assert headers['Content-Type'].startswith('application/problem+json')
                status, _, body = request(port, '/api/v1/entries/'+str(uuid.uuid4()))
                assert status == 404 and json.loads(body)['code'] == 'ENTRY_NOT_FOUND'
                assert request(port, '/api/v1/entries/invalid')[0] == 400
                status, _, body = request(port, '/openapi.yaml')
                assert status == 200
                assert body == (project/'src/main/resources/openapi/openapi.yaml').read_bytes()
                status, _, body = request(port, '/swagger-ui/index.html')
                assert status == 200 and b'swagger-ui' in body
                status, _, body = request(port, '/v3/api-docs/swagger-config')
                assert status == 200 and json.loads(body)['url'] == '/openapi.yaml'
                assert request(port, '/v3/api-docs')[0] == 404
                assert request(port, '/actuator/health/readiness')[0] == 200
                assert request(port, '/actuator/env')[0] == 403
        finally:
            process.terminate()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
    if demo:
        lines = log_path.read_text(encoding='utf-8', errors='replace').splitlines()
        events = [json.loads(line) for line in lines if line.startswith('{')]
        assert any(e.get('message') == 'http.request.completed' and e.get('correlationId') == 'smoke-correlation' for e in events)
        assert all('smoke-nonsensitive-example' not in json.dumps(e) for e in events)
    print(label+': PASS', flush=True)


def counts(project):
    result = {'tests':0, 'failures':0, 'errors':0, 'skipped':0}
    for report in (project/'target').glob('*surefire-reports/TEST-*.xml'):
        root = ET.parse(report).getroot()
        for key in result:
            result[key] += int(root.attrib.get(key, 0))
    for report in (project/'target/failsafe-reports').glob('TEST-*.xml'):
        root = ET.parse(report).getroot()
        for key in result:
            result[key] += int(root.attrib.get(key, 0))
    return result


try:
    run('install-archetype', ['install'])
    for engine, group, package in [
        ('memory','org.verification','org.verification.alpha'),
        ('postgresql','net.verification','net.verification.beta'),
        ('dynamodb','io.verification','io.verification.gamma')]:
        artifact = 'service-'+engine
        run('generate-'+engine, ['org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate',
            '-DarchetypeCatalog=local', '-DarchetypeGroupId=co.com.bmm.platform',
            '-DarchetypeArtifactId=java-microservice-archetype', '-DarchetypeVersion=1.0.0',
            '-DinteractiveMode=false', '-DgroupId='+group, '-DartifactId='+artifact,
            '-Dpackage='+package, '-Dversion=0.1.0-SNAPSHOT', '-Dpersistence='+engine,
            '-DoutputDirectory='+str(OUT)])
        project = OUT/artifact
        for path in project.rglob('*'):
            if path.is_file() and path.suffix in {'.java','.xml','.yaml','.md','.config'}:
                text = path.read_text(encoding='utf-8')
                for unresolved in ['${package}', '${artifactId}', '${groupId}', '${symbol_dollar}', '@@PACKAGE@@']:
                    assert unresolved not in text, (path, unresolved)
        assert (project/'.mvn/maven.config').read_text().strip() == '-Dstorage='+engine
        assert (project/'src/main/java'/Path(package.replace('.', '/'))/'ServiceApplication.java').is_file()
        # Exercise persisted generation choice, without passing -Dstorage again.
        arguments = ['clean','verify']
        if engine != 'memory':
            arguments += ['-Pdatabase-it']
            if not args.database_it:
                arguments += ['-DskipITs=true']
        run('verify-'+engine, arguments, project)
        with zipfile.ZipFile(project/'target/service.jar') as jar:
            libs = '\n'.join(name for name in jar.namelist() if name.startswith('BOOT-INF/lib/'))
            assert ('/postgresql-' in libs) == (engine == 'postgresql')
            assert ('/flyway-core-' in libs) == (engine == 'postgresql')
            assert ('/dynamodb-' in libs) == (engine == 'dynamodb')
            assert '/h2-' not in libs and '/kafka-clients-' not in libs
        report = counts(project)
        assert report['tests'] > 0 and not report['failures'] and not report['errors']
        summary['results'].append({'engine':engine, **report})
        if engine == 'memory':
            smoke(project, False)
            smoke(project, True)
    # Check compile-time switch from a generated DB default back to a dependency-free core.
    project = OUT/'service-postgresql'
    run('switch-to-memory', ['-Dstorage=memory','clean','verify'], project)
    with zipfile.ZipFile(project/'target/service.jar') as jar:
        assert not any('/postgresql-' in name or '/flyway-core-' in name for name in jar.namelist())
    summary['switchToMemory'] = counts(project)
    summary['status'] = 'PASS'
finally:
    (OUT/'summary.json').write_text(json.dumps(summary, indent=2)+'\n', encoding='utf-8')
    print('Evidence: '+str(OUT), flush=True)
