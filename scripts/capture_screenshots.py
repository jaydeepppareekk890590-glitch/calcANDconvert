from pathlib import Path
import os
import shutil
import subprocess
import time

root = Path(__file__).resolve().parents[1]
sdk = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk')))
adb = shutil.which('adb') or str(sdk / 'platform-tools/adb')
names = {'unit-picker', 'regional-area', 'landscape', 'calculator', 'history', 'validation', 'angle', 'area', 'data', 'energy', 'force', 'fuel', 'length', 'power', 'pressure', 'speed', 'temperature', 'time', 'volume', 'weight'}
out = root / 'docs/screenshots'
out.mkdir(parents=True, exist_ok=True)
subprocess.run([adb, 'shell', 'run-as', 'com.example.calculator', 'rm', '-rf', 'files/screenshots'], capture_output=True)
log = root / 'build/screenshot-test.log'
log.parent.mkdir(exist_ok=True)
with log.open('w') as stream:
    process = subprocess.Popen([str(root / 'gradlew'), 'connectedDebugAndroidTest', '-Pandroid.testInstrumentationRunnerArguments.class=com.example.calculator.CombinedAppTest', '--rerun-tasks', '--console=plain'], cwd=root, stdout=stream, stderr=subprocess.STDOUT)
    remaining = set(names)
    while process.poll() is None:
        for name in tuple(remaining):
            result = subprocess.run([adb, 'exec-out', 'run-as', 'com.example.calculator', 'cat', f'files/screenshots/{name}.png'], capture_output=True)
            if result.stdout.startswith(b'\x89PNG') and result.stdout.endswith(b'IEND\xaeB`\x82'):
                (out / f'{name}.png').write_bytes(result.stdout)
                remaining.remove(name)
                print(f'Captured {name}', flush=True)
        time.sleep(.2)
if process.returncode or remaining:
    raise SystemExit(f'Test failed or screenshots missing: {sorted(remaining)}. See {log}')
print('All screenshots refreshed; emulator tests passed.')
