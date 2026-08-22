from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
tracked = subprocess.check_output(["git", "-C", str(root), "ls-files", "app/src/main"], text=True).splitlines()
files = [root / path for path in tracked if path.endswith((".kt", ".java", ".xml", ".kts")) and (root / path).is_file()]
credential_literal = re.compile(
    r"(?i)(?:stream[-_ ]?key|client_secret|refresh_token|access_token)\s*[:=]\s*['\"][A-Za-z0-9._~+/=-]{8,}['\"]"
)
bearer_literal = re.compile(r"(?i)['\"]Bearer\s+[A-Za-z0-9._~+/=-]{12,}['\"]")
log_call = re.compile(r"\b(?:Log\.(?:v|d|i|w|e)|println)\s*\(")
credential_hits = []
log_hits = []
for path in files:
    for line_number, line in enumerate(path.read_text(errors="replace").splitlines(), 1):
        if (credential_literal.search(line) or bearer_literal.search(line)) and "LEGACY_STREAM_KEY" not in line:
            credential_hits.append(f"{path.relative_to(root)}:{line_number}:{line.strip()}")
        if log_call.search(line):
            log_hits.append(f"{path.relative_to(root)}:{line_number}:{line.strip()}")
print("SUSPICIOUS_CREDENTIAL_LITERAL_COUNT", len(credential_hits))
for hit in credential_hits[:80]:
    print(hit)
print("DIRECT_LOG_CALL_COUNT", len(log_hits))
for hit in log_hits[:120]:
    print(hit)
