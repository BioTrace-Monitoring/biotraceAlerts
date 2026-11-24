package com.biotrace.integration;

import org.json.JSONObject;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.List;

public class HistoricoS3Reader {
    private final S3Client s3Client;

    public HistoricoS3Reader(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public List<S3Object> listarObjetos() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket("biotrace-datalake")
                .prefix("gold/historico/")
                .build();

        ListObjectsV2Iterable paginator = s3Client.listObjectsV2Paginator(request);
        List<S3Object> objetosEncontrados = paginator.stream()
                .flatMap(response -> response.contents().stream())
                .filter(s3Object -> s3Object.key().endsWith("/leituras.json"))
                .toList();

        return objetosEncontrados;
    }

    public JSONObject lerObjeto(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("biotrace-datalake")
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return new JSONObject(objectBytes.asUtf8String());
    }
}
