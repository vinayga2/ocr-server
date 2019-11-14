package dynamic.groovy

import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.metadata.IIOInvalidTreeException
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageOutputStream
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage

class PreProcessImage extends MBMFaxReader {
    public BufferedImage preProcess(BufferedImage bufferedImage) {
        return bufferedImage;
    }

    RenderedImage changeDpi(BufferedImage gridImage) {
        String formatName = "jpg";
        Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName);
        iw.hasNext();
        ImageWriter writer = iw.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
        setDPI(metadata, 300);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream stream = ImageIO.createImageOutputStream(baos);
        try {
            writer.setOutput(stream);
            writer.write(metadata, new IIOImage(gridImage, null, metadata), writeParam);
        }
        finally {
            writer.dispose();
            stream.flush();
            stream.close();
        }
        byte[] imageInByte = baos.toByteArray();

        InputStream inputStream = new ByteArrayInputStream(imageInByte);
        BufferedImage bImageFromConvert = ImageIO.read(inputStream);
        return bImageFromConvert;
    }

    void setDPI(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {
        // for PNG, it's dots per millimeter
        float  inch_2_cm = 2.54;
        double dotsPerMilli = 1.0 * dpi / 10 / inch_2_cm;
        IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
        horiz.setAttribute("value", Double.toString(dotsPerMilli));
        IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
        vert.setAttribute("value", Double.toString(dotsPerMilli));
        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horiz);
        dim.appendChild(vert);
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dim);
        metadata.mergeTree("javax_imageio_1.0", root);
    }
}
