package com.emat.reapi.statement.infra;

import com.emat.reapi.statement.domain.Profile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(value = ProfileDocument.COLLECTION_NAME)
@TypeAlias(value = ProfileDocument.COLLECTION_NAME)
public class ProfileDocument {

    public static final String COLLECTION_NAME = "profiles";

    @Id
    private String id;
    private String plName;
    private String blockingName;
    private String transitionalName;
    private String resourcesName;
    private int order;
    private boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;

    public Profile toDomain() {
        return Profile.builder()
                .id(id)
                .plName(plName)
                .blockingName(blockingName)
                .transitionalName(transitionalName)
                .resourcesName(resourcesName)
                .order(order)
                .isDeleted(isDeleted)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static ProfileDocument toDocument(Profile domain) {
        return ProfileDocument.builder()
                .id(domain.getId())
                .plName(domain.getPlName())
                .blockingName(domain.getBlockingName())
                .transitionalName(domain.getTransitionalName())
                .resourcesName(domain.getResourcesName())
                .order(domain.getOrder())
                .isDeleted(domain.isDeleted())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
