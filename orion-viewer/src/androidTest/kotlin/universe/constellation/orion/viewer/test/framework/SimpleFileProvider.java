package universe.constellation.orion.viewer.test.framework;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

//Cause AGP logic it's possible to use in android test only platform api, so Java and ContentProvider
public class SimpleFileProvider extends ContentProvider {

    private final Map<String, Integer> file2Error = new HashMap<>();

    public SimpleFileProvider() {
        super();
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    private static final String NETWORK_MARKER = ".network.";

    /** Like {@link #NETWORK_MARKER}, without the network, but a projection with _data is rejected. */
    private static final String NO_DATA_MARKER = ".nodata.";

    public static final String METHOD_QUERY_COUNT = "queryCount";

    public static final String METHOD_RESET_QUERY_COUNT = "resetQueryCount";

    public static final String KEY_COUNT = "count";

    /** Every query, for tests counting them: the provider runs in the test apk's own process. */
    private final AtomicInteger queries = new AtomicInteger();

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (METHOD_RESET_QUERY_COUNT.equals(method)) {
            queries.set(0);
            return null;
        }
        if (METHOD_QUERY_COUNT.equals(method)) {
            Bundle result = new Bundle();
            result.putInt(KEY_COUNT, queries.get());
            return result;
        }
        return super.call(method, arg, extras);
    }

    /**
     * Metadata is served only for names with a marker. With the {@link #NETWORK_MARKER} it stands
     * for one over a network share (Material Files on SMB, say), whose query() does a round trip
     * to the server, and answers with the path of the test book named by the rest, e.g.
     * sicp.network.pdf gives sicp.pdf.
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        queries.incrementAndGet();
        String fileName = uri.getLastPathSegment();
        if (fileName == null) {
            return null;
        }
        String book;
        if (fileName.contains(NETWORK_MARKER)) {
            touchNetwork();
            book = fileName.replace(NETWORK_MARKER, ".");
        } else if (fileName.contains(NO_DATA_MARKER)) {
            if (projection == null || Arrays.asList(projection).contains("_data")) {
                throw new IllegalArgumentException("Invalid column _data");
            }
            book = fileName.replace(NO_DATA_MARKER, ".");
        } else {
            return null;
        }

        File file = new File(Environment.getExternalStorageDirectory(), "Download/orion/testData/" + book);
        String[] columns = projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, "_data"};
        MatrixCursor cursor = new MatrixCursor(columns);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            switch (columns[i]) {
                case OpenableColumns.DISPLAY_NAME: row[i] = book; break;
                case OpenableColumns.SIZE: row[i] = file.length(); break;
                case "_data": row[i] = file.getAbsolutePath(); break;
                default: row[i] = null;
            }
        }
        cursor.addRow(row);
        return cursor;
    }

    /**
     * Resolves a server name, as a provider over a network share does first. StrictMode checks
     * the lookup before it starts, and the caller's thread policy travels with the binder call:
     * queried from the viewer's main thread this throws NetworkOnMainThreadException. Otherwise
     * the lookup just fails: the name is unresolvable and the test apk has no INTERNET permission.
     */
    private static void touchNetwork() {
        try {
            InetAddress.getAllByName("network-share.invalid");
        } catch (IOException | SecurityException ignored) {
            //only the attempt matters
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        PdfDocument document = new PdfDocument();
        String fileName = uri.getLastPathSegment();
        assert fileName != null;
        System.out.println(fileName);
        int firstDot = fileName.indexOf('.');
        String middlePart = fileName.substring(firstDot + 1, fileName.indexOf('.', firstDot + 1));
        int pageCount = 101;
        if (middlePart.startsWith("error")) {
            if (middlePart.equals("error")) {
                throw new FileNotFoundException(fileName);
            } else {
                Integer value = file2Error.get(fileName);
                if (value == null || value == 0) {
                    file2Error.put(fileName, 1);
                } else {
                    throw new FileNotFoundException(fileName);
                }
            }
        } else {
            try {
                pageCount = Integer.parseInt(middlePart);
            } catch (NumberFormatException e) {
                //a marker like "network" or "nodata": the default count
            }
        }

        for (int i = 1; i <= pageCount; i++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600, 800, i).create();        // start a page
            PdfDocument.Page page = document.startPage(pageInfo);
            document.finishPage(page);
        }

        File file = new File(Objects.requireNonNull(getContext()).getCacheDir(), fileName);
        file.delete();

        try (OutputStream fileOutputStream = new FileOutputStream(file)) {
            document.writeTo(fileOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            document.close();
        }
        System.out.println("Created new file: " + document + " size " + file.length());

        try {
            return ParcelFileDescriptor.open(file, MODE_READ_ONLY);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
