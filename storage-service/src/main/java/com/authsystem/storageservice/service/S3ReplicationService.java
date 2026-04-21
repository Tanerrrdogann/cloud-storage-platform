package com.authsystem.storageservice.service;

import com.authsystem.storageservice.dto.ObjectReplicationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ReplicationService {

    private final S3Client s3Client;

    @Value("${storage.replication.s3.bucket}")
    private String bucket;

    public void replicate(ObjectReplicationMessage message) {
        Path sourcePath = Path.of(message.getStoragePath());

        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            throw new IllegalArgumentException("Local encrypted blob file not found: " + sourcePath);
        }

        ensureBucketExists();

        String objectKey = buildReplicaObjectKey(message);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .metadata(java.util.Map.of(
                        "source-object-id", message.getObjectId().toString(),
                        "source-blob-id", message.getBlobId().toString(),
                        "owner-id", message.getOwnerId(),
                        "source-object-key", message.getObjectKey()
                ))
                .build();

        s3Client.putObject(request, RequestBody.fromFile(sourcePath));

        log.info(
                "ST_LOG | ACTION:OBJECT_REPLICATED_TO_S3 | TASK_ID:{} | BUCKET:{} | S3_KEY:{} | SOURCE_PATH:{}",
                message.getTaskId(),
                bucket,
                objectKey,
                sourcePath
        );
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build());
        } catch (S3Exception ex) {
            if (ex.statusCode() != 404) {
                throw ex;
            }

            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build());

            log.info(
                    "ST_LOG | ACTION:S3_REPLICATION_BUCKET_CREATED | BUCKET:{}",
                    bucket
            );
        }
    }

    private String buildReplicaObjectKey(ObjectReplicationMessage message) {
        return String.join(
                "/",
                "objects",
                message.getOwnerId(),
                message.getObjectId().toString(),
                message.getBlobId() + ".blob"
        );
    }
}
