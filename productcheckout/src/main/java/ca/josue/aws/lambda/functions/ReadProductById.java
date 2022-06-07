package ca.josue.aws.lambda.functions;

import ca.josue.aws.lambda.dto.Product;
import ca.josue.aws.lambda.dto.ProductDTO;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ReadProductById implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String PRODUCT_TABLE = System.getenv("PRODUCT_TABLE");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    private final Logger logger = LoggerFactory.getLogger(ReadProductById.class);

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
                                                      Context context){
        // scan all products from table
        ScanResult scanResult = dynamoDB.scan(new ScanRequest().withTableName(PRODUCT_TABLE));

        logger.info("Scanning all products from table...");
        List<Product> products = scanResult.getItems().stream().map(product -> new Product(
                        product.get("id").getS(),
                        product.get("name").getS(),
                        product.get("description").getS(),
                        Double.parseDouble(product.get("price").getN()),
                        Integer.parseInt(product.get("quantity").getN())
                )
        ).collect(Collectors.toList());

        // get id from request
        String id = request.getPathParameters().get("id");

        // get product by id if id is not null
        if(id == null){
            try {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody(objectMapper.writeValueAsString(products));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        // get product by id
        Product product = products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);

        // return product if found
        if(product != null){
            try {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody(objectMapper.writeValueAsString(product));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        // return error if product not found
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatus.SC_NOT_FOUND);

    }

}
