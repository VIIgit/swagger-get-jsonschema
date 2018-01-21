package ch.vii.openapi.getjsonschema;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;

public class SchemaAppenderOptions {

	private boolean resolve = true;
	private HashMap<String, Schema<?>> schemasNew = new HashMap<>();
	private boolean updateReference = true;
	private Mode mode = Mode.PATH_PREFIX;
	private ParseOptions parseOptions;

	public ParseOptions getParseOptions() {
		return parseOptions;
	}

	public void setParseOptions(ParseOptions parseOptions) {
		this.parseOptions = parseOptions;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public boolean isResolve() {
		return resolve;
	}

	public boolean isUpdateReference() {
		return updateReference;
	}

	public void put(String key, Schema<?> schema) {
		schemasNew.put(null, schema);
	}

	public void setResolve(boolean resolve) {
		this.resolve = resolve;
	}

	public void setUpdateReference(boolean updateReference) {
		this.updateReference = updateReference;
	}

	public Map<String, Schema<?>> getNewSchemas() {
		return schemasNew;
	}
}

enum Mode {

	PATH_PREFIX,

	CONTENT_TYPE

}
