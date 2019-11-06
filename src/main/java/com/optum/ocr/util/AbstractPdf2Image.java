package com.optum.ocr.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class AbstractPdf2Image {
    public abstract byte[] convert(MultipartFile file);

    public static class MemoryFile {
        MemoryFile(String fileName, byte[] contents) {
            this.fileName = fileName;
            this.contents = contents;
        }
        public String fileName;
        public byte[] contents;
    }

    public byte[] createZipByteArray(List<MemoryFile> files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        try {
            for (MemoryFile memoryFile : files) {
                ZipEntry zipEntry = new ZipEntry(memoryFile.fileName);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(memoryFile.contents);
                zipOutputStream.closeEntry();
            }
        } finally {
            zipOutputStream.close();
        }
        return byteArrayOutputStream.toByteArray();
    }
}
