package com.ds.eventwish.data.cache;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A persistent disk cache using LRU policy
 */
public class DiskLruCache {
    private static final String TAG = "DiskLruCache";
    private final File directory;
    private final long maxSize;
    private long size = 0;
    private final Map<String, Entry> entries = new HashMap<>();
    private final int valueCount;
    private boolean closed = false;
    private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            // Do nothing
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // Do nothing
        }
    };
    
    private DiskLruCache(File directory, long maxSize, int valueCount) {
        this.directory = directory;
        this.maxSize = maxSize;
        this.valueCount = valueCount;
    }
    
    public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize) throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        if (valueCount <= 0) {
            throw new IllegalArgumentException("valueCount <= 0");
        }
        
        // Create directory if needed
        if (!directory.exists()) {
            directory.mkdirs();
            if (!directory.isDirectory()) {
                throw new IOException("Failed to create directory: " + directory);
            }
        }
        
        return new DiskLruCache(directory, maxSize, valueCount);
    }
    
    public Editor edit(String key) throws IOException {
        if (closed) {
            throw new IllegalStateException("Cache is closed");
        }
        
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry(directory, key);
            entries.put(key, entry);
        }
        
        return new Editor(entry);
    }
    
    public InputStream getInputStream(String key, int index) throws IOException {
        if (closed) {
            throw new IllegalStateException("Cache is closed");
        }
        
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        
        File file = entry.getCleanFile(index);
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
    
    public boolean remove(String key) {
        if (closed) {
            throw new IllegalStateException("Cache is closed");
        }
        
        Entry entry = entries.remove(key);
        if (entry == null) {
            return false;
        }
        
        for (int i = 0; i < valueCount; i++) {
            File file = entry.getCleanFile(i);
            size -= file.length();
            file.delete();
        }
        
        return true;
    }
    
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
    }
    
    public void flush() throws IOException {
        if (closed) {
            throw new IllegalStateException("Cache is closed");
        }
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    public long size() {
        return size;
    }
    
    public Snapshot get(String key) throws IOException {
        if (closed) {
            throw new IllegalStateException("Cache is closed");
        }
        
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        
        InputStream[] ins = new InputStream[valueCount];
        for (int i = 0; i < valueCount; i++) {
            ins[i] = new FileInputStream(entry.getCleanFile(i));
        }
        
        return new Snapshot(key, 0, ins, new long[valueCount]);
    }
    
    public Editor edit(String key, long sequenceNumber) throws IOException {
        if (closed) {
            throw new IllegalStateException("Cache is closed");
        }
        
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry(directory, key);
            entries.put(key, entry);
        }
        
        return new Editor(entry);
    }
    
    public void completeEdit(Editor editor, boolean success) throws IOException {
        Entry entry = editor.entry;
        if (success) {
            for (int i = 0; i < valueCount; i++) {
                File dirty = entry.getDirtyFile(i);
                File clean = entry.getCleanFile(i);
                
                if (dirty.exists()) {
                    if (clean.exists()) {
                        size -= clean.length();
                    }
                    size += dirty.length();
                    dirty.renameTo(clean);
                }
            }
        } else {
            // Remove dirty files
            for (int i = 0; i < valueCount; i++) {
                File dirty = entry.getDirtyFile(i);
                dirty.delete();
            }
        }
    }
    
    /**
     * Represents a cached entry on disk
     */
    static class Entry {
        private final File directory;
        private final String key;
        private final File[] cleanFiles;
        private final File[] dirtyFiles;
        private boolean readable = true;
        private Editor currentEditor;
        
        private Entry(File directory, String key) {
            this.directory = directory;
            this.key = key;
            this.cleanFiles = new File[1]; // We're using just one value
            this.dirtyFiles = new File[1];
            
            cleanFiles[0] = new File(directory, key + ".0");
            dirtyFiles[0] = new File(directory, key + ".0.tmp");
        }
        
        public File getCleanFile(int index) {
            return cleanFiles[index];
        }
        
        public File getDirtyFile(int index) {
            return dirtyFiles[index];
        }
    }
    
    /**
     * A snapshot of the values for an entry.
     */
    public final class Snapshot implements Closeable {
        private final String key;
        private final long sequenceNumber;
        private final InputStream[] ins;
        private final long[] lengths;
        
        private Snapshot(String key, long sequenceNumber, InputStream[] ins, long[] lengths) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.ins = ins;
            this.lengths = lengths;
        }
        
        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress.
         */
        public Editor edit() throws IOException {
            return DiskLruCache.this.edit(key, sequenceNumber);
        }
        
        /**
         * Returns the unbuffered stream with the value for {@code index}.
         */
        public InputStream getInputStream(int index) {
            return ins[index];
        }
        
        /**
         * Returns the string value for {@code index}.
         */
        public String getString(int index) throws IOException {
            InputStream in = getInputStream(index);
            if (in == null) {
                return null;
            }
            return inputStreamToString(in);
        }
        
        /**
         * Convert InputStream to String
         */
        private String inputStreamToString(InputStream in) throws IOException {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = bufferedInputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        }
        
        /**
         * Returns the byte length of the value for {@code index}.
         */
        public long getLength(int index) {
            return lengths[index];
        }
        
        @Override
        public void close() {
            for (InputStream in : ins) {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Edits the values for an entry.
     */
    public final class Editor {
        private final Entry entry;
        private final boolean[] written;
        private boolean hasErrors;
        private boolean committed;
        
        private Editor(Entry entry) {
            this.entry = entry;
            this.written = (entry.readable) ? null : new boolean[valueCount];
        }
        
        /**
         * Returns an unbuffered input stream to read the last committed value,
         * or null if no value has been committed.
         */
        public InputStream newInputStream(int index) throws IOException {
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    return null;
                }
                try {
                    return new FileInputStream(entry.getCleanFile(index));
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
        }
        
        /**
         * Returns the last committed value as a string, or null if no value
         * has been committed.
         */
        public String getString(int index) throws IOException {
            InputStream in = newInputStream(index);
            if (in != null) {
                return inputStreamToString(in);
            }
            return null;
        }
        
        /**
         * Convert InputStream to String
         */
        private String inputStreamToString(InputStream in) throws IOException {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = bufferedInputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        }
        
        /**
         * Returns a new unbuffered output stream to write the value at
         * {@code index}. If the underlying output stream encounters errors
         * when writing to the filesystem, this edit will be aborted when
         * {@link #commit} is called. The returned output stream does not throw
         * IOExceptions.
         */
        public OutputStream newOutputStream(int index) throws IOException {
            if (index < 0 || index >= valueCount) {
                throw new IllegalArgumentException("Expected index " + index + " to "
                        + "be greater than 0 and less than the maximum value count "
                        + "of " + valueCount);
            }
            synchronized (DiskLruCache.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    written[index] = true;
                }
                File dirtyFile = entry.getDirtyFile(index);
                FileOutputStream outputStream;
                try {
                    outputStream = new FileOutputStream(dirtyFile);
                } catch (FileNotFoundException e) {
                    // Attempt to recreate the cache directory
                    directory.mkdirs();
                    try {
                        outputStream = new FileOutputStream(dirtyFile);
                    } catch (FileNotFoundException e2) {
                        // We are unable to recover. Silently eat the writes.
                        return NULL_OUTPUT_STREAM;
                    }
                }
                return new FaultHidingOutputStream(outputStream);
            }
        }
        
        /**
         * Sets the value at {@code index} to {@code value}.
         */
        public void set(int index, String value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException("value == null");
            }
            OutputStream outputStream = newOutputStream(index);
            outputStream.write(value.getBytes("UTF-8"));
            outputStream.close();
        }
        
        /**
         * Commits this edit so it is visible to readers.  This releases the
         * edit lock so another edit may be started on the same key.
         */
        public void commit() throws IOException {
            if (hasErrors) {
                completeEdit(this, false);
                remove(entry.key); // The previous entry is stale.
            } else {
                completeEdit(this, true);
            }
            committed = true;
        }
        
        /**
         * Aborts this edit. This releases the edit lock so another edit may be
         * started on the same key.
         */
        public void abort() throws IOException {
            completeEdit(this, false);
        }
        
        public void abortUnlessCommitted() {
            if (!committed) {
                try {
                    abort();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }
        
        private class FaultHidingOutputStream extends FilterOutputStream {
            private FaultHidingOutputStream(OutputStream out) {
                super(out);
            }
            
            @Override
            public void write(int oneByte) {
                try {
                    out.write(oneByte);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }
            
            @Override
            public void write(byte[] buffer, int offset, int length) {
                try {
                    out.write(buffer, offset, length);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }
            
            @Override
            public void close() {
                try {
                    out.close();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }
            
            @Override
            public void flush() {
                try {
                    out.flush();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }
        }
    }
} 