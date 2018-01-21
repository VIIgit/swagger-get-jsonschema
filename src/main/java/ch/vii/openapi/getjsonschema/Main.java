package ch.vii.openapi.getjsonschema;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Set;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.ParseOptions;

public class Main {

	public static void main(String[] args) throws Exception {
		File file = new File("./src/main/resources/openapi/v3/petstore.yaml");
		String filename = file.getAbsolutePath();

		SchemaAppenderOptions appenderOptions = new SchemaAppenderOptions();
		ParseOptions options = new ParseOptions();
		options.setResolve(true);
		options.setFlatten(true);
		options.setResolveFully(true);
		appenderOptions.setParseOptions(options);

		OpenApiSchemaAppender schemaAppender = new OpenApiSchemaAppender(filename, file.getAbsolutePath());
		OpenAPI dereferedOpenApi = schemaAppender.derefer(appenderOptions);
		String newFileopenAPISchema = Yaml.pretty(dereferedOpenApi);
		Files.write(Paths.get("./src/main/resources/openapi/v3/petstore-schema.yaml"), newFileopenAPISchema.getBytes());

		OpenAPI openAPI = new OpenAPIV3Parser().read(filename, null, options);

		ResolverCache resolverCache = new ResolverCache(openAPI, null, file.getAbsolutePath());

		String newFile = Yaml.pretty(openAPI);
		Files.write(Paths.get("./src/main/resources/openapi/v3/petstore-formatted.yaml"), newFile.getBytes());

		OpenAPI openAPISchema = new OpenAPI();
		openAPISchema.setPaths(new io.swagger.v3.oas.models.Paths());

		Set<Entry<String, PathItem>> entrySet = openAPI.getPaths().entrySet();

		for (Entry<String, PathItem> entry : entrySet) {

			// PathItem schemaPathItem = new PathItem();
			System.err.println(entry.getKey());
			PathItem pathItem = entry.getValue();

			// Operation get = pathItem.getGet();
			// if (get != null) {
			// extractGet(openAPI, get);
			// }

			Operation put = pathItem.getPut();
			if (put != null) {
				extractPut(openAPI, put, resolverCache);
			}

		}

		String newFileopenAPISchema2 = Yaml.pretty(openAPI);
		Files.write(Paths.get("./src/main/resources/openapi/v3/petstore-schema2.yaml"),
				newFileopenAPISchema2.getBytes());

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

	private static void extractPut(OpenAPI openAPI, Operation put, ResolverCache resolverCache) {

		SchemaAppenderOptions options = new SchemaAppenderOptions();
		put.getResponses().forEach((key, response) -> {

			ApiResponse apiResponse2 = new ApiResponse();

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

}
