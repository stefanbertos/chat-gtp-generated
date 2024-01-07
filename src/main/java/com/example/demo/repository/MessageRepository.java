package com.example.demo.repository;

import com.example.demo.dto.MessageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<MessageEntity, String> {
    // You can add custom query methods here if needed
}