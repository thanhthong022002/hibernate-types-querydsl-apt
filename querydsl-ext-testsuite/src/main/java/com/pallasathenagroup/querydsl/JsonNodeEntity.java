package com.pallasathenagroup.querydsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.querydsl.core.annotations.NameClass;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JsonNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    JsonNode jsonNode;

    @Type(type = "jsonb")
    @Column(name = "embed_1", columnDefinition = "jsonb")
    Embed1 embed1;

    @Type(type = "jsonb")
    @Column(name = "list_int", columnDefinition = "jsonb")
    List<Integer> listInt;

    @Column(name = "int_number")
    Integer intNumber;

    @Column(name = "uuid")
    UUID uuid;

    @Column(name = "embed1_list", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    List<Embed1> embed1List;

    @NameClass
    public static final class Embed1 {

        String embed1_attr1;

        Embed2 embed1_attr2;

        List<Integer> embed1_intList;

        Boolean embed1_boolean;

        Integer embed1_int;

        String uuidText;

        public String getEmbed1_attr1() {
            return embed1_attr1;
        }

        public void setEmbed1_attr1(String embed1_attr1) {
            this.embed1_attr1 = embed1_attr1;
        }

        public Embed2 getEmbed1_attr2() {
            return embed1_attr2;
        }

        public void setEmbed1_attr2(Embed2 embed1_attr2) {
            this.embed1_attr2 = embed1_attr2;
        }

        public List<Integer> getEmbed1_intList() {
            return embed1_intList;
        }

        public void setEmbed1_intList(List<Integer> embed1_intList) {
            this.embed1_intList = embed1_intList;
        }

        public Boolean getEmbed1_boolean() {
            return embed1_boolean;
        }

        public void setEmbed1_boolean(Boolean embed1_boolean) {
            this.embed1_boolean = embed1_boolean;
        }

        public Integer getEmbed1_int() {
            return embed1_int;
        }

        public void setEmbed1_int(Integer embed1_int) {
            this.embed1_int = embed1_int;
        }

        public String getUuidText() {
            return uuidText;
        }

        public void setUuidText(String uuidText) {
            this.uuidText = uuidText;
        }
    }

    @NameClass
    public static final class Embed2 {

        String embed2_attr1;

        public String getEmbed2_attr1() {
            return embed2_attr1;
        }

        public void setEmbed2_attr1(String embed2_attr1) {
            this.embed2_attr1 = embed2_attr1;
        }
    }
}
