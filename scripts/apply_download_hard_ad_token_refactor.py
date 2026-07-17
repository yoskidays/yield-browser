from pathlib import Path

main_path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
policy_path = Path("app/src/main/java/com/yieldbrowser/app/DownloadUrlPolicy.java")
main_text = main_path.read_text(encoding="utf-8")
policy_text = policy_path.read_text(encoding="utf-8")

policy_call = "DownloadUrlPolicy.hasHardAdClickToken(u)"
policy_method = "static boolean hasHardAdClickToken(String u)"

if policy_method not in policy_text:
    anchor = '''    static String normalizeGoogleDriveDownloadUrl(String value) {
'''
    addition = '''    static boolean hasHardAdClickToken(String u) {
        if (u == null) return false;
        return u.contains("adclick")
                || u.contains("ad_click")
                || u.contains("adurl=")
                || u.contains("click_id")
                || u.contains("af_click")
                || u.contains("clickunder")
                || u.contains("popunder")
                || u.contains("popupads")
                || u.contains("onclickads")
                || u.contains("interstitial")
                || u.contains("utm_medium=affiliates")
                || u.contains("deep_and_deferred")
                || u.contains("navigate_url=")
                || u.contains("reactpath");
    }

'''
    if policy_text.count(anchor) != 1:
        raise SystemExit(
            f"Expected one DownloadUrlPolicy insertion anchor, found {policy_text.count(anchor)}")
    policy_text = policy_text.replace(anchor, addition + anchor, 1)

if policy_call not in main_text:
    old_method = '''    private boolean hasHardAdClickToken(String u) {
        if (u == null) return false;
        return u.contains("adclick")
                || u.contains("ad_click")
                || u.contains("adurl=")
                || u.contains("click_id")
                || u.contains("af_click")
                || u.contains("clickunder")
                || u.contains("popunder")
                || u.contains("popupads")
                || u.contains("onclickads")
                || u.contains("interstitial")
                || u.contains("utm_medium=affiliates")
                || u.contains("deep_and_deferred")
                || u.contains("navigate_url=")
                || u.contains("reactpath");
    }
'''
    new_method = '''    private boolean hasHardAdClickToken(String u) {
        return DownloadUrlPolicy.hasHardAdClickToken(u);
    }
'''
    if main_text.count(old_method) != 1:
        raise SystemExit(
            f"Expected one legacy hard-ad token method, found {main_text.count(old_method)}")
    main_text = main_text.replace(old_method, new_method, 1)

if policy_text.count(policy_method) != 1:
    raise SystemExit("Expected exactly one DownloadUrlPolicy hard-ad classifier")
if main_text.count(policy_call) != 1:
    raise SystemExit("Expected exactly one MainActivity hard-ad delegation")

policy_path.write_text(policy_text, encoding="utf-8")
main_path.write_text(main_text, encoding="utf-8")
print("Hard-ad click token classification centralized in DownloadUrlPolicy")
