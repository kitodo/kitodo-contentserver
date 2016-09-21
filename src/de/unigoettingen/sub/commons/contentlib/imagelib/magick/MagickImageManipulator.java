package de.unigoettingen.sub.commons.contentlib.imagelib.magick;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import magick.MagickException;
import magick.MagickImage;

import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator.MergingMode;

public class MagickImageManipulator {

    private static final Logger LOGGER = Logger.getLogger(MagickImageManipulator.class);

    public static MagickImage scaleSubsampleBinaryToGrey(MagickImage inImage, float internalScaling_x, float internalScaling_y) {
        return scaleImage(inImage, internalScaling_x, internalScaling_y);
    }

    public static MagickImage scaleInterpolationBilinear(MagickImage inImage, float internalScaling_x, float internalScaling_y) {
        return scaleImage(inImage, internalScaling_x, internalScaling_y);
    }

    public static MagickImage scaleNextNeighbor(MagickImage inImage, float internalScaling_x, float internalScaling_y) {
        return scaleImage(inImage, internalScaling_x, internalScaling_y);
    }

    public static MagickImage scaleImage(MagickImage inImage, float internalScaling_x, float internalScaling_y) {
        try {
            Dimension imageDim = inImage.getDimension();
            MagickImage outImage = inImage.scaleImage((int) (imageDim.width * internalScaling_x), (int) (imageDim.height * internalScaling_y));
            return outImage;
        } catch (MagickException e) {
            LOGGER.error("Error scaling image");
            return inImage;
        }
    }

    public static MagickImage drawBoxes(MagickImage inImage, List<String> draw_coordinates, Color inColor) {
        // TODO Auto-generated method stub
        return inImage;
    }

    public static MagickImage rotate(MagickImage inImage, int angle) {
        try {
            MagickImage outImage = inImage.rotateImage(angle);
            return outImage;
        } catch (MagickException e) {
            LOGGER.error("Error rotating image");
            return inImage;
        }
    }

    public static MagickImage mergeImages(MagickImage image1, MagickImage image2, MergingMode mode) {
        // TODO: implement method
        // MagickImage watermark = new MagickImage();
        //
        // MagickImage images[] = new MagickImage[2];
        // images[0] = inImage;
        // images[1] = image;
        // MagickImage seqImage = new MagickImage(images);
        // MontageInfo montageInfo = new MontageInfo(new ImageInfo());
        // montageInfo.setFileName("montage.jpg");
        // montageInfo.setTitle("Melbourne");
        // montageInfo.setBorderWidth(5);
        // MagickImage montage = seqImage.montageImages(montageInfo);
        // montage.writeImage(new ImageInfo());

        return image1;
    }

}
