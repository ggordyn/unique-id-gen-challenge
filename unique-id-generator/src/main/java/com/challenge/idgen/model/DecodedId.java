package com.challenge.idgen.model;

public record DecodedId(long id, long timestamp, int datacenterId, int workerId, int sequence) {
}
