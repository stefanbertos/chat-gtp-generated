package com.example.demo.service;

import com.example.demo.dto.MessageEntity;
import com.example.demo.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessingService {

    @Autowired
    private MessageRepository messageRepository;

    @Retryable(value = {DataAccessResourceFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @Transactional(timeout = 5)  // Transaction timeout in seconds
    public void processMessage(String messageContent) {
        // Save to MongoDB
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setContent(messageContent);
        messageRepository.save(messageEntity);
    }
}