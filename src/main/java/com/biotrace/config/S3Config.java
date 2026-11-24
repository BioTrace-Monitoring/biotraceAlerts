package com.biotrace.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Config {
    private static final Region region = Region.US_EAST_1;
    private static S3Client instance = null;

    public static S3Client getClient() {
        if (instance == null) {
            instance = S3Client.builder()
                    .region(region)
                    .build();
        }

        return instance;
    }
}
