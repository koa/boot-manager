package ch.bergturbenthal.infrastructure.db.repository;

import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ch.bergturbenthal.infrastructure.db.model.Log;

@Repository
public interface LogRepository extends JpaRepository<Log, UUID> {
    @Query("select l from Log l order by l.timestamp asc")
    Stream<Log> readAllOrdered();
}
