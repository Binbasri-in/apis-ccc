package jp.co.sony.csl.dcoes.apis.tools.ccc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.Fixtures;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.HttpCaptureServer;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.TestConfigs;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class DealReportingTest {
	@Test
	void dealLoggingReportsSaveworthyDeal(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");
		JsonObject deal = Fixtures.saveworthyDeal("direct-deal");

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.configWithHttpEndpoint("dealReporting", started.port(), "/deal/report"));
					return vertx.deployVerticle(new DealReporting());
				})
				.compose(id -> vertx.eventBus().<String>request(ServiceAddress.Mediator.dealLogging(), deal))
				.onComplete(testContext.succeeding(reply -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
					assertEquals("ok", reply.body());
					assertEquals("POST", captured.method());
					assertEquals("/deal/report", captured.path());
					JsonArray body = captured.bodyAsJsonArray();
					assertEquals(1, body.size());
					JsonObject posted = body.getJsonObject(0);
					assertEquals("direct-deal", posted.getString("dealId"));
					assertEquals(TestConfigs.COMMUNITY_ID, posted.getString("communityId"));
					assertEquals(TestConfigs.CLUSTER_ID, posted.getString("clusterId"));
					assertTrue(posted.containsKey("reportTime"));
					testContext.completeNow();
				})))));
	}

	@Test
	void dealLoggingRejectsNonSaveworthyDeal(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.configWithHttpEndpoint("dealReporting", started.port(), "/deal/report"));
					return vertx.deployVerticle(new DealReporting());
				})
				.compose(id -> vertx.eventBus().request(ServiceAddress.Mediator.dealLogging(), Fixtures.nonSaveworthyDeal()))
				.onComplete(testContext.failing(err -> server.assertNoRequest(vertx).onComplete(testContext.succeedingThenComplete())));
	}

	@Test
	void disabledDealLoggingRepliesNotAvailable(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer();

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.disabledConfig());
					return vertx.deployVerticle(new DealReporting());
				})
				.compose(id -> vertx.eventBus().<String>request(ServiceAddress.Mediator.dealLogging(), Fixtures.saveworthyDeal("disabled-deal")))
				.onComplete(testContext.succeeding(reply -> testContext.verify(() -> {
					assertEquals("N/A", reply.body());
					server.assertNoRequest(vertx).onComplete(testContext.succeedingThenComplete());
				})));
	}

	@Test
	void startupTimerReportsOnlySaveworthyDeals(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		vertx.eventBus().consumer(ServiceAddress.Mediator.deals(), req -> {
			req.reply(new JsonArray()
					.add(Fixtures.saveworthyDeal("timer-deal"))
					.add(Fixtures.nonSaveworthyDeal())
					.add("not-a-deal"));
		});

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.configWithEnabledHttpReporting("dealReporting", started.port(), "/deal/report", 60000L));
					return vertx.deployVerticle(new DealReporting());
				})
				.onComplete(testContext.succeeding(id -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
					JsonArray body = captured.bodyAsJsonArray();
					assertEquals(1, body.size());
					JsonObject posted = body.getJsonObject(0);
					assertEquals("timer-deal", posted.getString("dealId"));
					assertEquals(TestConfigs.COMMUNITY_ID, posted.getString("communityId"));
					assertEquals(TestConfigs.CLUSTER_ID, posted.getString("clusterId"));
					assertFalse(posted.containsKey("ignored-deal"));
					testContext.completeNow();
				})))));
	}
}
