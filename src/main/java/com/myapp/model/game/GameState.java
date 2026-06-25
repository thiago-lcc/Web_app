package com.myapp.model.game;

import java.time.Instant;
import java.util.*;

public class GameState {


    public enum Phase {
        WAITING_FOR_QUESTION,
        WAITING_FOR_ANSWERS,
        WAITING_FOR_ELIMINATION,
        OVER
    }

    private final List<Agent> agents;
    private final List<Round> rounds = new ArrayList<>();
    private final List<Message> pendingMessages = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> idsByToken = new HashMap<>();

    private final String ownerToken;
    private String rematchId;
    private final String name;
    private String currQuestion;
    private String guesserToken;
    private final int totalImpostors;
    private Phase phase;
    private Instant phaseStartedAt;


    public GameState(List<Agent> agents, String ownerToken, String name) {

        this.agents = agents;
        this.ownerToken = ownerToken;
        this.name = name;
        this.totalImpostors = numCurrImpostors();
    }


    public String getName() {

        return name;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public String getRematchId() {
        return rematchId;
    }

    public void setRematchId(String rematchId) {
        this.rematchId = rematchId;
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

    public List<Agent> currAgents(){

        return agents.stream()
                .filter(Agent::isALive)
                .toList();
    }

    public int numCurrImpostors() {

        return currImpostors().size();
    }

    public int numCurrAis() {

        return currAis().size();
    }

    public int numCurrAgents(){

        return currAgents().size();
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
        this.phaseStartedAt = Instant.now();
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

        return numCurrImpostors() == 0 || numCurrAis() <= 4 - totalImpostors();
    }

    public Instant phaseStartedAt(){

        return phaseStartedAt;
    }

    public String whoWon(){

        // win conditions
        if(numCurrImpostors() == 0) return "guesser";
        if (numCurrAis() <= 4 - totalImpostors()) return "impostors";

        return null;

    }

    public List<Round> getRounds() {

        return rounds;
    }

    public void addRound(Round round) {

        this.rounds.add(round);
    }

    public List<Message> getPendingMessages() {

        return pendingMessages;
    }

    public void addImpostorMessage(Message impostorMessage){

        pendingMessages.add(impostorMessage);
    }

    public void addAiMessages(List<Message> aiMessages){

        pendingMessages.addAll(aiMessages);
    }

    public int numPendingMessages(){

        return pendingMessages.size();
    }

    public void clearPendingMessages(){

        pendingMessages.clear();
    }

    public boolean alreadyAnswered(int id){

        return getPendingMessages()
                .stream()
                .anyMatch(m -> m.agentId() == id);
    }
}