package dynamic.groovy.mbm

import java.nio.file.Files

class ArchivePdf {
    public void archiveFile(String folderOut, String file) {
        System.out.println("TEST");
        File folder = new File(folderOut, file);
        File searchFile = new File(folder, "Searchable-"+file);
        System.out.println(searchFile.getAbsolutePath());

        File outFile = new File(folder, "Archived-"+file);
        Files.deleteIfExists(outFile.toPath());
        Files.move(searchFile.toPath(), outFile.toPath());
    }
}
