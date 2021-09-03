package utils;

import soot.CompilationDeathException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DexPrinter extends soot.toDex.DexPrinter {
    private Path outputDir;

    public DexPrinter() {
        super();
        outputDir = null;
    }

    public DexPrinter(String outputPath, String folder) {
        super();
        String path = outputPath + "/" + folder;
        try {
            outputDir = Files.createDirectory(Paths.get(path));
        } catch (IOException e) {
            outputDir = Paths.get(path);
        }
    }

    /**
     * Write the dex bytes to a specific file
     */
    public void writeToFile() {
        try {
            dexBuilder.writeTo(outputDir.toString());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error in writeToFile function. Message: " + e.getMessage());
        }
    }

    public byte[] retrieveByteArray() {
        try {
            boolean isTmpDirectory = false;
            if (outputDir == null) {
                Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
                outputDir = Files.createTempDirectory(tmpDir, "dexbytesprinter-temp-");
                isTmpDirectory = true;
            }

            // write to file
            this.writeToFile();

            // Read file to byte array
            File outDirFile = new File(outputDir.toString());
            File[] outDexFiles = outDirFile.listFiles();
            assert outDexFiles != null && outDexFiles.length == 1;

            File dexFile = outDexFiles[0];
            byte[] res = new byte[(int) dexFile.length()];
            FileInputStream fis = new FileInputStream(dexFile);
            int read = fis.read(res);
            assert res.length == read;

            // Close and delete file, delete temporary directory
            fis.close();

            // delete file and folder if it is temporary
            if (isTmpDirectory) {
                boolean fileDelSuccess = dexFile.delete();
                assert fileDelSuccess;
                Files.delete(outputDir);
            }
            return res;
        } catch (IOException e) {
            throw new CompilationDeathException("I/O exception while printing dex: " + e.getMessage());
        }
    }

}
