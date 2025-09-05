package com.vht.connection;



import javax.validation.constraints.*;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties // chỉ để Spring tạo bean container cho file; mỗi record bên dưới mới bind theo prefix riêng
public class AppConfigProperties {

    /* -------- app -------- */
    @Validated
    @ConfigurationProperties(prefix = "app")
    public record App(
            @NotBlank String name,
            @NotBlank String environment
    ) {}

    /* -------- network -------- */
    @Validated
    @ConfigurationProperties(prefix = "network")
    public record Network(
            @NotBlank String mainAddress,
            List<String> dp,
            Grpc grpc
    ) {
        public record Grpc(
        @Min(1) @Max(65535) int port,
        @Min(100) int timeoutMs
        ) {}
    }

    /* -------- security -------- */
    @Validated
    @ConfigurationProperties(prefix = "security")
    public record Security(
            @NotBlank String empGrpcAddress,
            Verify verify
    ) {
        public record Verify(
                @NotBlank String username,
                @NotBlank String password
        ) {}
    }

    /* -------- features / flags -------- */
    @ConfigurationProperties(prefix = "features")
    public record Features(
            boolean enableCaptureData,
            EnableLog enableLog
    ) {
        public record EnableLog(
        boolean bv5Target,
        boolean bmsTarget,
        boolean command,
        boolean empJammer,
        boolean df
        ) {}
    }

    /* -------- ranges -------- */
    @ConfigurationProperties(prefix = "ranges")
    public record Ranges(
            double df,
            double emp,
            double sendTarget,
            double openAngleDf,
            boolean turnOffDf,
            boolean turnOffVq
    ) {}

    /* -------- bv modules (Map theo tên) -------- */
    @Validated
    @ConfigurationProperties(prefix = "bv")
    public record BV(
            Map<@NotBlank String, Module> modules
    ) {
        @Validated
        public record Module(
                @NotBlank String alias,
                @NotBlank String ip,
        @Min(1) @Max(65535) int port,
        double lat,
        double lon,
        String jmName,
        String empName,
        @Min(0) Double rangeJammer
        ) {}
    }

    /* -------- telemetry -------- */
    @ConfigurationProperties(prefix = "telemetry")
    public record Telemetry(
            @Min(1) @Max(65535) int udpPort,
            double bmsLat,
            double bmsLon
    ) {}
}

