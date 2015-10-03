package universe.constellation.orion.viewer;

import universe.constellation.orion.viewer.djvu.DjvuDocument;
import universe.constellation.orion.viewer.pdf.PdfDocument;

import java.io.File;

/**
 * User: mike
 * Date: 19.10.13
 * Time: 10:08
 */
public class FileUtil {

    public static boolean isDjvuFile(String filePathLowCase) {
        return filePathLowCase.endsWith("djvu") || filePathLowCase.endsWith("djv");
    }

    public static DocumentWrapper openFile(String fileName) throws Exception {
        if (FileUtil.isDjvuFile(fileName.toLowerCase())) {
            return new DjvuDocument(fileName);
        } else {
            return new PdfDocument(fileName);
        }
    }

    public static DocumentWrapper openFile(File fileName) throws Exception {
        return openFile(fileName.getAbsolutePath());
    }

}
