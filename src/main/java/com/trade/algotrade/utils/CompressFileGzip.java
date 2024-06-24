package com.trade.algotrade.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class CompressFileGzip {

    public static void main(String[] args) {

        String gzip_filepath = "E:\\Workspace\\code\\algotrade\\log";
        String decopressed_filepath = "E:\\Workspace\\code\\algotrade\\log\\";

        CompressFileGzip gZipFile = new CompressFileGzip();
        File folder = new File(gzip_filepath);
        File[] listOfFiles = folder.listFiles();

        for (File file :
                listOfFiles) {
            if (file.isFile() && file.getName().contains(".gz")) {
                gZipFile.unGunzipFile(file.getPath(), decopressed_filepath.concat(file.getName()));
            } else if (file.isDirectory()) {
                System.out.println("Directory " + file.getName());
            }
        }

    }

    public void unGunzipFile(String compressedFile, String decompressedFile) {

        byte[] buffer = new byte[1024];
        decompressedFile = decompressedFile.replace(".gz", ".txt");

        try {

            FileInputStream fileIn = new FileInputStream(compressedFile);

            GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);

            FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile);

            int bytes_read;

            while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {

                fileOutputStream.write(buffer, 0, bytes_read);
            }

            gZIPInputStream.close();
            fileOutputStream.close();

            System.out.println("The file was decompressed successfully!" + compressedFile);
            File fileToDelete= new File(compressedFile);
            fileToDelete.deleteOnExit();


        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}