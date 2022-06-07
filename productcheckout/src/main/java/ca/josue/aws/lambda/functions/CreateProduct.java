package ca.josue.aws.lambda.functions;

import ca.josue.aws.lambda.dto.ProductDTO;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class CreateProduct implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String PRODUCT_TABLE = System.getenv("PRODUCT_TABLE");
    public static final String PRODUCT_CHECKOUT_TOPIC = System.getenv("PRODUCT_CHECKOUT_TOPIC");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
    private final AmazonSNS sns = AmazonSNSClientBuilder.defaultClient();
    private final Logger logger = LoggerFactory.getLogger(CreateProduct.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
                                                      Context context) {
        try {
            // get Body from request
            ProductDTO product = objectMapper.readValue(request.getBody(), ProductDTO.class);

            // create table in DynamoDB if it doesn't exist
            Table table = dynamoDB.getTable(PRODUCT_TABLE);

            // create item
            logger.info("Creating product...");
            Item item = new Item()
                    .withPrimaryKey("id", UUID.randomUUID().toString())
                    .withString("name", product.getName())
                    .withString("description", product.getDescription())
                    .withDouble("price", product.getPrice())
                    .withInt("quantity", product.getQuantity());

            // put item in table
            table.putItem(item);

            // publish message to SNS
            logger.info("Message being published to SNS...");
            publishMessageToSNS(product);

            String jsonOutput = objectMapper.writeValueAsString(product);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_CREATED)
                    .withBody(jsonOutput);

        } catch (IOException e) {
            logger.error("Exception is : ", e);
            throw new RuntimeException(e);
        }
    }

    private void publishMessageToSNS(ProductDTO product) {
        try {
            sns.publish(
                    PRODUCT_CHECKOUT_TOPIC,
                    objectMapper.writeValueAsString(product)
            );
        } catch (JsonProcessingException e) {
            logger.error("Exception on publishing student checkout event to SNS", e);
            throw new RuntimeException("Error while publishing product checkout event to SNS : " + e);
        }
    }
}
