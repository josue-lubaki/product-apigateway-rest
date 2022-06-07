package ca.josue.aws.lambda.errorhandling;

import ca.josue.aws.lambda.functions.CreateProduct;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {
    private final Logger logger = LoggerFactory.getLogger(CreateProduct.class);

    public void handleRequest(SNSEvent event){
        event.getRecords().forEach(record -> {
            logger.info("Dead Letter Queue Event : " + record.toString());
        });
    }
}
