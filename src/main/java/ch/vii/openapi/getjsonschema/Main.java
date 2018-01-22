package ch.vii.openapi.getjsonschema;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
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
		System.err.println("ok");
	}

}
