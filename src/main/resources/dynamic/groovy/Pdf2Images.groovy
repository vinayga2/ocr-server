package dynamic.groovy

import com.optum.ocr.util.AbstractPdf2Image
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.springframework.web.multipart.MultipartFile

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class Pdf2Images extends AbstractPdf2Image {
    @Override
    byte[] convert(MultipartFile file) {
        List<MemoryFile> memoryFiles = new ArrayList<>();

        PDDocument document = PDDocument.load(file.getBytes());
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            BufferedImage originalImage = pdfRenderer.renderImageWithDPI(page, 200, ImageType.RGB);
            String fileName = "image-" + (page+1) + ".jpg";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "jpg", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();

            memoryFiles.add(new MemoryFile(fileName, imageInByte));
        }
        document.close();
        byte[] bytes = createZipByteArray(memoryFiles);
        return bytes;
    }
}
