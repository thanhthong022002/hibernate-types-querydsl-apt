package com.pallasathenagroup.querydsl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Entity
public class TimeZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Column(name = "local_timestamp", columnDefinition = "timestamp")
    LocalDateTime localTimestamp;

    @Column(name = "zoned_timestamp", columnDefinition = "timestamp with time zone")
    ZonedDateTime zonedTimestamp;

    @Column(name = "offset_timestamp", columnDefinition = "timestamp with time zone")
    OffsetDateTime offsetTimestamp;

}

