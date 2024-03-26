package universe.constellation.orion.viewer.test.framework;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

//Cause AGP logic it's possible to use in android test only platform api, so Java and ContentProvider
public class SimpleFileProvider extends ContentProvider {

    public SimpleFileProvider() {
        super();
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
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
        int pageCount = Integer.parseInt(fileName.substring(firstDot + 1, fileName.indexOf('.', firstDot + 1)));

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

        try {
            return ParcelFileDescriptor.open(file, MODE_READ_ONLY);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
