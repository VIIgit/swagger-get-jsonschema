package ch.vii.openapi.getjsonschema;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.ResolverCache;

public class SchemaDecoration<A extends Schema<?>> extends Schema<A> {

	private Schema<?> s;

	public SchemaDecoration(Schema<?> s, String type) {
		this.s = s;
		super.setType(type);
	}

	public void decorate(ResolverCache resolverCache, SchemaAppenderOptions options) {

		if (s.get$ref() != null) {
			String get$ref = s.get$ref();

			String newcomponentKey = update$Ref(s);

			this.s = resolverCache.loadRef(get$ref, computeRefFormat(get$ref), Schema.class);

			options.getNewSchemas().put(newcomponentKey, this.s);

			super.setType(this.s.getType());
		}

		Map<String, Schema> properties = new HashMap<>();

		List<String> attrOfSameSchema = asList("minimum", "maximum", "default", "multipleOf");
		properties.putAll(mapToSameSchema(attrOfSameSchema, s.getClass(), resolverCache, options));

		List<String> attrOfStringSchema = asList("type", "description", "title");
		properties.putAll(mapToSameSchema(attrOfStringSchema, StringSchema.class, resolverCache, options));

		List<String> attrOfIntegerSchema = asList("minLength", "maxLength");
		properties.putAll(mapToSameSchema(attrOfIntegerSchema, IntegerSchema.class, resolverCache, options));

		List<String> attrOfBooleanSchema = asList("readOnly", "writeOnly", "exclusiveMaximum", "exclusiveMinimum",
				"uniqueItems");
		properties.putAll(mapToSameSchema(attrOfBooleanSchema, BooleanSchema.class, resolverCache, options));

		if (s.getEnum() != null) {
			ArraySchema items = new ArraySchema();
			Schema<?> item = toSchema(s, "enum", s.getClass(), false, resolverCache, options);
			items.setItems(item);
			items.setExample(s.getEnum());
			properties.put("enum", items);
		}
		if (s.getRequired() != null) {
			ArraySchema items = new ArraySchema();
			Schema<?> item = toSchema(s, "required", s.getClass(), false, resolverCache, options);
			items.setItems(item);
			items.setExample(s.getRequired());
			properties.put("required", items);
		}
		if (s instanceof ArraySchema) {
			Schema<?> itemsSchema = ((ArraySchema) s).getItems();
			SchemaDecoration schemaDecoration = new SchemaDecoration(itemsSchema, itemsSchema.getType());

			schemaDecoration.decorate(resolverCache, options);

			properties.put("items", schemaDecoration);
		}

		if (s.getProperties() != null) {
			Map<String, Schema> propMap = new HashMap<>();
			s.getProperties().forEach((key, schema) -> {
				SchemaDecoration schemaObject = new SchemaDecoration<>(schema, "object");

				schemaObject.decorate(resolverCache, options);

				propMap.put(key, schemaObject);
			});
			if (!propMap.isEmpty()) {
				properties.put("properties", new PropertiesAsSchema<>(propMap));
			}
		}

		super.setProperties(properties);

	}

	private String update$Ref(Schema<?> schema) {
		String newcomponentKey = "__schemaOf" + schema.get$ref().replaceAll("[\\.\\#]", "\\_")
				.replace("#/components/schemas/", "").replaceAll("[\\/]", "-");

		return newcomponentKey;
	}

	private Map<String, Schema<?>> mapToSameSchema(List<String> attributes, Class<?> schemaClass,
			ResolverCache resolverCache, SchemaAppenderOptions options) {
		Map<String, Optional<Schema<?>>> resolvedSchemaMap = attributes.stream()

				.collect(Collectors.toMap(e -> e,
						e -> Optional.ofNullable(toSchema(s, e, schemaClass, true, resolverCache, options))));

		return resolvedSchemaMap.entrySet().stream().filter(e -> e.getValue().isPresent())
				.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue().get()));
	}

	private Schema<?> toSchema(Schema<?> schema, String method, Class<?> schemaClass, boolean withExample,
			ResolverCache resolverCache, SchemaAppenderOptions options) {

		try {
			String getMethod = "get" + method.substring(0, 1).toUpperCase() + method.substring(1);
			if (schema.get$ref() != null) {
				String get$ref = s.get$ref();
				String newcomponentKey = update$Ref(schema);
				schema.set$ref("#/components/schemas/" + newcomponentKey);
				schema = resolverCache.loadRef(get$ref, computeRefFormat(get$ref), Schema.class);
				options.getNewSchemas().put(newcomponentKey, schema);
			}

			Method m = schema.getClass().getMethod(getMethod);
			Object result = m.invoke(schema);

			if (result != null) {
				Schema<?> newSchema = (Schema<?>) schemaClass.newInstance();
				if (withExample) {
					newSchema.setExample(result);
				}
				return newSchema;
			}
		} catch (ReflectiveOperationException ex) {
			System.err.println("not found method " + method + " in " + schema.getClass().getSimpleName());
		}

		return null;
	}
}
