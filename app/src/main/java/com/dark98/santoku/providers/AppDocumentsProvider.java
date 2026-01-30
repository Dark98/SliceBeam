package com.dark98.santoku.providers;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

public class AppDocumentsProvider extends DocumentsProvider {
    private static final String ROOT_ID = "app_files";
    private static final String ROOT_DOC_ID = "app_files:";

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        final String[] proj = projection != null ? projection : DEFAULT_ROOT_PROJECTION;
        MatrixCursor cursor = new MatrixCursor(proj);

        File base = getBaseDir();
        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID);
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOC_ID);
        row.add(DocumentsContract.Root.COLUMN_TITLE, "Santoku Files");
        int flags = DocumentsContract.Root.FLAG_SUPPORTS_CREATE
                | DocumentsContract.Root.FLAG_LOCAL_ONLY;
        row.add(DocumentsContract.Root.COLUMN_FLAGS, flags);
        row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, base.getFreeSpace());
        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        File file = fileForDocId(documentId);
        includeFile(cursor, documentId, file);
        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        File parent = fileForDocId(parentDocumentId);
        File[] files = parent.listFiles();
        if (files != null) {
            for (File file : files) {
                includeFile(cursor, docIdForFile(file), file);
            }
        }
        return cursor;
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        File file = fileForDocId(documentId);
        return getMimeType(file);
    }

    @Override
    public android.os.ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        File file = fileForDocId(documentId);
        int accessMode = ParcelFileDescriptorUtil.parseMode(mode);
        return android.os.ParcelFileDescriptor.open(file, accessMode);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        File parent = fileForDocId(parentDocumentId);
        if (!parent.isDirectory()) {
            throw new FileNotFoundException("Parent is not a directory: " + parentDocumentId);
        }

        String name = displayName;
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
            File dir = new File(parent, name);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new FileNotFoundException("Failed to create directory: " + name);
            }
            return docIdForFile(dir);
        }

        if (!hasExtension(name)) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ext != null && !ext.isEmpty()) {
                name = name + "." + ext.toLowerCase(Locale.US);
            }
        }

        File file = new File(parent, name);
        try {
            if (!file.exists() && !file.createNewFile()) {
                throw new FileNotFoundException("Failed to create file: " + name);
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to create file: " + e.getMessage());
        }
        return docIdForFile(file);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        File file = fileForDocId(documentId);
        deleteRecursively(file);
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        File file = fileForDocId(documentId);
        File target = new File(file.getParentFile(), displayName);
        if (!file.renameTo(target)) {
            throw new FileNotFoundException("Failed to rename to: " + displayName);
        }
        return docIdForFile(target);
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        try {
            File parent = fileForDocId(parentDocumentId);
            File doc = fileForDocId(documentId);
            return isDescendant(parent, doc);
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        File base = getBaseDir();
        File[] files = base.listFiles();
        if (files != null) {
            for (File file : files) {
                includeFile(cursor, docIdForFile(file), file);
            }
        }
        return cursor;
    }

    private File getBaseDir() {
        File dir = getContext().getFilesDir();
        if (dir == null) {
            dir = new File(Environment.getDataDirectory(), "data");
        }
        return dir;
    }

    private void includeFile(MatrixCursor cursor, String docId, File file) {
        int flags = 0;
        if (file.isDirectory()) {
            if (file.canWrite()) {
                flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
            }
        } else {
            if (file.canWrite()) {
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
            }
        }

        String mime = getMimeType(file);
        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId);
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.isDirectory() ? null : file.length());
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mime);
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified());
    }

    private String getMimeType(File file) {
        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            String ext = name.substring(dot + 1).toLowerCase(Locale.US);
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (type != null) {
                return type;
            }
        }
        return "application/octet-stream";
    }

    private String docIdForFile(File file) {
        File base = getBaseDir();
        String basePath = base.getAbsolutePath();
        String path = file.getAbsolutePath();
        if (path.equals(basePath)) {
            return ROOT_DOC_ID;
        }
        if (path.startsWith(basePath + File.separator)) {
            String rel = path.substring(basePath.length() + 1);
            return ROOT_DOC_ID + rel;
        }
        return ROOT_DOC_ID;
    }

    private File fileForDocId(String docId) throws FileNotFoundException {
        File base = getBaseDir();
        if (ROOT_DOC_ID.equals(docId) || ROOT_ID.equals(docId)) {
            return base;
        }
        if (!docId.startsWith(ROOT_DOC_ID)) {
            throw new FileNotFoundException("Unknown docId: " + docId);
        }
        String rel = docId.substring(ROOT_DOC_ID.length());
        File target = new File(base, rel);
        try {
            File canonical = target.getCanonicalFile();
            File canonicalBase = base.getCanonicalFile();
            if (!isDescendant(canonicalBase, canonical)) {
                throw new FileNotFoundException("Invalid path: " + docId);
            }
            return canonical;
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to resolve: " + docId);
        }
    }

    private boolean isDescendant(File parent, File child) {
        try {
            String parentPath = parent.getCanonicalPath();
            String childPath = child.getCanonicalPath();
            return childPath.equals(parentPath) || childPath.startsWith(parentPath + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteRecursively(File file) throws FileNotFoundException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new FileNotFoundException("Failed to delete: " + file.getName());
        }
    }

    private boolean hasExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1;
    }

    private static final class ParcelFileDescriptorUtil {
        private ParcelFileDescriptorUtil() {}

        static int parseMode(String mode) {
            if ("r".equals(mode)) {
                return android.os.ParcelFileDescriptor.MODE_READ_ONLY;
            }
            if ("w".equals(mode) || "wt".equals(mode)) {
                return android.os.ParcelFileDescriptor.MODE_WRITE_ONLY
                        | android.os.ParcelFileDescriptor.MODE_CREATE
                        | android.os.ParcelFileDescriptor.MODE_TRUNCATE;
            }
            if ("wa".equals(mode)) {
                return android.os.ParcelFileDescriptor.MODE_WRITE_ONLY
                        | android.os.ParcelFileDescriptor.MODE_CREATE
                        | android.os.ParcelFileDescriptor.MODE_APPEND;
            }
            if ("rw".equals(mode)) {
                return android.os.ParcelFileDescriptor.MODE_READ_WRITE
                        | android.os.ParcelFileDescriptor.MODE_CREATE;
            }
            if ("rwt".equals(mode)) {
                return android.os.ParcelFileDescriptor.MODE_READ_WRITE
                        | android.os.ParcelFileDescriptor.MODE_CREATE
                        | android.os.ParcelFileDescriptor.MODE_TRUNCATE;
            }
            return android.os.ParcelFileDescriptor.MODE_READ_ONLY;
        }
    }
}
