"""Build the addon version checked out in this branch."""
from pathlib import Path
import subprocess
root = Path(__file__).resolve().parents[1]
subprocess.run(['./gradlew', '--offline', 'build'], cwd=root, check=True)
for jar in sorted((root/'build/libs').glob('*.jar')):
    print(jar)
