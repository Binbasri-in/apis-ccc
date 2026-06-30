package jp.co.sony.csl.dcoes.apis.tools.ccc.impl.http_post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.Fixtures;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.HttpCaptureServer;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.TestConfigs;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class HttpPostScenarioAcquisitionImplTest {
	@Test
	void acquireCurrentPostsCredentialsAndParsesJson(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, Fixtures.scenarioResponse().encode());

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("scenarioAcquisition", started.port(), "/scenario/current"));
			HttpPostScenarioAcquisitionImpl impl = new HttpPostScenarioAcquisitionImpl(vertx);
			Promise<io.vertx.core.json.JsonObject> promise = Promise.promise();
			impl.acquireCurrent("acct", "secret", "unit-1", promise);
			promise.future().onComplete(testContext.succeeding(result -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
				assertEquals(Fixtures.scenarioResponse(), result);
				Map<String, String> params = captured.formParams();
				assertEquals("acct", params.get("account"));
				assertEquals("secret", params.get("password"));
				assertEquals("unit-1", params.get("unitId"));
				assertEquals(TestConfigs.COMMUNITY_ID, params.get("communityId"));
				assertEquals(TestConfigs.CLUSTER_ID, params.get("clusterId"));
				assertEquals("true", params.get("isMD5Password"));
				testContext.completeNow();
			})))));
		}));
	}

	@Test
	void emptySuccessBodyReturnsNullResult(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("scenarioAcquisition", started.port(), "/scenario/current"));
			HttpPostScenarioAcquisitionImpl impl = new HttpPostScenarioAcquisitionImpl(vertx);
			Promise<io.vertx.core.json.JsonObject> promise = Promise.promise();
			impl.acquireCurrent("acct", "secret", "unit-1", promise);
			promise.future().onComplete(testContext.succeeding(result -> testContext.verify(() -> {
				assertNull(result);
				testContext.completeNow();
			})));
		}));
	}

	@Test
	void nonSuccessHttpStatusFailsAcquire(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(500, "boom");

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("scenarioAcquisition", started.port(), "/scenario/current"));
			HttpPostScenarioAcquisitionImpl impl = new HttpPostScenarioAcquisitionImpl(vertx);
			Promise<io.vertx.core.json.JsonObject> promise = Promise.promise();
			impl.acquireCurrent("acct", "secret", "unit-1", promise);
			promise.future().onComplete(testContext.failingThenComplete());
		}));
	}
}
