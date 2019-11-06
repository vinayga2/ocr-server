package com.optum.ocr.util;

import lombok.Data;

import java.awt.image.RenderedImage;

@Data
public class PageObj {
    public int pageNum;
    public RenderedImage imageIn;
    public RenderedImage imageOut;
    public String text;
}
