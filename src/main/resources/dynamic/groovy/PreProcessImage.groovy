package dynamic.groovy

import com.optum.ocr.util.OcrAlignImage

import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.plugins.jpeg.JPEGImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp

class PreProcessImage extends MBMFaxReader {
    public BufferedImage preProcess(BufferedImage bufferedImage) {
        bufferedImage = OcrAlignImage.getAlignedImage(bufferedImage);               //significant improvemnt here.
//        bufferedImage = imageToBlackAndWhite(bufferedImage);                      //note: this is already done by tesseract, no improvements from this function.
        bufferedImage = improveQuality(bufferedImage, 1.0f);          //slight improvement here.
        return bufferedImage;
    }

    BufferedImage imageToBlackAndWhite(BufferedImage bufferedImage) throws IOException {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        bufferedImage = op.filter(bufferedImage, null);
        return bufferedImage;
    }

    BufferedImage improveQuality(BufferedImage img, float compressionLevel) {
        try {
            ImageWriter iw = ImageIO.getImageWritersByFormatName("jpeg").next();
            JPEGImageWriteParam iwp = (JPEGImageWriteParam) iw.getDefaultWriteParam();
            iwp.setOptimizeHuffmanTables(false);
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
            iwp.setCompressionQuality(compressionLevel);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            iw.setOutput(new MemoryCacheImageOutputStream(baos));

            IIOImage outputImage = new IIOImage(img, null, null);
            iw.write(null, outputImage, iwp);
            iw.dispose();

            baos.flush();
            byte[] returnImage = baos.toByteArray();
            baos.close();

            BufferedImage img2 = ImageIO.read(new ByteArrayInputStream(returnImage));
            if(img2 == null) {
                throw new Exception();
            }
            else {
                img = img2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img;
    }
}
