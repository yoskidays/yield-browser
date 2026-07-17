package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlankCompatibilityReportPolicyTest {
    @Test
    public void missingAndMalformedReportsAreRejected() {
        assertFalse(BlankCompatibilityReportPolicy.isLikelyBlank(null));
        assertFalse(BlankCompatibilityReportPolicy.isLikelyBlank(" "));
        assertFalse(BlankCompatibilityReportPolicy.isLikelyBlank("1|2|3"));
        assertFalse(BlankCompatibilityReportPolicy.isLikelyBlank(
                "x|2|0|0|100|300|complete"));
    }

    @Test
    public void loadingDocumentIsRejected() {
        assertFalse(BlankCompatibilityReportPolicy.isLikelyBlank(
                "0|1|0|0|100|300|loading"));
    }

    @Test
    public void stronglyBlankBoundaryMatches() {
        assertTrue(BlankCompatibilityReportPolicy.isLikelyBlank(
                "8|18|0|99|1600|0|complete"));
    }

    @Test
    public void sparseBlankBoundaryMatchesInteractiveDocument() {
        assertTrue(BlankCompatibilityReportPolicy.isLikelyBlank(
                "20|30|0|8|3000|0|interactive"));
    }

    @Test
    public void viewportBlankBoundaryMatches() {
        assertTrue(BlankCompatibilityReportPolicy.isLikelyBlank(
                "8|40|0|99|9999|300|complete"));
    }

    @Test
    public void contentOrMediaPreventsBlankClassification() {
        assertFalse(BlankCompatibilityReportPolicy.isLikelyBlank(
                "21|31|0|9|3001|299|complete"));
        assertFalse(BlankCompatibilityReportPolicy.isLikelyBlank(
                "0|1|1|0|100|500|complete"));
    }

    @Test
    public void readyStateComparisonIsCaseInsensitive() {
        assertTrue(BlankCompatibilityReportPolicy.isLikelyBlank(
                "0|1|0|0|100|0|COMPLETE"));
    }
}
