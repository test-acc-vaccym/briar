package org.briarproject.plugins;

import static java.util.logging.Level.INFO;

import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.briarproject.api.plugins.Plugin;
import org.briarproject.api.plugins.PluginExecutor;
import org.briarproject.api.system.Timer;
import org.briarproject.api.transport.ConnectionRegistry;

class PollerImpl implements Poller {

	private static final Logger LOG =
			Logger.getLogger(PollerImpl.class.getName());

	private final Executor pluginExecutor;
	private final ConnectionRegistry connRegistry;
	private final Timer timer;

	@Inject
	PollerImpl(@PluginExecutor Executor pluginExecutor,
			ConnectionRegistry connRegistry, Timer timer) {
		this.pluginExecutor = pluginExecutor;
		this.connRegistry = connRegistry;
		this.timer = timer;
	}

	public void start(Collection<Plugin> plugins) {
		for(Plugin plugin : plugins) schedule(plugin, true);
	}

	private void schedule(Plugin plugin, boolean randomise) {
		if(plugin.shouldPoll()) {
			long interval = plugin.getPollingInterval();
			// Randomise intervals at startup to spread out connection attempts
			if(randomise) interval = (long) (interval * Math.random());
			timer.schedule(new PollTask(plugin), interval);
		}
	}

	public void stop() {
		timer.cancel();
	}

	public void pollNow(final Plugin p) {
		pluginExecutor.execute(new Runnable() {
			public void run() {
				if(LOG.isLoggable(INFO))
					LOG.info("Polling " + p.getClass().getSimpleName());
				p.poll(connRegistry.getConnectedContacts(p.getId()));
			}
		});
	}

	private class PollTask extends TimerTask {

		private final Plugin plugin;

		private PollTask(Plugin plugin) {
			this.plugin = plugin;
		}

		public void run() {
			pollNow(plugin);
			schedule(plugin, false);
		}
	}
}
