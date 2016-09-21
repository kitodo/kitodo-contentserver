package de.unigoettingen.sub.commons.contentlib.imagelib;

public class ImageHolder {

    private byte[] image;
    private int width;
    private int height;
    private float xResolution;
    private float yResolution;

    public ImageHolder(byte[] image) {
        super();
        this.image = image;
    }

    public ImageHolder(byte[] image, int width, int height) {
        super();
        this.image = image;
        this.width = width;
        this.height = height;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public float getxResolution() {
        return xResolution;
    }

    public void setxResolution(float xResolution) {
        this.xResolution = xResolution;
    }

    public float getyResolution() {
        return yResolution;
    }

    public void setyResolution(float yResolution) {
        this.yResolution = yResolution;
    }

}
