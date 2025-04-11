package com.example.projetoAP.repository;
import com.example.projetoAP.domain.model.ItemsICMP;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "icmp",uniqueConstraints = @UniqueConstraint(columnNames = "key_"))
public class ItemsICMPEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long itemid;
    private String name;
    @Column(name = "key_", unique = true)
    private String key_;
    public ItemsICMPEntity(){}
    public ItemsICMPEntity(ItemsICMP e){
        this.name = e.getName();
        this.key_ = e.getKey_();
    }
    public long getItemid() {
        return itemid;
    }

    public String getKey_() {
        return key_;
    }

    public String getName() {
        return name;
    }
}
