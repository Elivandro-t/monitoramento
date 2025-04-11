package com.example.projetoAP.domain.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.springframework.data.annotation.Id;

public class ItemsICMP {
    private String itemid;
    private String name;
    private String key_;

    public String getItemid() {
        return itemid;
    }

    public String getKey_() {
        return key_;
    }

    public String getName() {
        return name;
    }
}
