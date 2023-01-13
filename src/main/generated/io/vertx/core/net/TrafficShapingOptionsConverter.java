package io.vertx.core.net;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.core.net.TrafficShapingOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.core.net.TrafficShapingOptions} original class using Vert.x codegen.
 */
public class TrafficShapingOptionsConverter {


  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, TrafficShapingOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "checkIntervalForStats":
          if (member.getValue() instanceof Number) {
            obj.setCheckIntervalForStats(((Number)member.getValue()).longValue());
          }
          break;
        case "inboundGlobalBandwidth":
          break;
        case "maxDelayToWaitTime":
          if (member.getValue() instanceof Number) {
            obj.setMaxDelayToWaitTime(((Number)member.getValue()).longValue());
          }
          break;
        case "outboundGlobalBandwidth":
          break;
        case "peakOutboundGlobalBandwidth":
          if (member.getValue() instanceof Number) {
            obj.setPeakOutboundGlobalBandwidth(((Number)member.getValue()).longValue());
          }
          break;
      }
    }
  }

   static void toJson(TrafficShapingOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(TrafficShapingOptions obj, java.util.Map<String, Object> json) {
    json.put("checkIntervalForStats", obj.getCheckIntervalForStats());
    json.put("inboundGlobalBandwidth", obj.getInboundGlobalBandwidth());
    json.put("maxDelayToWaitTime", obj.getMaxDelayToWaitTime());
    json.put("outboundGlobalBandwidth", obj.getOutboundGlobalBandwidth());
    json.put("peakOutboundGlobalBandwidth", obj.getPeakOutboundGlobalBandwidth());
  }
}
