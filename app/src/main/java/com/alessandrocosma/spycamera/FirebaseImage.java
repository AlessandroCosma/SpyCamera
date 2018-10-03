package com.alessandrocosma.spycamera;


/** Classe che mi rappresenta un'immagine che scarico dal FirebaseDB */

public class FirebaseImage {

    private String imageUrl;
    private long timestamp;

    public FirebaseImage(){
    }

    public FirebaseImage(String imageUrl, long timestamp){
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }


}

/*
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("imageUrl", imageUrl);
        result.put("timestamp", timestamp);

        return result;
    }

*/