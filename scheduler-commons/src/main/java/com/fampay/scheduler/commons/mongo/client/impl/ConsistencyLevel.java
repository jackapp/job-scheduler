package com.fampay.scheduler.commons.mongo.client.impl;

public enum ConsistencyLevel {
    // fully consistent
    STRONG,
    // read committed but may be not reflect the latest data
    // if there are writes w1, w2, w3
    // the reads may give any of the state, e.g. following are possible
    // w1, w1, w3, w3, w2, w2
    EVENTUAL
}
