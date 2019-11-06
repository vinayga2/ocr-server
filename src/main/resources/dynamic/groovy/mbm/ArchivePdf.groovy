package dynamic.groovy.mbm

import java.nio.file.Files

class ArchivePdf {
    public void archiveFile(String folderOut, String file) {
        System.out.println("TEST");
        File folder = new File(folderOut, file);
        File archivedFolder = new File(folderOut, "Archived-"+file);
        Files.move(folder.toPath(), archivedFolder.toPath());
    }
}
