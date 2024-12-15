package psd.group4.utils;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class ProgressBarTask implements Runnable {
    private final int durationInSeconds;

    public ProgressBarTask(int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    @Override
    public void run() {
        try (ProgressBar progressBar = new ProgressBarBuilder()
                .setTaskName("Processing")
                .setStyle(ProgressBarStyle.ASCII)
                .setInitialMax(durationInSeconds * 2) // Double the steps for smoother updates
                .continuousUpdate()
                .setUpdateIntervalMillis(100) // Refresh rate for smoother updates
                .build()) {

            for (int i = 0; i < durationInSeconds * 2; i++) {
                Thread.sleep(500); // Enforce a 0.5-second delay for each step
                progressBar.step(); // Update the progress bar by one step
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Progress interrupted: " + e.getMessage());
        }
    }
}