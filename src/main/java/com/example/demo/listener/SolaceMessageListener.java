package com.example.demo.listener;

import com.example.demo.service.ProcessingService;
import com.solacesystems.jcsmp.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SolaceMessageListener {

    private final SpringJCSMPFactory solaceConnectionFactory;
    private final ProcessingService processingService;
    @Value("${solace.queue.name}")
    private String solaceQueueName;

    @EventListener(ApplicationStartedEvent.class)
    public void listenToSolaceQueue() {
        try {
            JCSMPSession session = solaceConnectionFactory.createSession();

            // Create a Queue instance
            Queue queue = JCSMPFactory.onlyInstance().createQueue(solaceQueueName);

            var endPointProperties = new EndpointProperties();
            endPointProperties.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);
            endPointProperties.setPermission(EndpointProperties.PERMISSION_CONSUME);

            var flowProps = new ConsumerFlowProperties();
            flowProps.setEndpoint(queue);
            flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);


            var flowConsumer = session.createFlow(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage bytesXMLMessage) {
                    if (bytesXMLMessage instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) bytesXMLMessage;
                        String messageContent = textMessage.getText();
                        processingService.processMessage(messageContent);
                    }
                    bytesXMLMessage.ackMessage();
                }

                @Override
                public void onException(JCSMPException e) {

                }
            }, flowProps, endPointProperties);
            flowConsumer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

