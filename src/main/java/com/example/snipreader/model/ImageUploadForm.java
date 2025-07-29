package com.example.snipreader.model;

import org.springframework.web.multipart.MultipartFile;

/**
 * Form model for handling image uploads and pasted images.
 */
public class ImageUploadForm {
    private MultipartFile image;
    private String base64Image;

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }
}