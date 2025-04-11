package com.example.projetoAP.domain.vo;

import com.example.projetoAP.repository.ItemsICMPEntity;

public record IcmpDTO(long itemid, String name, String key_) {
    public IcmpDTO(ItemsICMPEntity item) {
        this(item.getItemid(),item.getName(),item.getKey_());
    }
}
