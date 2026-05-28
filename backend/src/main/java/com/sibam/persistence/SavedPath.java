package com.sibam.persistence;

import com.sibam.graph.model.output.Journey;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_paths")
@Data
public class SavedPath {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "journey", nullable = false, columnDefinition = "jsonb")
    private Journey journey;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
