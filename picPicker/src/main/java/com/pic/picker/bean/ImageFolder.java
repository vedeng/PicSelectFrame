package com.pic.picker.bean;

import java.io.Serializable;
import java.util.ArrayList;

public class ImageFolder implements Serializable {

    public String name;
    public String path;
    public ImageItem cover;
    public ArrayList<ImageItem> images;

    @Override
    public boolean equals(Object o) {
        try {
            ImageFolder other = (ImageFolder) o;
            return this.path.equalsIgnoreCase(other.path) && this.name.equalsIgnoreCase(other.name);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return super.equals(o);
    }
}
