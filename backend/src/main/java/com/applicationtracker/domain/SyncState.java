package com.applicationtracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sync_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncState implements Persistable<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_sync_states_user"))
    private User user;

    @Column(name = "last_history_id")
    private String lastHistoryId;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = true;

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNewRecord;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNewRecord = false;
    }

    public static SyncState of(User user, String lastHistoryId, Instant lastSyncedAt) {
        return SyncState.builder()
                .userId(user.getId())
                .user(user)
                .lastHistoryId(lastHistoryId)
                .lastSyncedAt(lastSyncedAt)
                .isNewRecord(true)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncState syncState = (SyncState) o;
        return Objects.equals(userId, syncState.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
