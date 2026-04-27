package com.myapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {


    public enum Phase {
        WAITING_FOR_START,
        WAITING_FOR_QUESTION,
        WAITING_FOR_ANSWERS,
        WAITING_FOR_ROUND,
        WAITING_FOR_ELIMINATION,
        OVER
    }

    private final List<Agent> agents;
    private final List<Round> rounds = new ArrayList<>();
    private final List<Message> pendingImpostorMessages = new ArrayList<>();
    private final Map<String, Integer> idsByToken = new HashMap<>();

    private String currQuestion;
    private String guesserToken;
    private final int totalImpostors;
    private Phase phase = Phase.WAITING_FOR_START;


    public GameState(List<Agent> agents) {

        this.agents = agents;
        this.totalImpostors = numCurrImpostors();
    }


    public int roundNum() {

        return rounds.size();
    }

    public List<Agent> currImpostors() {

        return agents.stream()
                .filter(a -> a.isALive() && a.isImpostor())
                .toList();
    }

    public List<Agent> currAis() {

        return agents.stream()
                .filter(a -> a.isALive() && !a.isImpostor())
                .toList();
    }

    public int numCurrImpostors() {

        return currImpostors().size();
    }

    public int numCurrAis() {

        return currAis().size();
    }

    public int totalImpostors() {

        return totalImpostors;
    }

    public Agent getAgent(int id) {

        for (Agent agent : agents) if (agent.getId() == id) return agent;

        throw new IllegalArgumentException("Id: " + id + " not found.");
    }

    public String getGuesserToken() {

        return guesserToken;
    }

    public void setGuesserToken(String guesserToken) {

        this.guesserToken = guesserToken;
    }

    public Integer getIdByToken(String impostorToken){

        Integer id = idsByToken.get(impostorToken);
        if (id == null)
            throw new IllegalArgumentException("Token key does not exist");

        return id;
    }

    public void setImpostorToken(String impostorToken, int id){

        idsByToken.put(impostorToken, id);
    }

    public List<Integer> availableImpostorIds(){

            return currImpostors().stream()
                    .map(Agent::getId)
                    .filter(id -> !idsByToken.containsValue(id))
                    .toList();
    }

    public Phase getPhase() {

        return phase;
    }

    public void setPhase(Phase phase) {

        this.phase = phase;
    }

    public String getCurrQuestion() {

        return currQuestion;
    }

    public void setCurrQuestion(String currQuestion) {

        this.currQuestion = currQuestion;
    }

    public void discardCurrQuestion(){

        this.currQuestion = null;
    }

    public boolean isOver() {

        return (numCurrImpostors() == 0 || numCurrAis() <= 4 - totalImpostors());
    }

    public boolean canStart(){

        return (totalImpostors() == idsByToken.size() && getGuesserToken() != null);
    }

    public List<Round> getRounds() {

        return rounds;
    }

    public void addRound(Round round) {

        this.rounds.add(round);
    }

    public List<Message> getImpostorMessages() {

        return pendingImpostorMessages;
    }

    public void addImpostorMessage(Message impostorMessage){

        pendingImpostorMessages.add(impostorMessage);
    }

    public int numImpostorMessages(){

        return pendingImpostorMessages.size();
    }

    public void clearImpostorMessages(){

        pendingImpostorMessages.clear();
    }
}