package de.unikassel.rtr.regions;

import marvin.image.MarvinImage;
import marvin.image.MarvinSegment;
import marvinplugins.MarvinPluginCollection;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class TextRegions {

    public static List<BufferedImage> findText(BufferedImage bufferedImage) {
        MarvinImage marvinImage = new MarvinImage(bufferedImage);
        List<MarvinSegment> textSegments = MarvinPluginCollection.findTextRegions(marvinImage, 500, 200, 50, 200);
        List<BufferedImage> textList = new ArrayList<>(textSegments.size());
        for (MarvinSegment s : textSegments) {
            if(s.height >= 10) {
                MarvinImage subImage = marvinImage.subimage(s.x1 - 5, s.y1 - 5, s.width + 10, s.height + 10);
                subImage.update();
                textList.add(subImage.getBufferedImage());
            }
        }

        return textList;
    }

}
