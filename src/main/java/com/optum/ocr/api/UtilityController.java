package com.optum.ocr.api;

import com.optum.ocr.service.UtilityService;
import com.optum.ocr.util.OcrObj;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Controller
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/utility")
@Api(value = "utility", description = "The Utility API", tags = {"Utility"})
public class UtilityController {
    @Value("${ocr.batch.folder.in}")
    private String folderIn;
    @Value("${ocr.batch.folder.out}")
    private String folderOut;
    @Value("${ocr.batch.folder.done}")
    private String folderDone;

    @Value("${tesseract.data}")
    private String tesseractFolder;

    @Autowired
    UtilityService utilityService;

    @PostMapping("/pdf2images/upload")
    public ResponseEntity<?> pdf2images(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IllegalAccessException, InstantiationException, IOException {
        byte[] bytes = utilityService.pdf2images(file);
        Resource resource = new ByteArrayResource(bytes);
        String contentType = "application/octet-stream";
        String filename = file.getOriginalFilename()+".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+filename)
                .body(resource);
    }

    @GetMapping("/ocr/createSearchable/{file}")
    public ResponseEntity<String> createSearchable(@PathVariable("file") String file) throws IllegalAccessException, InstantiationException, IOException {
        String str = utilityService.createSearchable(folderOut, file);
        return new ResponseEntity(str, HttpStatus.OK);
    }

    @GetMapping("/ocr/runBatch")
    public ResponseEntity<String> runBatch() throws IllegalAccessException, InstantiationException, IOException {
        String str = utilityService.ocrBatch(folderIn, folderOut, folderDone, tesseractFolder);
        return new ResponseEntity(str, HttpStatus.OK);
    }

    @GetMapping("/ocr/viewFaxOnQueue")
    public ResponseEntity<List<String>> viewFaxOnQueue() throws IllegalAccessException, InstantiationException, IOException {
        List<String> lst = utilityService.viewFaxOnQueue(folderIn);
        return new ResponseEntity(lst, HttpStatus.OK);
    }

    @GetMapping("/ocr/files")
    public ResponseEntity<List<String>> getFiles() throws IllegalAccessException, InstantiationException, IOException {
        List<String> lst = utilityService.ocrFiles(folderOut);
        return new ResponseEntity(lst, HttpStatus.OK);
    }

    @GetMapping("/ocr/archive/{file}")
    public ResponseEntity<?> archiveFile(@PathVariable("file") String file) throws IllegalAccessException, InstantiationException, IOException {
        utilityService.archiveFile(folderOut, file);
        return new ResponseEntity("Ok", HttpStatus.OK);
    }

    @GetMapping("/ocr/file/{file}")
    public ResponseEntity<OcrObj> readFile(@PathVariable("file") String file) throws IllegalAccessException, InstantiationException, IOException {
        OcrObj ocrObj = utilityService.readFile(folderOut, file);
        return new ResponseEntity(ocrObj, HttpStatus.OK);
    }

    @PostMapping("/ocr/upload")
    public ResponseEntity<?> uploadFax(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IllegalAccessException, InstantiationException, IOException {
        utilityService.ocrUpload(folderIn, folderOut, folderDone, tesseractFolder, file);
        return new ResponseEntity("Ok", HttpStatus.OK);
    }

    @PostMapping("/ocr")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IllegalAccessException, InstantiationException, IOException {
        byte[] bytes = utilityService.ocr(file);
        Resource resource = new ByteArrayResource(bytes);
        String contentType = "application/pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getOriginalFilename() + ".pdf")
                .body(resource);
    }

    @GetMapping("/ocr/searchable/{faxfile}")
    public ResponseEntity<?> searchablePdf(@PathVariable("faxfile") String faxfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        byte[] bytes = utilityService.getSearchablePdf(folderOut, faxfile);
        Resource resource = new ByteArrayResource(bytes);
        String contentType = "application/pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + faxfile)
                .body(resource);
    }

    @GetMapping("/ocr/file/{faxfile}/{page}")
    public ResponseEntity<?> displayOcrPage(@PathVariable("faxfile") String faxfile, @PathVariable("page") String page, HttpServletRequest request) throws InstantiationException, IllegalAccessException, IOException {
        byte[] bytes = utilityService.getPageImage(folderOut, faxfile, page);
        String fileName = faxfile+"-"+page+".jpg";
        Resource resource = new ByteArrayResource(bytes);
        // Try to determine file's content type
        String contentType = request.getServletContext().getMimeType(fileName);
        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName)
                .body(resource);
    }
}
