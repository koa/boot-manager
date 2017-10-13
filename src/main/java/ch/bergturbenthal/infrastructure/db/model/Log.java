package ch.bergturbenthal.infrastructure.db.model;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Log {
    @Id
    private UUID    id;
    private Instant timestamp;
    @Lob
    @Column(length = 1024 * 1024)
    private String  data;
}
