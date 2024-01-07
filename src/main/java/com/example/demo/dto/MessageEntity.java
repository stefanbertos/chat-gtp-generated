package com.example.demo.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "messages")
@Getter
@Setter
public class MessageEntity {

    @Id
    private String id;

    private String content;
}
