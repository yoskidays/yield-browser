package com.yieldbrowser.app;

/** Pure ordered short-circuit evaluation of compatibility navigation signals. */
final class CompatibilityNavigationContextPolicy {
    interface Evaluator {
        boolean evaluate();
    }

    private CompatibilityNavigationContextPolicy() {
    }

    static boolean any(Evaluator... evaluators) {
        try {
            if (evaluators == null) return false;
            for (Evaluator evaluator : evaluators) {
                if (evaluator != null && evaluator.evaluate()) return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }
}
