from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsHostRelationPolicy.areRelated(" in text:
    print("HTTPS host relation delegation already installed")
    raise SystemExit(0)

old_failure = '''            if (expectedHost.length() == 0 || failedHost.length() == 0
                    || (!sameOrSubDomain(failedHost, expectedHost) && !sameOrSubDomain(expectedHost, failedHost))) {
                return false;
            }
'''
new_failure = '''            if (!HttpsHostRelationPolicy.areRelated(
                    expectedHost,
                    failedHost,
                    MainActivity.this::sameOrSubDomain)) return false;
'''

old_success = '''        boolean related = expectedHost.length() > 0 && finalHost.length() > 0
                && (sameOrSubDomain(finalHost, expectedHost) || sameOrSubDomain(expectedHost, finalHost));
'''
new_success = '''        boolean related = HttpsHostRelationPolicy.areRelated(
                expectedHost,
                finalHost,
                MainActivity.this::sameOrSubDomain);
'''

if text.count(old_failure) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS failure host relation, found {text.count(old_failure)}")
if text.count(old_success) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS success host relation, found {text.count(old_success)}")

text = text.replace(old_failure, new_failure, 1)
text = text.replace(old_success, new_success, 1)

if text.count("HttpsHostRelationPolicy.areRelated(") != 2:
    raise SystemExit("Expected exactly two HTTPS host relation delegations")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS host relations delegated")
