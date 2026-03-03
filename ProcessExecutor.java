package main;

import javax.swing.Timer;
import java.util.List;

/**
 * Executes processes sequentially, handling blocking and non-blocking actions
 */
public class ProcessExecutor {
    private AdventureGame game;
    private Process currentProcess;
    private List<Process.ProcessAction> actions;
    private int currentActionIndex;
    private Timer executionTimer;
    private boolean isExecuting;
    private Process.ProcessAction currentBlockingAction;

    public ProcessExecutor(AdventureGame game) {
        this.game = game;
        this.isExecuting = false;
    }

    /**
     * Execute a process
     */
    public void executeProcess(Process process) {
        if (isExecuting) {
            System.out.println("ProcessExecutor: Already executing a process, skipping");
            return;
        }

        // Check conditions
        if (!process.checkConditions(game.getGameProgress())) {
            System.out.println("ProcessExecutor: Conditions not met for process '" +
                             process.getProcessName() + "'");
            return;
        }

        System.out.println("ProcessExecutor: Starting execution of process '" +
                         process.getProcessName() + "'");

        this.currentProcess = process;
        this.actions = process.getActions();
        this.currentActionIndex = 0;
        this.isExecuting = true;
        this.currentBlockingAction = null;

        // Start execution
        executeNextAction();
    }

    /**
     * Execute the next action in the sequence
     */
    private void executeNextAction() {
        // Check if we're waiting for a blocking action to complete
        if (currentBlockingAction != null) {
            if (!currentBlockingAction.isComplete()) {
                // Still waiting, check again in 100ms
                scheduleNextCheck(100);
                return;
            } else {
                // Blocking action completed
                System.out.println("ProcessExecutor: Blocking action completed");
                currentBlockingAction = null;
            }
        }

        // Check if we're done
        if (currentActionIndex >= actions.size()) {
            finishExecution();
            return;
        }

        // Execute next action
        Process.ProcessAction action = actions.get(currentActionIndex);
        currentActionIndex++;

        System.out.println("ProcessExecutor: Executing action " + currentActionIndex + "/" +
                         actions.size() + ": " + action.getActionType());

        try {
            boolean shouldBlock = action.execute(game);

            if (shouldBlock) {
                // This is a blocking action - wait for completion
                System.out.println("ProcessExecutor: Action is blocking, waiting for completion...");
                currentBlockingAction = action;
                scheduleNextCheck(100);
            } else {
                // Non-blocking action - continue immediately
                executeNextAction();
            }
        } catch (Exception e) {
            System.err.println("ProcessExecutor: Error executing action: " + e.getMessage());
            e.printStackTrace();
            // Continue with next action despite error
            executeNextAction();
        }
    }

    /**
     * Schedule next action check
     */
    private void scheduleNextCheck(int delayMs) {
        if (executionTimer != null && executionTimer.isRunning()) {
            executionTimer.stop();
        }

        executionTimer = new Timer(delayMs, e -> {
            executeNextAction();
        });
        executionTimer.setRepeats(false);
        executionTimer.start();
    }

    /**
     * Finish process execution
     */
    private void finishExecution() {
        System.out.println("ProcessExecutor: Process '" + currentProcess.getProcessName() +
                         "' completed");

        isExecuting = false;
        currentProcess = null;
        actions = null;
        currentActionIndex = 0;
        currentBlockingAction = null;

        if (executionTimer != null && executionTimer.isRunning()) {
            executionTimer.stop();
        }
    }

    /**
     * Check if executor is currently running a process
     */
    public boolean isExecuting() {
        return isExecuting;
    }

    /**
     * Cancel current process execution
     */
    public void cancel() {
        if (isExecuting) {
            System.out.println("ProcessExecutor: Cancelling process execution");
            finishExecution();
        }
    }

    /**
     * Get the currently executing process
     */
    public Process getCurrentProcess() {
        return currentProcess;
    }
}
