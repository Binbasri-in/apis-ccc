package jp.co.sony.csl.dcoes.apis.tools.ccc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.Fixtures;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.HttpCaptureServer;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.TestConfigs;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class PolicyAcquisitionTest {
	@Test
	void enabledServiceRepliesWithHttpJson(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, Fixtures.policyResponse().encode());
		DeliveryOptions credentials = credentials();

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.configWithHttpEndpoint("policyAcquisition", started.port(), "/policy/current"));
					return vertx.deployVerticle(new PolicyAcquisition());
				})
				.compose(id -> vertx.eventBus().request(ServiceAddress.ControlCenterClient.policy(), null, credentials))
				.onComplete(testContext.succeeding(reply -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
					assertEquals(Fixtures.policyResponse(), reply.body());
					assertEquals("POST", captured.method());
					assertEquals("/policy/current", captured.path());
					assertTrue(captured.header("content-type").startsWith("application/x-www-form-urlencoded"));
					Map<String, String> params = captured.formParams();
					assertEquals("acct", params.get("account"));
					assertEquals("secret", params.get("password"));
					assertEquals("unit-1", params.get("unitId"));
					assertEquals(TestConfigs.COMMUNITY_ID, params.get("communityId"));
					assertEquals(TestConfigs.CLUSTER_ID, params.get("clusterId"));
					assertEquals("true", params.get("isMD5Password"));
					testContext.completeNow();
				})))));
	}

	@Test
	void httpFailureFailsEventBusRequest(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(500, "boom");

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.configWithHttpEndpoint("policyAcquisition", started.port(), "/policy/current"));
					return vertx.deployVerticle(new PolicyAcquisition());
				})
				.compose(id -> vertx.eventBus().request(ServiceAddress.ControlCenterClient.policy(), null, credentials()))
				.onComplete(testContext.failingThenComplete());
	}

	@Test
	void disabledServiceRepliesNullWithoutHttp(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer();

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.disabledConfig());
					return vertx.deployVerticle(new PolicyAcquisition());
				})
				.compose(id -> vertx.eventBus().request(ServiceAddress.ControlCenterClient.policy(), null, credentials()))
				.onComplete(testContext.succeeding(reply -> testContext.verify(() -> {
					assertNull(reply.body());
					server.assertNoRequest(vertx).onComplete(testContext.succeedingThenComplete());
				})));
	}

	private static DeliveryOptions credentials() {
		return new DeliveryOptions()
				.addHeader("account", "acct")
				.addHeader("password", "secret")
				.addHeader("unitId", "unit-1");
	}
}
