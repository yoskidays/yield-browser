package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class DownloadProtocolTest {
    @Test
    public void parsesValidContentRange() {
        DownloadProtocol.RangeInfo range = DownloadProtocol.parseContentRange("bytes 100-199/1000");
        assertEquals(100, range.start);
        assertEquals(199, range.end);
        assertEquals(1000, range.total);
    }

    @Test
    public void rejectsMalformedContentRange() {
        assertNull(DownloadProtocol.parseContentRange("bytes 200-100/1000"));
        assertNull(DownloadProtocol.parseContentRange("items 0-1/2"));
    }

    @Test
    public void expectedLengthIsInclusive() {
        assertEquals(100, DownloadProtocol.expectedLength(100, 199));
        assertEquals(0, DownloadProtocol.expectedLength(5, 4));
    }
}
