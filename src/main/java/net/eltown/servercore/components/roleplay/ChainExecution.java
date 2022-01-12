package net.eltown.servercore.components.roleplay;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class ChainExecution {

    private final LinkedList<Chain> chain;

    private ChainExecution(final LinkedList<Chain> chain) {
        this.chain = chain;
    }

    public void start() {
        final Timer timer = new Timer();

        int previousTime = 0;

        for (final Chain chain : chain) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    chain.task().run();
                }
            }, (previousTime + chain.seconds()) * 1000L);

            previousTime += chain.seconds();
        }
    }

    public static class Builder {

        private final LinkedList<Chain> preChain;

        public Builder() {
            this.preChain = new LinkedList<>();
        }

        public Builder append(final int seconds, final Runnable task) {
            this.preChain.add(new Chain(seconds, task));
            return this;
        }

        public ChainExecution build() {
            return new ChainExecution(preChain);
        }

    }

    private record Chain(int seconds, Runnable task) {

    }

}