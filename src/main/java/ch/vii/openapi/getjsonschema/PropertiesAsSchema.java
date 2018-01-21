package ch.vii.openapi.getjsonschema;

import java.util.Map;

import io.swagger.v3.oas.models.media.Schema;

public class PropertiesAsSchema<A extends Schema<?>> extends Schema<A> {

	private Map<String, Schema> props;

	public PropertiesAsSchema(Map<String, Schema> props) {
		this.props = props;
	}

	@Override
	public String getName() {
		return "properties";
	}

	@Override
	public String getType() {
		return "object";
	}

	@Override
	public Map<String, Schema> getProperties() {
		return props;
	}
}
