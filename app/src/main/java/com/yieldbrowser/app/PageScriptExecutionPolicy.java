package com.yieldbrowser.app;

/** Pure normalization and execution-path selection for page scripts. */
final class PageScriptExecutionPolicy {
    static final class Plan {
        final boolean execute;
        final boolean evaluateJavascript;
        final String payload;

        private Plan(boolean execute, boolean evaluateJavascript, String payload) {
            this.execute = execute;
            this.evaluateJavascript = evaluateJavascript;
            this.payload = payload;
        }
    }

    private PageScriptExecutionPolicy() {
    }

    static Plan plan(String script, boolean evaluateJavascriptSupported) {
        if (script == null || script.length() == 0) {
            return new Plan(false, evaluateJavascriptSupported, "");
        }
        String code = script;
        if (code.startsWith("javascript:")) {
            code = code.substring("javascript:".length());
        }
        return new Plan(
                true,
                evaluateJavascriptSupported,
                evaluateJavascriptSupported ? code : "javascript:" + code);
    }
}
