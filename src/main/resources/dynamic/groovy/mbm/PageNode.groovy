package dynamic.groovy.mbm

import com.itextpdf.text.Anchor

import java.awt.image.RenderedImage

class PageNode {
    PageNode(int page, Anchor anchor, org.w3c.dom.Element node, RenderedImage image) {
        this.page = page;
        this.anchor = anchor;
        this.node = node;
        this.image = image;
    }
    public int page;
    public Anchor anchor;
    public org.w3c.dom.Element node;
    RenderedImage image;
}
