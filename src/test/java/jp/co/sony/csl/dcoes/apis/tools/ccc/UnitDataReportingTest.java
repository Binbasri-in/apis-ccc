package jp.co.sony.csl.dcoes.apis.tools.ccc;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class UnitDataReportingTest {
	@Test
	void startupTimerReportsFilteredUnitDataWithDatasetId(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		vertx.eventBus().consumer(ServiceAddress.GridMaster.unitDatas(), req -> req.reply(Fixtures.unitDataMap()));

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.configWithEnabledHttpReporting("unitDataReporting", started.port(), "/unit-data/report", 60000L));
					return vertx.deployVerticle(new UnitDataReporting());
				})
				.onComplete(testContext.succeeding(id -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
					JsonArray body = captured.bodyAsJsonArray();
					assertEquals(2, body.size());
					for (Object item : body) {
						JsonObject unitData = (JsonObject) item;
						assertTrue(unitData.containsKey("unitId"));
						assertTrue(unitData.containsKey("datasetId"));
					}
					testContext.completeNow();
				})))));
	}

	@Test
	void emptyUnitDataDoesNotPostAndVerticleStaysDeployed(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		vertx.eventBus().consumer(ServiceAddress.GridMaster.unitDatas(), req -> req.reply(new JsonObject()));

		server.start(vertx)
				.compose(started -> {
					TestConfigs.apply(TestConfigs.configWithEnabledHttpReporting("unitDataReporting", started.port(), "/unit-data/report", 60000L));
					return vertx.deployVerticle(new UnitDataReporting());
				})
				.onComplete(testContext.succeeding(id -> testContext.verify(() -> {
					assertTrue(vertx.deploymentIDs().contains(id));
					server.assertNoRequest(vertx).onComplete(testContext.succeedingThenComplete());
				})));
	}
}
