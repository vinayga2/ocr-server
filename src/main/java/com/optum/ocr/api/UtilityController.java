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
    @Value("${ocr.folder}")
    private String ocrFolder;

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

    @GetMapping("/ocr/{companyCode}/createSearchable/{file}")
    public ResponseEntity<String> createSearchable(@PathVariable("companyCode") String companyCode, @PathVariable("file") String file) throws IllegalAccessException, InstantiationException, IOException {
        String str = utilityService.createSearchable(companyCode, ocrFolder, file);
        return new ResponseEntity(str, HttpStatus.OK);
    }

    @GetMapping("/ocr/{companyCode}/viewFaxOnQueue")
    public ResponseEntity<List<String>> viewFaxOnQueue(@PathVariable("companyCode") String companyCode) throws IllegalAccessException, InstantiationException, IOException {
        List<String> lst = utilityService.viewFaxOnQueue(companyCode, ocrFolder);
        return new ResponseEntity(lst, HttpStatus.OK);
    }

    @GetMapping("/ocr/{companyCode}/files")
    public ResponseEntity<List<String>> getFiles(@PathVariable("companyCode") String companyCode) throws IllegalAccessException, InstantiationException, IOException {
        List<String> lst = utilityService.ocrFiles(companyCode, ocrFolder);
        return new ResponseEntity(lst, HttpStatus.OK);
    }

    @GetMapping("/ocr/{companyCode}/archive/{file}")
    public ResponseEntity<?> archiveFile(@PathVariable("companyCode") String companyCode, @PathVariable("file") String file) throws IllegalAccessException, InstantiationException, IOException {
        utilityService.archiveFile(companyCode, ocrFolder, file);
        return new ResponseEntity("Ok", HttpStatus.OK);
    }

    @GetMapping("/ocr/{companyCode}/file/{file}")
    public ResponseEntity<OcrObj> readFile(@PathVariable("companyCode") String companyCode, @PathVariable("file") String file) throws IllegalAccessException, InstantiationException, IOException {
        OcrObj ocrObj = utilityService.readFile(companyCode, ocrFolder, file);
        return new ResponseEntity(ocrObj, HttpStatus.OK);
    }

    @PostMapping("/ocr/{companyCode}/upload")
    public ResponseEntity<?> uploadFax(@PathVariable("companyCode") String companyCode, @RequestParam("file") MultipartFile file, HttpServletRequest request) throws IllegalAccessException, InstantiationException, IOException {
        utilityService.ocrUpload(companyCode, ocrFolder, tesseractFolder, file);
        return new ResponseEntity("Ok", HttpStatus.OK);
    }

    @PostMapping("/ocr/{companyCode}")
    public ResponseEntity<?> upload(@PathVariable("companyCode") String companyCode, @RequestParam("file") MultipartFile file, HttpServletRequest request) throws IllegalAccessException, InstantiationException, IOException {
        byte[] bytes = utilityService.ocr(companyCode, ocrFolder, file);
        Resource resource = new ByteArrayResource(bytes);
        String contentType = "application/pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getOriginalFilename() + ".pdf")
                .body(resource);
    }

    @GetMapping("/ocr/{companyCode}/searchable/{faxfile}")
    public ResponseEntity<?> searchablePdf(@PathVariable("companyCode") String companyCode, @PathVariable("faxfile") String faxfile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        byte[] bytes = utilityService.getSearchablePdf(companyCode, ocrFolder, faxfile);
        Resource resource = new ByteArrayResource(bytes);
        String contentType = "application/pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + faxfile)
                .body(resource);
    }

    @GetMapping("/ocr/{companyCode}/file/{faxfile}/{page}")
    public ResponseEntity<?> displayOcrPage(@PathVariable("companyCode") String companyCode, @PathVariable("faxfile") String faxfile, @PathVariable("page") String page, HttpServletRequest request) throws InstantiationException, IllegalAccessException, IOException {
        byte[] bytes = utilityService.getPageImage(companyCode, ocrFolder, faxfile, page);
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
