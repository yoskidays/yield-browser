from pathlib import Path

main_path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
main = main_path.read_text(encoding="utf-8")

old_signature = '''        String signature = activeDownloadSection + "|" + activeDownloadSort + "|"
                + downloadSelectMode + "|" + selectedDownloadIds.size() + "|"
                + countActiveDownloads() + "|" + countQueuedDownloads() + "|"
                + downloadQueuePaused + "|" + downloadMaxActive;
'''
new_signature = '''        String signature = activeDownloadSection + "|" + activeDownloadSort + "|"
                + downloadSelectMode + "|" + selectedDownloadIds.size() + "|"
                + countActiveDownloads() + "|" + countQueuedDownloads() + "|"
                + downloadQueuePaused + "|" + downloadMaxActive + "|"
                + countCompletedDownloadHistory();
'''
if old_signature not in main:
    raise SystemExit("Download control signature marker not found")
main = main.replace(old_signature, new_signature, 1)

old_tools = '''        tools.addView(select, selectParams);

        if (downloadSelectMode) {
'''
new_tools = '''        tools.addView(select, selectParams);

        if ("Selesai".equals(activeDownloadSection)
                && !downloadSelectMode
                && countCompletedDownloadHistory() > 0) {
            TextView clearAll = downloadToolButton("Hapus semua");
            clearAll.setTextColor(Color.WHITE);
            clearAll.setBackground(roundRect(Color.parseColor("#E5484D"),
                    dp(18), 0, Color.TRANSPARENT));
            clearAll.setContentDescription("Hapus semua riwayat unduhan selesai");
            clearAll.setOnClickListener(v -> confirmClearCompletedDownloadHistory());
            LinearLayout.LayoutParams clearAllParams = new LinearLayout.LayoutParams(0, dp(40), 1);
            clearAllParams.setMargins(dp(8), 0, 0, 0);
            tools.addView(clearAll, clearAllParams);
        }

        if (downloadSelectMode) {
'''
if old_tools not in main:
    raise SystemExit("Download tool row marker not found")
main = main.replace(old_tools, new_tools, 1)

old_delete = '''    private void deleteSelectedDownloads() {
'''
new_delete = '''    private int countCompletedDownloadHistory() {
        synchronized (downloadItems) {
            return DownloadHistoryClearPolicy.countClearable(downloadItems);
        }
    }

    private void confirmClearCompletedDownloadHistory() {
        int count = countCompletedDownloadHistory();
        if (count <= 0) {
            QuietToast.makeText(this, "Tidak ada riwayat unduhan selesai",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Hapus semua riwayat unduhan?")
                .setMessage(count + " riwayat unduhan selesai akan dihapus dari daftar. "
                        + "File yang sudah tersimpan tetap ada di perangkat.")
                .setPositiveButton("Hapus semua", (dialog, which) -> clearCompletedDownloadHistory())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void clearCompletedDownloadHistory() {
        ArrayList<DownloadItem> removed = new ArrayList<>();
        synchronized (downloadItems) {
            for (int i = downloadItems.size() - 1; i >= 0; i--) {
                DownloadItem item = downloadItems.get(i);
                if (!DownloadHistoryClearPolicy.isClearable(item)) continue;
                item.runGeneration++;
                item.pauseRequested = true;
                item.status = "removed";
                removed.add(item);
                downloadItems.remove(i);
            }
        }

        if (removed.isEmpty()) {
            QuietToast.makeText(this, "Tidak ada riwayat unduhan selesai",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        for (DownloadItem item : removed) {
            stopActiveDownloadTransports(item);
            cancelDownloadNotification(item);
            selectedDownloadIds.remove(item.id);
        }
        downloadSelectMode = false;
        lastDownloadControlsSignature = "";
        saveDownloadHistory();
        renderDownloadList();
        updateDownloadKeepAliveState();
        mainHandler.post(this::pumpDownloadQueue);
        QuietToast.makeText(this, removed.size() + " riwayat unduhan dihapus. File tetap tersimpan.",
                QuietToast.LENGTH_SHORT).show();
    }

    private void deleteSelectedDownloads() {
'''
if old_delete not in main:
    raise SystemExit("Selected download deletion marker not found")
main = main.replace(old_delete, new_delete, 1)
main_path.write_text(main, encoding="utf-8")

policy_path = Path("app/src/main/java/com/yieldbrowser/app/DownloadHistoryClearPolicy.java")
policy_path.write_text('''package com.yieldbrowser.app;

import java.util.List;

/** Rules for clearing finished download records without deleting downloaded files. */
final class DownloadHistoryClearPolicy {
    private DownloadHistoryClearPolicy() {
    }

    static boolean isClearable(DownloadItem item) {
        return item != null && "completed".equals(item.status);
    }

    static int countClearable(List<DownloadItem> items) {
        if (items == null || items.isEmpty()) return 0;
        int count = 0;
        for (DownloadItem item : items) {
            if (isClearable(item)) count++;
        }
        return count;
    }
}
''', encoding="utf-8")

test_path = Path("app/src/test/java/com/yieldbrowser/app/DownloadHistoryClearPolicyTest.java")
test_path.write_text('''package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadHistoryClearPolicyTest {
    private DownloadItem item(int id, String status) {
        return new DownloadItem(id, "https://example.org/file-" + id,
                "file-" + id + ".mp4", "/tmp/file-" + id + ".mp4", status, 100);
    }

    @Test
    public void onlyCompletedItemsAreClearableHistory() {
        assertTrue(DownloadHistoryClearPolicy.isClearable(item(1, "completed")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(item(2, "running")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(item(3, "paused")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(item(4, "failed")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(null));
    }

    @Test
    public void countsOnlyCompletedRecords() {
        assertEquals(2, DownloadHistoryClearPolicy.countClearable(Arrays.asList(
                item(1, "completed"),
                item(2, "running"),
                item(3, "completed"),
                item(4, "failed")
        )));
    }
}
''', encoding="utf-8")

print("Clear download history feature applied")
