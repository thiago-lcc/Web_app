package com.myapp.model.game;

public record GameEvent<T>(String gameId, T msg) {
}
