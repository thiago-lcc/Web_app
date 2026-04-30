package com.myapp.model.game;

import java.util.List;

public record Round(String question, List<Message> answers, int num){}