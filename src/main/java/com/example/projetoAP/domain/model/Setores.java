package com.example.projetoAP.domain.model;

import com.example.projetoAP.domain.model.Setor.SetorDTO;
import com.example.projetoAP.domain.model.Setor.SetorRegistroDTO;
import com.example.projetoAP.repository.EntitySetor.EntitySetores;

public class Setores {
    private long id;
    private  String nameComplexo;
    private Integer filial;
    public Setores(SetorDTO s) {
        this.nameComplexo = s.nome();
        this.filial = Integer.parseInt(s.filial());
        this.id = s.id();
    }
    public Setores(EntitySetores s) {
        this.nameComplexo = s.getNameComplexo();
        this.filial = s.getFilial();
        this.id = s.getId();
    }
  public  Setores(){};

    public Setores(SetorRegistroDTO s) {
        this.nameComplexo = s.nome();
        this.filial = Integer.parseInt(s.filial());
    }

    public long getId() {
        return id;
    }

    public Integer getFilial() {
        return filial;
    }

    public String getNameComplexo() {
        return nameComplexo;
    }
}


//http://localhost:8081/api_jsonrpc.php


//{
//        "jsonrpc": "2.0",
//        "method": "history.get",
//        "params": {
//        "output": "extend",
//        "history": 0,
//        "itemids": "30981",
//        "sortfield": "clock",
//        "sortorder": "DESC",
//        "limit": 1000000000
//        },
//        "auth": "d5721da3ff4da7fc1ede036ecb77af0b",
//        "id": 1
//        }


//http://localhost:8081/api_jsonrpc.php


//{
//        "jsonrpc": "2.0",
//        "method": "user.login",
//        "params": {
//        "user": "Admin",
//        "password": "zabbix"
//        },
//        "id": 1,
//        "auth": null
//        }


//{
//        "jsonrpc": "2.0",
//        "method": "item.get",
//        "params": {
//        "output": ["itemid", "name", "key_"],
//        "hostids": "10325",
//        "search": {
//        "key_": "icmpping"
//        }
//        },
//        "auth": "d5721da3ff4da7fc1ede036ecb77af0b",
//        "id": 1
//        }
