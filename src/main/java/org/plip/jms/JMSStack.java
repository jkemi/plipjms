package org.plip.jms;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;

/**
 * A stack of JMSClosable items that are closed in correct order.
 */
public final class JMSStack implements JMSClosable {

//	private static Logger _logger = LoggerFactory.getLogger(JMSStack.class);

	private final Deque<JMSClosable> _deque;

	public JMSStack() {
		_deque = new LinkedBlockingDeque<>();
	}

	private JMSStack(Deque<JMSClosable> d) {
		_deque = new LinkedBlockingDeque<>(d.size());

		// iterate from first to last in deque
		for (JMSClosable c : d) {
			_deque.addLast(c);
		}
	}


	public JMSStack(JMSClosable g) {
		this();
		push(g);
	}

	public <T extends JMSClosable> T push(T g) {
		_deque.addLast(g);
		return g;
	}

	public <T extends Connection> T push(final T connection) {
		push(new JMSClosable() {
			@Override
			public void close() throws JMSException {
				connection.close();
			}
		});
		return connection;
	}

	public <T extends Session> T push(final T session) {
		push(new JMSClosable() {
			@Override
			public void close() throws JMSException {
				session.close();
			}
		});
		return session;
	}

	public <T extends MessageConsumer> T push(final T consumer) {
		push(new JMSClosable() {
			@Override
			public void close() throws JMSException {
				consumer.close();
			}
		});
		return consumer;
	}

	public <T extends MessageProducer> T push(final T producer) {
		push(new JMSClosable() {
			@Override
			public void close() throws JMSException {
				producer.close();
			}
		});
		return producer;
	}

	public <T extends QueueBrowser> T push(final T browser) {
		push(new JMSClosable() {
			@Override
			public void close() throws JMSException {
				browser.close();
			}
		});
		return browser;
	}

	/**
	 * TemporaryQueue will be {@link TemporaryQueue#delete()}-ed on {@link JMSStack#close()}
	 * @param <T>
	 * @param temporary
	 * @return
	 */
	public <T extends TemporaryQueue> T push(final T temporary) {
		push(new JMSClosable() {
			@Override
			public void close() throws JMSException {
				temporary.delete();
			}
		});
		return temporary;
	}

	/**
	 * TemporaryTopic will be {@link TemporaryTopic#delete()}-ed on {@link JMSStack#close()}
	 * @param <T>
	 * @param temporary
	 * @return
	 */
	public <T extends TemporaryTopic> T push(final T temporary) {
		push(new JMSClosable() {
			@Override
			public void close() throws JMSException {
				temporary.delete();
			}
		});
		return temporary;
	}

	public JMSClosable pop() {
		return _deque.pollLast();
	}

	/**
	 * Transfers all elements of this stack into a new instance, making this one empty.
	 * @return a new instance if {@link JMSStack} containing all children of this stack.
	 */
	public synchronized JMSStack claim() {
		JMSStack ret = new JMSStack(_deque);
		_deque.clear();
		return ret;
	}

	@Override
	public void close() throws JMSException {
		JMSException err = null;
		int suppressedCount = 0;

		// Pop and close items from queue (order last to first)
		JMSClosable g;
		while ((g = _deque.pollLast()) != null) {
			try {
				g.close();
			} catch (JMSException e) {
				if (err == null) {
					err = e;
				} else {
					err.setLinkedException(e);
					err.addSuppressed(e);
					suppressedCount += 1;
				}
			} catch (RuntimeException e) {
				if (err == null) {
					err = new JMSException("caught exception during close");
					err.initCause(err);
				} else {
					err.setLinkedException(e);
					err.addSuppressed(e);
					suppressedCount += 1;
				}
			}
		}

		if (err != null) {
			//_logger.warn("suppressed {} additional exceptions", suppressedCount);
			throw err;
		}
	}

	public void closeSuppress() {
		try {
			close();
		} catch (JMSException e) {
			//_logger.info("suppressed a cascading JMS error on guard close", e);
		}
	}
}
