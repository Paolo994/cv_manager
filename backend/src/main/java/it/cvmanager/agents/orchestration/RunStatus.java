package it.cvmanager.agents.orchestration;

public enum RunStatus {
    RUNNING, SUCCEEDED, FAILED;

    public boolean isTerminal() {
        return this != RUNNING;
    }
}
