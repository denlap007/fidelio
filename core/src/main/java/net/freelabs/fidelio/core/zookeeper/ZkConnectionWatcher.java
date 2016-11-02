/*
 * Copyright (C) 2015 Dionysis Lappas (dio@freelabs.net)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.freelabs.fidelio.core.zookeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;

/**
 *
 * Class that establishes a zookeeper connection.
 */
public class ZkConnectionWatcher implements Watcher {

    /**
     * A zookeeper handle.
     */
    protected ZooKeeper zk;
    /**
     * The zookeeper host:port list.
     */
    private final String zkHosts;
    /**
     * The zookeeper client session timeout.
     */
    private final int zkSessionTimeout;
    /**
     * A CountDownLatch with a count of one, representing the number of events
     * that need to occur before it releases all waiting threads.
     */
    private CountDownLatch connectedSignal;

    /**
     * A Logger object.
     */
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ZkConnectionWatcher.class);

    /**
     * Constructor.
     *
     * @param zkHosts the zookeeper host:port list.
     * @param zkSessionTimeout the client session timeout.
     */
    public ZkConnectionWatcher(String zkHosts, int zkSessionTimeout) {
        this.zkHosts = zkHosts;
        this.zkSessionTimeout = zkSessionTimeout;
    }

    /**
     * <p>
     * Creates a new zookeeper handle and waits until connection to the
     * zookeeper server is established.
     * <p>
     * The call to the constructor returns immediately, so it is important to
     * wait for the connection to be established before using the ZooKeeper
     * object. We make use of Java’s CountDownLatch class (in the
     * java.util.concurrent package) to block until the ZooKeeper instance is
     * ready.
     *
     * @throws IOException in cases of network failure
     * @throws InterruptedException if thread is interrupted while waiting.
     */
    public void connect() throws IOException, InterruptedException {
        connectedSignal = new CountDownLatch(1);
        zk = new ZooKeeper(zkHosts, zkSessionTimeout, this);
        connectedSignal.await(30, TimeUnit.SECONDS);
    }

    /**
     * Processes a watched event when a
     * {@link ZkConnectionWatcher ZkConnectionWatcher} object is passed as a
     * watcher.
     *
     * @param event a watched event.
     */
    @Override
    public void process(WatchedEvent event) {
        LOG.info("Session state event: {}", event.getState());
        if (event.getState() == KeeperState.SyncConnected) {
            connectedSignal.countDown();
        }
    }

    /**
     * Register a watcher for the connection (overrides the one specified during
     * construction).
     *
     * @param watcher a watcher for the connection.
     */
    public void registerNewZkConnectionWatcher(Watcher watcher) {
        zk.register(watcher);
    }

    /**
     * Closes the client session of a {@link org.apache.zookeeper.ZooKeeper
     * zookeeper handle}.
     *
     * @throws java.lang.InterruptedException if thread is interrupted.
     */
    public void closeSession() throws InterruptedException {
        zk.close();
    }
}
