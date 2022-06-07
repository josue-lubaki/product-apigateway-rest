package ca.josue.aws.lambda.functions;

import ca.josue.aws.lambda.dto.Product;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Logging {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(CreateProduct.class);

    public void handleRequest(SNSEvent event){
        event.getRecords().forEach(snsRecord -> {
            try {
                Product product = objectMapper
                        .readValue(snsRecord.getSNS().getMessage(), Product.class);

                logger.info("Student Checkout Event Received: " + product);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }
}
