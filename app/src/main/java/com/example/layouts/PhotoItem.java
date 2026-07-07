package com.example.layouts;

public class PhotoItem {

    private final String name;
    private final String size;
    private final int thumbnailColor;
    private boolean selected;

    public PhotoItem(String name, String size, int thumbnailColor) {
        this.name = name;
        this.size = size;
        this.thumbnailColor = thumbnailColor;
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public int getThumbnailColor() {
        return thumbnailColor;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
