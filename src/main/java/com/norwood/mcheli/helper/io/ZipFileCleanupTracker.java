package com.norwood.mcheli.helper.io;

import com.norwood.mcheli.helper.MCH_Logger;

import java.io.File;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipFile;

public final class ZipFileCleanupTracker {

    private static final ReferenceQueue<Object> REFERENCE_QUEUE = new ReferenceQueue<>();
    private static final Set<CleanupReference> LIVE_REFERENCES = ConcurrentHashMap.newKeySet();

    static {
        Thread cleanupThread = new Thread(ZipFileCleanupTracker::processQueue, "MCH-ZipFileCleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private ZipFileCleanupTracker() {
    }

    public static void track(Object owner, AtomicReference<ZipFile> zipFileRef, File source) {
        LIVE_REFERENCES.add(new CleanupReference(owner, zipFileRef, source));
    }

    public static void close(AtomicReference<ZipFile> zipFileRef) throws IOException {
        ZipFile zipFile = zipFileRef.getAndSet(null);
        if (zipFile != null) {
            zipFile.close();
        }
    }

    private static void processQueue() {
        while (true) {
            try {
                CleanupReference reference = (CleanupReference) REFERENCE_QUEUE.remove();
                reference.cleanup();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable t) {
                MCH_Logger.error("[ZipFileCleanupTracker] Unexpected cleanup failure", t);
            }
        }
    }

    private static final class CleanupReference extends PhantomReference<Object> {

        private final AtomicReference<ZipFile> zipFileRef;
        private final File source;

        private CleanupReference(Object owner, AtomicReference<ZipFile> zipFileRef, File source) {
            super(owner, REFERENCE_QUEUE);
            this.zipFileRef = zipFileRef;
            this.source = source;
        }

        private void cleanup() {
            LIVE_REFERENCES.remove(this);
            clear();

            try {
                ZipFileCleanupTracker.close(this.zipFileRef);
            } catch (IOException e) {
                MCH_Logger.error("[ZipFileCleanupTracker] Failed to close leaked zip file: " + this.source, e);
            }
        }
    }
}
