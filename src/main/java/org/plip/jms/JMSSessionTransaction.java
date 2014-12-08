package org.plip.jms;

import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TransactionRolledBackException;

/**
 * try-with-resource compatible wrapper for JMS session transactions ("Local transactions")
 *
 * Example usage:
 *
 * <pre><code>
 *  try (JMSSessionTransaction trans = new JMSSessionTransaction(session)) {
 * 	    ... do stuff with messages and such
 *      trans.commit()
 *  }
 * </code></pre>
 *
 * This ensures that {@link Session#rollback()} is called in all cases where {@link #commit()} isn't reached.
 */
public class JMSSessionTransaction implements JMSClosable {
	private Session session;

	public JMSSessionTransaction(Session session) throws JMSException {
		if (session == null) {
			throw new NullPointerException("session mustn't be null");
		}
		this.session = session;
	}

	/**
	 * Commits any changes in this transaction.
	 *
	 * @see Session#commit()
	 *
	 * @throws JMSException				see {@link Session#commit()}
	 * @throws IllegalStateException	{@link #commit()}, {@link #rollback()} or {@link #close} has already been called
	 * @throws TransactionRolledBackException	see {@link Session#commit()}
	 */
	public void commit() throws JMSException, IllegalStateException, TransactionRolledBackException {
		if (session == null) {
			throw new IllegalStateException("not in transaction");
		}
		try {
			session.commit();
		} finally {
			session = null;
		}
	}

	/**
	 * Performs an explicit rollback.
	 *
	 * @see Session#rollback()
	 *
	 * It should not be necessary to call this method explicitly as
	 * as it's taken care of in {@link #close()}
	 * However it is still safe to call this method, in which case nothing is performed by {@link #close()}.
	 *
	 * @throws JMSException if rollback fails
	 * @throws IllegalStateException if already committed or rolled back
	 */
	public void rollback() throws JMSException, IllegalStateException {
		if (session == null) {
			throw new IllegalStateException("not in transaction");
		}
		try {
			session.rollback();
		} finally {
			session = null;
		}
	}

	/**
	 * Rollbacks any uncommitted session state, or if {@link #rollback()} or {@link #commit()} has already been called
	 * performs nothing.
	 *
	 * @see Session#rollback()
	 * @throws JMSException if rollback fails
	 */
	@Override
	public void close() throws JMSException {
		if (session != null) {
			try {
				session.rollback();
			} finally {
				session = null;
			}
		}
	}

}
