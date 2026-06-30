package jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport;

import io.vertx.core.json.JsonObject;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.VertxConfig;

public final class TestConfigs {
	public static final String COMMUNITY_ID = "test-community";
	public static final String CLUSTER_ID = "test-cluster";

	private TestConfigs() {
	}

	public static JsonObject disabledConfig() {
		return new JsonObject()
				.put("programId", "apis-ccc-test")
				.put("communityId", COMMUNITY_ID)
				.put("clusterId", CLUSTER_ID)
				.put("security", new JsonObject().put("enabled", false))
				.put("dealReporting", reportingSection(false, "/deal"))
				.put("unitDataReporting", reportingSection(false, "/unit-data"))
				.put("scenarioAcquisition", acquisitionSection(false, "/scenario"))
				.put("policyAcquisition", acquisitionSection(false, "/policy"))
				.put("watchdog", new JsonObject().put("enabled", false));
	}

	public static JsonObject configWithHttpEndpoint(String section, int port, String uri) {
		JsonObject config = disabledConfig();
		JsonObject target = config.getJsonObject(section);
		target.put("enabled", true)
				.put("host", "127.0.0.1")
				.put("port", port)
				.put("ssl", false)
				.put("sslTrustAll", false)
				.put("uri", uri)
				.put("requestTimeoutMsec", 1000);
		if ("dealReporting".equals(section) || "unitDataReporting".equals(section)) {
			target.put("type", "http_post").put("periodMsec", 60000L);
		}
		return config;
	}

	public static JsonObject configWithEnabledHttpReporting(String section, int port, String uri, long periodMsec) {
		JsonObject config = configWithHttpEndpoint(section, port, uri);
		config.getJsonObject(section).put("periodMsec", periodMsec);
		return config;
	}

	public static void apply(JsonObject config) {
		VertxConfig.config.setJsonObject(config);
	}

	private static JsonObject reportingSection(boolean enabled, String uri) {
		return new JsonObject()
				.put("enabled", enabled)
				.put("type", "http_post")
				.put("periodMsec", 60000L)
				.put("host", "127.0.0.1")
				.put("port", 1)
				.put("ssl", false)
				.put("sslTrustAll", false)
				.put("database", "apis_test")
				.put("collection", "test")
				.put("uri", uri)
				.put("requestTimeoutMsec", 1000L);
	}

	private static JsonObject acquisitionSection(boolean enabled, String uri) {
		return new JsonObject()
				.put("enabled", enabled)
				.put("host", "127.0.0.1")
				.put("port", 1)
				.put("ssl", false)
				.put("sslTrustAll", false)
				.put("uri", uri)
				.put("requestTimeoutMsec", 1000L);
	}
}
