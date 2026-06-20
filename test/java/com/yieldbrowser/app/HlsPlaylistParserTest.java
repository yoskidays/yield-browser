package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class HlsPlaylistParserTest {
    @Test
    public void sortsMasterVariantsByBandwidth() throws Exception {
        String text = "#EXTM3U\n"
                + "#EXT-X-STREAM-INF:BANDWIDTH=800000\nlow/index.m3u8\n"
                + "#EXT-X-STREAM-INF:BANDWIDTH=2400000\nhigh/index.m3u8\n";
        HlsPlaylistParser.Playlist playlist = HlsPlaylistParser.parse(
                "https://cdn.example.com/master.m3u8", text);
        assertEquals(2, playlist.variants.size());
        assertEquals("https://cdn.example.com/high/index.m3u8", playlist.variants.get(0).url);
    }

    @Test
    public void parsesInitMapAndByteRanges() throws Exception {
        String text = "#EXTM3U\n"
                + "#EXT-X-MAP:URI=\"file.mp4\",BYTERANGE=\"100@0\"\n"
                + "#EXT-X-BYTERANGE:200@100\nfile.mp4\n"
                + "#EXT-X-BYTERANGE:300\nfile.mp4\n";
        HlsPlaylistParser.Playlist playlist = HlsPlaylistParser.parse(
                "https://cdn.example.com/v/index.m3u8", text);
        assertNotNull(playlist.initMap);
        assertEquals(0, playlist.initMap.range.start);
        assertEquals(99, playlist.initMap.range.end);
        assertEquals(2, playlist.segments.size());
        assertEquals(100, playlist.segments.get(0).range.start);
        assertEquals(299, playlist.segments.get(0).range.end);
        assertEquals(300, playlist.segments.get(1).range.start);
        assertEquals(599, playlist.segments.get(1).range.end);
        assertFalse(playlist.encrypted);
    }

    @Test
    public void detectsEncryptedPlaylist() throws Exception {
        HlsPlaylistParser.Playlist playlist = HlsPlaylistParser.parse(
                "https://cdn.example.com/index.m3u8",
                "#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\nseg.ts\n");
        assertTrue(playlist.encrypted);
        assertFalse(playlist.unsupportedEncryption);
        assertNotNull(playlist.segments.get(0).key);
        assertEquals(0, playlist.segments.get(0).sequence);
    }

    @Test
    public void rejectsSampleAes() throws Exception {
        HlsPlaylistParser.Playlist playlist = HlsPlaylistParser.parse(
                "https://cdn.example.com/index.m3u8",
                "#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"key.bin\"\nseg.ts\n");
        assertTrue(playlist.unsupportedEncryption);
    }
}
