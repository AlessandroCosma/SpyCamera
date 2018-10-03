package com.alessandrocosma.spycamera;

import java.util.HashMap;
import java.util.Map;

/** Classe che mi rappresenta un UniqueID */

public class Uid {

    private String uid;

    public Uid(){
        // Default constructor required for calls to DataSnapshot.getValue(Uid.class)
    }

    public Uid(String uid){
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public Map<String, String> toMap() {
        HashMap<String, String> result = new HashMap<>();
        result.put("uid", uid);

        return result;
    }

}