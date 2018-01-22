package ch.vii.openapi.getjsonschema;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.ResolverCache;

public class OpenApiSchemaAppender {
	private String filename;
	private String rootFolder;

	public OpenApiSchemaAppender(String filename, String rootFolder) {
		this.filename = filename;
		this.rootFolder = rootFolder;
	}

	public OpenAPI derefer(SchemaAppenderOptions options) {
		options = options == null ? new SchemaAppenderOptions() : options;

		OpenAPI openAPI = new OpenAPIV3Parser().read(filename, null, options.getParseOptions());

		ResolverCache resolverCache = new ResolverCache(openAPI, null, rootFolder);

		Set<Entry<String, PathItem>> entrySet = openAPI.getPaths().entrySet();

		for (Entry<String, PathItem> entry : entrySet) {

			Operation putSource = entry.getValue().getPut();
			if (putSource != null) {
				Operation putTarget = extractPut(openAPI, putSource, resolverCache);
				PathItem pathItemTarget = new PathItem();
				pathItemTarget.setPut(putTarget);
				openAPI.getPaths().addPathItem("/schema" + entry.getKey(), pathItemTarget);
			}
		}
		return openAPI;
	}

	private static void extractGet(OpenAPI openAPI, Operation get, ResolverCache resolverCache) {
		SchemaAppenderOptions options = new SchemaAppenderOptions();
		get.getResponses().forEach((key, response) -> {

			Content content = response.getContent();

			content.entrySet().forEach((consumer) -> {

				MediaType mediaType = consumer.getValue();
				Schema schema = mediaType.getSchema();

				SchemaDecoration schemaAsSchema = new SchemaDecoration(schema, "object");
				schemaAsSchema.decorate(resolverCache, options);

			});
		});
		openAPI.getComponents().getSchemas().putAll(options.getNewSchemas());
	}

	private static Operation extractPut(OpenAPI openAPI, Operation put, ResolverCache resolverCache) {

		MediaType requestMediaType = put.getRequestBody().getContent().get("application/json");
		SchemaAppenderOptions options = new SchemaAppenderOptions();
		SchemaDecoration schemaAsSchema = null;
		if (requestMediaType != null && requestMediaType.getSchema() != null) {

			// MediaType appJson = response.getContent().get("application/json");
			// MediaType appSchemaJson =
			// response.getContent().get("application/schema+json");

			schemaAsSchema = new SchemaDecoration(requestMediaType.getSchema(), "object");
			schemaAsSchema.decorate(resolverCache, options);
		}

		openAPI.getComponents().getSchemas().putAll(options.getNewSchemas());

		Content requestContent = new Content();
		requestContent.addMediaType("application/json", requestMediaType);
		RequestBody requestBody = new RequestBody();
		requestBody.setContent(requestContent);

		Operation schemaPut = new Operation();
		schemaPut.setRequestBody(requestBody);

		MediaType mediaType = new MediaType();
		mediaType.setSchema(schemaAsSchema);
		Content addMediaType = new Content().addMediaType("application/schema+json", mediaType);

		ApiResponse apiResponse = new ApiResponse();
		apiResponse.setContent(addMediaType);
		apiResponse.setDescription("Generated Schema");
		schemaPut.setResponses(new ApiResponses());
		schemaPut.getResponses().addApiResponse("200", apiResponse);
		ArrayList<String> tagList = new ArrayList<>();
		tagList.addAll(put.getTags());
		tagList.replaceAll(tag -> tag + " (generated schema)");

		schemaPut.setTags(tagList);
		return schemaPut;
	}
}
